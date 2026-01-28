package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetails;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetailsResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CcmsUserDetailsServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Spy
    private CcmsUserDetailsService ccmsUserDetailsService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ccmsUserDetailsService, "udaBaseUrl", "http://uda-host");
        ReflectionTestUtils.setField(ccmsUserDetailsService, "udaApiKey", "test-key");
        doReturn(restTemplate).when(ccmsUserDetailsService).createRestTemplate();
    }

    @Test
    void getUserDetailsByLegacyUserId_WhenCallFails_ReturnsNull() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(CcmsUserDetailsResponse.class)))
                .thenThrow(new RuntimeException("boom"));

        CcmsUserDetailsResponse response = ccmsUserDetailsService.getUserDetailsByLegacyUserId("some-legacy-id");

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

        when(restTemplate.exchange(any(String.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(CcmsUserDetailsResponse.class)))
                .thenReturn(responseEntity);

        CcmsUserDetailsResponse response = ccmsUserDetailsService.getUserDetailsByLegacyUserId("legacy-id");

        assertEquals("CCMS_USER_FROM_API", response.getCcmsUserDetails().getUserName());
    }
}
