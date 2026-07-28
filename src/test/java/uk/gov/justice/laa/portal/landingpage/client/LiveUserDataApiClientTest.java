package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveUserDataApiClientTest {

    private static final String OBO_TOKEN = "obo-access-token";
    private static final String USER_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String USER_ACCESS_TOKEN = "user-access-token";
    private static final String CORRELATION_ID = "test-correlation-id";

    @Mock
    private OboTokenService oboTokenService;

    @Mock
    private RestClient userDataApiRestClient;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    @Mock
    private OAuth2AuthenticationToken authentication;

    @Mock
    private OAuth2User principal;

    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    @Mock
    private OAuth2AccessToken accessToken;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private LiveUserDataApiClient client;

    @Test
    void hello_returnsBody_whenOboTokenAcquiredSuccessfully() {
        stubAuthAndOboToken();
        Map<String, String> expected = Map.of("message", "Hello from LAA Data User API");
        stubGetRequest("/api/v1/hello", expected);

        Map<String, String> result = client.hello(authentication, CORRELATION_ID);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void me_returnsOidAndSub_whenOboTokenAcquiredSuccessfully() {
        stubAuthAndOboToken();
        Map<String, String> expected = Map.of("oid", USER_OID, "sub", "test-sub");
        stubGetRequest("/api/v1/me", expected);

        Map<String, String> result = client.me(authentication, CORRELATION_ID);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void hello_generatesCorrelationId_whenNullProvided() {
        stubAuthAndOboToken();
        Map<String, String> expected = Map.of("message", "Hello from LAA Data User API");
        stubGetRequest("/api/v1/hello", expected);

        Map<String, String> result = client.hello(authentication, null);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void hello_throwsException_whenNoAuthorizedClient() {
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("azure");
        when(authentication.getName()).thenReturn("user-name");
        when(authorizedClientService.loadAuthorizedClient("azure", "user-name")).thenReturn(null);

        assertThatThrownBy(() -> client.hello(authentication, CORRELATION_ID))
            .isInstanceOf(UserDataApiClientException.class)
            .hasMessageContaining("No access token available");
    }

    @Test
    void hello_throwsException_whenNotOAuth2AuthenticationToken() {
        var nonOauthAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
            "user", "password");

        assertThatThrownBy(() -> client.hello(nonOauthAuth, CORRELATION_ID))
            .isInstanceOf(UserDataApiClientException.class)
            .hasMessageContaining("OAuth2AuthenticationToken");
    }

    private void stubAuthAndOboToken() {
        when(authentication.getAuthorizedClientRegistrationId()).thenReturn("azure");
        when(authentication.getName()).thenReturn("user-name");
        when(authorizedClientService.loadAuthorizedClient("azure", "user-name")).thenReturn(authorizedClient);
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(accessToken.getTokenValue()).thenReturn(USER_ACCESS_TOKEN);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getAttribute("oid")).thenReturn(USER_OID);
        when(oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID)).thenReturn(OBO_TOKEN);
    }

    @SuppressWarnings("unchecked")
    private void stubGetRequest(String uri, Map<String, String> responseBody) {
        when(userDataApiRestClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(uri)).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.body(any(ParameterizedTypeReference.class))).thenReturn(responseBody);
    }
}
