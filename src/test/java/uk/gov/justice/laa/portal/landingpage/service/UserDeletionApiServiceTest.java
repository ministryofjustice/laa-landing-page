package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.HttpClientErrorException;
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
import static org.mockito.Mockito.verifyNoInteractions;
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
        DataApiCaller dataApiCaller = new DataApiCaller(authorizedClientRepository, requestProvider,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        service = new UserDeletionApiService(userDataApiClient, dataApiCaller, featureFlags);
        authentication = new OAuth2AuthenticationToken(principal, Set.of(), "azure");
        targetEntraOid = UUID.randomUUID().toString();
        lenient().when(requestProvider.getObject()).thenReturn(request);
        lenient().when(request.getHeader("X-Correlation-ID")).thenReturn("correlation-id");
        lenient().when(principal.getAttribute("oid")).thenReturn(ACTOR_OID);
        lenient().when(authorizedClientRepository.loadAuthorizedClient(eq("azure"), eq(authentication), eq(request)))
                .thenReturn(authorizedClient);
        lenient().when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(
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
    void deleteUser_usesProvidedAuthenticationRatherThanSecurityContext() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("other", "password"));
        try {
            when(userDataApiClient.deleteUser(anyString(), anyString(), anyString(), any()))
                    .thenReturn(CommandResult.success("deleted"));

            service.deleteUser(authentication, targetEntraOid, "LeftFirm");

            verify(userDataApiClient).deleteUser(eq("user-token"), eq(ACTOR_OID), eq("correlation-id"), any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void deleteUser_whenApiRejectsToken_requiresReauthentication() {
        when(userDataApiClient.deleteUser(anyString(), anyString(), anyString(), any()))
                .thenThrow(new UserDataApiClientException("Data API rejected token", 401));

        assertThatThrownBy(() -> service.deleteUser(authentication, targetEntraOid, "LeftFirm"))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 403, 404, 500})
    void deleteUser_whenApiReturnsOtherError_preservesStatus(int status) {
        when(userDataApiClient.deleteUser(anyString(), anyString(), anyString(), any()))
                .thenThrow(new UserDataApiClientException("Data API error", status));

        assertThatThrownBy(() -> service.deleteUser(authentication, targetEntraOid, "LeftFirm"))
                .isInstanceOfSatisfying(UserDataApiClientException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401})
    void deleteUser_whenOboExchangeRejectsToken_requiresReauthentication(int status) {
        when(userDataApiClient.deleteUser(anyString(), anyString(), anyString(), any()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.valueOf(status), "OBO error", null, null, null));

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
    void deleteUser_whenCommandResultIsNull_throwsServerError() {
        when(userDataApiClient.deleteUser(anyString(), anyString(), anyString(), any())).thenReturn(null);

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

    @Test
    void deleteUser_whenAccessTokenExpiresAtBufferBoundary_requiresReauthentication() {
        when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "user-token", Instant.parse("2025-12-31T23:59:00Z"),
                Instant.parse("2026-01-01T00:00:30Z")));

        assertThatThrownBy(() -> service.deleteUser(authentication, targetEntraOid, "LeftFirm"))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
        verifyNoInteractions(userDataApiClient);
    }

    @Test
    void deleteUser_whenAuthorizedClientIsMissing_requiresReauthentication() {
        when(authorizedClientRepository.loadAuthorizedClient("azure", authentication, request)).thenReturn(null);

        assertThatThrownBy(() -> service.deleteUser(authentication, targetEntraOid, "LeftFirm"))
                .isInstanceOf(ClientAuthorizationRequiredException.class);
        verifyNoInteractions(userDataApiClient);
    }

    @Test
    void deleteUser_whenAuthenticationIsNotOauth_requiresReauthentication() {
        assertThatThrownBy(() -> service.deleteUser(new TestingAuthenticationToken("user", "password"),
                targetEntraOid, "LeftFirm"))
                .isInstanceOfSatisfying(ClientAuthorizationRequiredException.class,
                        exception -> assertThat(exception.getClientRegistrationId()).isEqualTo("azure"));
        verifyNoInteractions(authorizedClientRepository, userDataApiClient);
    }
}
