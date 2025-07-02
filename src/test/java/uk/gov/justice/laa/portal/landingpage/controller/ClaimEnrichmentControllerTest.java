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
import uk.gov.justice.laa.portal.landingpage.dto.EntraAuthenticationContext;
import uk.gov.justice.laa.portal.landingpage.dto.EntraClaimData;
import uk.gov.justice.laa.portal.landingpage.dto.EntraClientDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraServicePrincipalDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserPayloadDto;
import uk.gov.justice.laa.portal.landingpage.service.ClaimEnrichmentService;

import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


class ClaimEnrichmentControllerTest {

    private static final String TEST_APP_NAME = "Test App";
    private static final String TEST_USER_ID = "test-user-id";

    @Mock
    private ClaimEnrichmentService claimEnrichmentService;
    @InjectMocks
    private ClaimEnrichmentController controller;

    private ClaimEnrichmentRequest testRequest;
    private ClaimEnrichmentResponse testResponse;
    private List<String> testRoles;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create mock EntraUserPayloadDto
        EntraUserPayloadDto userPayload = EntraUserPayloadDto.builder()
                .id(TEST_USER_ID)
                .displayName("Test User")
                .userPrincipalName("test.user@justice.gov.uk")
                .build();

        // Create mock EntraServicePrincipalDto for client
        EntraServicePrincipalDto clientServicePrincipal = EntraServicePrincipalDto.builder()
                .appDisplayName("Test Application")
                .appId("test-app-id")
                .displayName("Test Service Principal")
                .id("sp-id-123")
                .build();

        // Create mock EntraServicePrincipalDto for resource
        EntraServicePrincipalDto resourceServicePrincipal = EntraServicePrincipalDto.builder()
                .appDisplayName("Test Resource")
                .appId("test-resource-id")
                .displayName("Test Resource Principal")
                .id("sp-id-456")
                .build();

        // Create mock EntraClientDto
        EntraClientDto clientDto = EntraClientDto.builder()
                .locale("en-US")
                .build();

        // Create mock EntraAuthenticationContext with new structure
        EntraAuthenticationContext authContext = EntraAuthenticationContext.builder()
                .client(clientDto)
                .clientServicePrincipal(clientServicePrincipal)
                .resourceServicePrincipal(resourceServicePrincipal)
                .user(userPayload)
                .build();

        // Create mock EntraClaimData with new structure
        EntraClaimData claimData = EntraClaimData.builder()
                .authenticationContext(authContext)
                .tenantId("test-tenant-id")
                .build();

        // Setup test request
        testRequest = ClaimEnrichmentRequest.builder()
                .data(claimData)
                .build();

        // Setup test roles
        testRoles = List.of("ROLE_USER", "ROLE_ADMIN");

        // Setup test response
        testResponse = ClaimEnrichmentResponse.builder()
                .success(true)
                .laa_app_roles(testRoles)
                .correlationId("test-correlation-id")
                .user_name(TEST_USER_ID)
                .user_email("test@example.com")
                .message("Access granted to " + TEST_APP_NAME)
                .laa_accounts(List.of("office-1", "office-2"))
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
        assertEquals(testRoles, response.getBody().getLaa_app_roles());
        assertEquals("test-correlation-id", response.getBody().getCorrelationId());
        assertEquals(TEST_USER_ID, response.getBody().getUser_name());
        assertEquals("test@example.com", response.getBody().getUser_email());
        assertEquals(List.of("office-1", "office-2"), response.getBody().getLaa_accounts());
        assertEquals("Access granted to " + TEST_APP_NAME, response.getBody().getMessage());

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