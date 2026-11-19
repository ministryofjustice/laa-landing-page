package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import uk.gov.justice.laa.datauserapi.contracts.request.DeleteUserRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeletionApiServiceTest {

    private static final String ACTOR_OID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private UserDataApiClient userDataApiClient;
    @Mock
    private OAuth2AuthorizedClientRepository authorizedClientRepository;
    @Mock
    private ObjectProvider<HttpServletRequest> requestProvider;
    @Mock
    private HttpServletRequest request;
    @Mock
    private OAuth2AuthorizedClient authorizedClient;
    @Mock
    private OAuth2User principal;
    @Mock
    private DataApiFeatureFlagService featureFlags;

    private UserDeletionApiService service;
    private OAuth2AuthenticationToken authentication;
    private String targetEntraOid;

    @BeforeEach
    void setUp() {
        service = new UserDeletionApiService(userDataApiClient, authorizedClientRepository, requestProvider,
                featureFlags, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        authentication = new OAuth2AuthenticationToken(principal, Set.of(), "azure");
        targetEntraOid = UUID.randomUUID().toString();
        when(requestProvider.getObject()).thenReturn(request);
        lenient().when(request.getHeader("X-Correlation-ID")).thenReturn("correlation-id");
        lenient().when(principal.getAttribute("oid")).thenReturn(ACTOR_OID);
        when(authorizedClientRepository.loadAuthorizedClient(eq("azure"), eq(authentication), eq(request)))
                .thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "user-token", Instant.parse("2025-12-31T23:59:00Z"),
                Instant.parse("2026-01-01T00:01:00Z")));
    }

    @Test
    void deleteUser_sendsEntraObjectIdAndMappedReason() {
        when(userDataApiClient.deleteUser(anyString(), anyString(), anyString(), any()))
                .thenReturn(CommandResult.success("deleted"));
        ArgumentCaptor<DeleteUserRequest> requestCaptor = ArgumentCaptor.forClass(DeleteUserRequest.class);

        service.deleteUser(authentication, targetEntraOid, "LeftFirm");

        verify(userDataApiClient).deleteUser(eq("user-token"), eq(ACTOR_OID), eq("correlation-id"),
                requestCaptor.capture());
        assertThat(requestCaptor.getValue().userEntraObjectId()).isEqualTo(UUID.fromString(targetEntraOid));
        assertThat(requestCaptor.getValue().deleteReason())
                .isEqualTo(uk.gov.justice.laa.datauserapi.contracts.domain.DeleteUserReason.LeftFirm);
    }

    @Test
    void deleteUser_whenApiRejectsToken_requiresReauthentication() {
        when(userDataApiClient.deleteUser(anyString(), anyString(), anyString(), any()))
                .thenThrow(new UserDataApiClientException("Data API rejected token", 401));

        assertThatThrownBy(() -> service.deleteUser(authentication, targetEntraOid, "LeftFirm"))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
    }

    @Test
    void deleteUser_whenCommandIsNotConfirmed_throwsServerError() {
        when(userDataApiClient.deleteUser(anyString(), anyString(), anyString(), any()))
                .thenReturn(CommandResult.failure("not deleted"));

        assertThatThrownBy(() -> service.deleteUser(authentication, targetEntraOid, "LeftFirm"))
                .isInstanceOfSatisfying(UserDataApiClientException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(502));
    }

    @Test
    void deleteUser_whenAccessTokenIsExpiring_requiresReauthenticationWithoutCallingApi() {
        when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "user-token", Instant.parse("2025-12-31T23:59:00Z"),
                Instant.parse("2026-01-01T00:00:20Z")));

        assertThatThrownBy(() -> service.deleteUser(authentication, targetEntraOid, "LeftFirm"))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
        verify(userDataApiClient, never()).deleteUser(anyString(), anyString(), anyString(), any());
    }
}
