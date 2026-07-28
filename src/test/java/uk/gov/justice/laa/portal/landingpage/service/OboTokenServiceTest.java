package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodyUriSpec;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClient.ResponseSpec;
import uk.gov.justice.laa.portal.landingpage.config.CachingConfig;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OboTokenServiceTest {

    private static final String USER_OID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
    private static final String USER_ACCESS_TOKEN = "user-access-token";
    private static final String OBO_TOKEN = "obo-access-token";
    private static final String TOKEN_ENDPOINT = "https://login.microsoftonline.com/test-tenant/oauth2/v2.0/token";

    @Mock
    private CacheManager cacheManager;

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private RestClient oboRestClient;

    @Mock
    private Cache cache;

    @Mock
    private RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RequestBodySpec requestBodySpec;

    @Mock
    private ResponseSpec responseSpec;

    private OboTokenService oboTokenService;

    @BeforeEach
    void setUp() {
        oboTokenService = new OboTokenService(cacheManager, jwtDecoder, oboRestClient);
        ReflectionTestUtils.setField(oboTokenService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(oboTokenService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(oboTokenService, "dataApiScope", "api://test-data-api/.default");
        ReflectionTestUtils.setField(oboTokenService, "tokenEndpoint", TOKEN_ENDPOINT);
    }

    @Test
    void acquireOboToken_returnsCachedToken_whenCacheHitAndNotExpired() {
        Jwt validJwt = buildJwt(Instant.now().plusSeconds(600));
        when(cacheManager.getCache(CachingConfig.USER_DATA_API_OBO_TOKENS_CACHE)).thenReturn(cache);
        when(cache.get(USER_OID, String.class)).thenReturn(OBO_TOKEN);
        when(jwtDecoder.decode(OBO_TOKEN)).thenReturn(validJwt);

        String result = oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID);

        assertThat(result).isEqualTo(OBO_TOKEN);
        verify(oboRestClient, never()).post();
    }

    @Test
    void acquireOboToken_exchangesToken_whenCacheMiss() {
        OboTokenResponse tokenResponse = buildOboResponse(OBO_TOKEN);
        when(cacheManager.getCache(CachingConfig.USER_DATA_API_OBO_TOKENS_CACHE)).thenReturn(cache);
        when(cache.get(USER_OID, String.class)).thenReturn(null);
        stubRestClientPost(tokenResponse);

        String result = oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID);

        assertThat(result).isEqualTo(OBO_TOKEN);
        verify(cache).put(USER_OID, OBO_TOKEN);
    }

    @Test
    void acquireOboToken_exchangesToken_whenCachedTokenExpired() {
        Jwt expiredJwt = buildJwt(Instant.now().minusSeconds(60));
        OboTokenResponse tokenResponse = buildOboResponse(OBO_TOKEN);
        when(cacheManager.getCache(CachingConfig.USER_DATA_API_OBO_TOKENS_CACHE)).thenReturn(cache);
        when(cache.get(USER_OID, String.class)).thenReturn("expired-token");
        when(jwtDecoder.decode("expired-token")).thenReturn(expiredJwt);
        stubRestClientPost(tokenResponse);

        String result = oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID);

        assertThat(result).isEqualTo(OBO_TOKEN);
        verify(cache).put(USER_OID, OBO_TOKEN);
    }

    @Test
    void acquireOboToken_exchangesToken_whenJwtDecoderThrows() {
        OboTokenResponse tokenResponse = buildOboResponse(OBO_TOKEN);
        when(cacheManager.getCache(CachingConfig.USER_DATA_API_OBO_TOKENS_CACHE)).thenReturn(cache);
        when(cache.get(USER_OID, String.class)).thenReturn("bad-token");
        when(jwtDecoder.decode("bad-token")).thenThrow(new RuntimeException("decode failed"));
        stubRestClientPost(tokenResponse);

        String result = oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID);

        assertThat(result).isEqualTo(OBO_TOKEN);
    }

    @Test
    void acquireOboToken_throwsIllegalState_whenResponseIsNull() {
        when(cacheManager.getCache(CachingConfig.USER_DATA_API_OBO_TOKENS_CACHE)).thenReturn(cache);
        when(cache.get(USER_OID, String.class)).thenReturn(null);
        stubRestClientPost(null);

        assertThatThrownBy(() -> oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("null access_token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void acquireOboToken_sendsCorrectGrantTypeInBody() {
        OboTokenResponse tokenResponse = buildOboResponse(OBO_TOKEN);
        when(cacheManager.getCache(CachingConfig.USER_DATA_API_OBO_TOKENS_CACHE)).thenReturn(cache);
        when(cache.get(USER_OID, String.class)).thenReturn(null);

        ArgumentCaptor<MultiValueMap<String, String>> bodyCaptor = ArgumentCaptor.forClass(MultiValueMap.class);
        when(oboRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(TOKEN_ENDPOINT)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(bodyCaptor.capture())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(OboTokenResponse.class))).thenReturn(tokenResponse);

        oboTokenService.acquireOboToken(USER_ACCESS_TOKEN, USER_OID);

        MultiValueMap<String, String> body = bodyCaptor.getValue();
        assertThat(body.getFirst("grant_type")).isEqualTo("urn:ietf:params:oauth2:grant-type:jwt-bearer");
        assertThat(body.getFirst("requested_token_use")).isEqualTo("on_behalf_of");
        assertThat(body.getFirst("assertion")).isEqualTo(USER_ACCESS_TOKEN);
        assertThat(body.getFirst("client_id")).isEqualTo("test-client-id");
        assertThat(body.getFirst("scope")).isEqualTo("api://test-data-api/.default");
    }

    private void stubRestClientPost(OboTokenResponse response) {
        when(oboRestClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(TOKEN_ENDPOINT)).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(OboTokenResponse.class))).thenReturn(response);
    }

    private Jwt buildJwt(Instant expiresAt) {
        return Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user")
            .issuedAt(Instant.now().minusSeconds(60))
            .expiresAt(expiresAt)
            .build();
    }

    private OboTokenResponse buildOboResponse(String accessToken) {
        if (accessToken == null) {
            return null;
        }
        OboTokenResponse response = new OboTokenResponse();
        ReflectionTestUtils.setField(response, "accessToken", accessToken);
        ReflectionTestUtils.setField(response, "expiresIn", 3600L);
        return response;
    }
}
