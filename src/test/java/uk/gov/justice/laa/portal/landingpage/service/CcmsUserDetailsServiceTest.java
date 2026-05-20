package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetails;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetailsResponse;
import uk.gov.justice.laa.portal.landingpage.registry.CcmsUdaRegistry;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcmsUserDetailsServiceTest {

    private static final String APP_OID = "app-oid";
    private static final String LEGACY_USER_ID = "legacy123";
    private static final String BASE_URL = "http://uda-host";
    private static final String API_KEY = "api-key";

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CcmsUdaRegistry ccmsUdaRegistry;
    @InjectMocks
    private CcmsUserDetailsService service;
    private CcmsUserDetailsService spyService;

    @BeforeEach
    void setUp() {
        spyService = Mockito.spy(service);
    }

    @Test
    void getUserDetailsByLegacyUserId_WhenCallFails_ReturnsNull() {
        doReturn(restTemplate).when(spyService).createRestTemplate();
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(CcmsUserDetailsResponse.class)))
                .thenThrow(new RuntimeException("boom"));

        CcmsUserDetailsResponse response = spyService.getUserDetailsByLegacyUserId(APP_OID, "some-legacy-id");

        assertNull(response);
    }

    @Test
    void getUserDetailsByLegacyUserId_WhenCallSucceeds_ReturnsBody() {
        CcmsUserDetails ccmsUserDetails = new CcmsUserDetails();
        ccmsUserDetails.setUserName("CCMS_USER_FROM_API");
        CcmsUserDetailsResponse body = new CcmsUserDetailsResponse();
        body.setCcmsUserDetails(ccmsUserDetails);

        ResponseEntity<CcmsUserDetailsResponse> responseEntity =
                new ResponseEntity<>(body, HttpStatus.OK);

        doReturn(restTemplate).when(spyService).createRestTemplate();
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(CcmsUserDetailsResponse.class)))
                .thenReturn(responseEntity);

        CcmsUserDetailsResponse response = spyService.getUserDetailsByLegacyUserId(APP_OID, "legacy-id");

        assertEquals("CCMS_USER_FROM_API", response.getCcmsUserDetails().getUserName());
    }

    @Test
    void shouldReturnNullWhenBaseUrlMissing() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.empty());
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));

        CcmsUserDetailsResponse result =
                spyService.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldReturnNullWhenApiKeyMissing() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.empty());

        CcmsUserDetailsResponse result =
                spyService.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldReturnNullWhenBaseUrlIsNone() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of("NONE"));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));

        CcmsUserDetailsResponse result =
                spyService.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void shouldReturnUserDetailsOn2xxResponse() {
        CcmsUserDetailsResponse responseBody = new CcmsUserDetailsResponse();

        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        doReturn(restTemplate).when(spyService).createRestTemplate();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CcmsUserDetailsResponse.class)
        )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        CcmsUserDetailsResponse result =
                spyService.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertSame(responseBody, result);
    }

    @Test
    void shouldReturnNullOn404Response() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        doReturn(restTemplate).when(spyService).createRestTemplate();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CcmsUserDetailsResponse.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.NOT_FOUND));

        CcmsUserDetailsResponse result =
                spyService.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
    }

    @Test
    void shouldReturnNullOnUnexpectedHttpStatus() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        doReturn(restTemplate).when(spyService).createRestTemplate();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CcmsUserDetailsResponse.class)
        )).thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_GATEWAY));

        CcmsUserDetailsResponse result =
                spyService.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenExceptionIsThrown() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        doReturn(restTemplate).when(spyService).createRestTemplate();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(CcmsUserDetailsResponse.class)
        )).thenThrow(new RuntimeException("boom"));

        CcmsUserDetailsResponse result =
                spyService.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        assertNull(result);
    }

    @Test
    void shouldCallCorrectUrlAndSetAuthorizationHeader() {
        when(ccmsUdaRegistry.getUdaBaseUrl(APP_OID)).thenReturn(Optional.of(BASE_URL));
        when(ccmsUdaRegistry.getUdaApiKey(APP_OID)).thenReturn(Optional.of(API_KEY));
        doReturn(restTemplate).when(spyService).createRestTemplate();

        when(restTemplate.exchange(
                anyString(),
                any(),
                any(),
                eq(CcmsUserDetailsResponse.class)
        )).thenReturn(new ResponseEntity<>(new CcmsUserDetailsResponse(), HttpStatus.OK));

        spyService.getUserDetailsByLegacyUserId(APP_OID, LEGACY_USER_ID);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<HttpEntity<Void>> entityCaptor =
                ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
                eq(CcmsUserDetailsResponse.class)
        );

        assertEquals(
                "http://uda-host/api/v1/user-details/silas/legacy123",
                urlCaptor.getValue()
        );
        assertEquals(
                API_KEY,
                entityCaptor.getValue().getHeaders().getFirst("X-Authorization")
        );
    }

}
