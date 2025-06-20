package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.EntraApplicationInfo;
import uk.gov.justice.laa.portal.landingpage.dto.EntraAuthenticationContext;
import uk.gov.justice.laa.portal.landingpage.dto.EntraClaim;
import uk.gov.justice.laa.portal.landingpage.dto.EntraClaimData;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserInfo;
import uk.gov.justice.laa.portal.landingpage.service.ClaimEnrichmentService;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


class ClaimEnrichmentControllerTest {

    private static final String TEST_EVENT_TYPE = "test-event";
    private static final String TEST_EVENT_ID = "test-id";
    private static final String TEST_APP_NAME = "Test App";

    @Mock
    private ClaimEnrichmentService claimEnrichmentService;
    @InjectMocks
    private ClaimEnrichmentController controller;

    private ClaimEnrichmentRequest testRequest;
    private ClaimEnrichmentResponse testResponse;
    private Set<String> testRoles;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create mock EntraAuthenticationContext with claims
        EntraAuthenticationContext authContext = EntraAuthenticationContext.builder()
                .claims(List.of(
                    EntraClaim.builder()
                        .type("roles")
                        .value("user")
                        .build()
                ))
                .build();

        // Create mock EntraUserInfo
        EntraUserInfo userInfo = EntraUserInfo.builder()
                .id("test-user-id")
                .displayName("Test User")
                .userPrincipalName("test.user@justice.gov.uk")
                .build();

        // Create mock EntraApplicationInfo
        EntraApplicationInfo appInfo = EntraApplicationInfo.builder()
                .id("test-app-id")
                .displayName("Test Application")
                .build();

        // Create mock EntraClaimData
        EntraClaimData claimData = EntraClaimData.builder()
                .authenticationContext(authContext)
                .user(userInfo)
                .application(appInfo)
                .build();

        // Setup test request
        testRequest = ClaimEnrichmentRequest.builder()
                .data(claimData)
                .eventType(TEST_EVENT_TYPE)
                .eventId(TEST_EVENT_ID)
                .time(Instant.now())
                .build();

        // Setup test roles
        testRoles = new HashSet<>();
        testRoles.add("ROLE_USER");
        testRoles.add("ROLE_ADMIN");

        // Setup test response
        testResponse = ClaimEnrichmentResponse.builder()
                .success(true)
                .roles(testRoles)
                .appName(TEST_APP_NAME)
                .message("Access granted to " + TEST_APP_NAME)
                .build();
    }

    @Test
    void shouldSuccessfullyEnrichClaims() {
        // Arrange
        when(claimEnrichmentService.enrichClaim(testRequest)).thenReturn(testResponse);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = controller.enrichClaims(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(testRoles, response.getBody().getRoles());
        assertEquals(TEST_APP_NAME, response.getBody().getAppName());

        verify(claimEnrichmentService, times(1)).enrichClaim(testRequest);
    }

    @Test
    void shouldHandleServiceError() {
        // Arrange
        when(claimEnrichmentService.enrichClaim(any(ClaimEnrichmentRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThrows(RuntimeException.class,
                () -> controller.enrichClaims(testRequest));

        verify(claimEnrichmentService, times(1)).enrichClaim(testRequest);
    }
}
