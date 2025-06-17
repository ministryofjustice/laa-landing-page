package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.service.ClaimEnrichmentInterface;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class ClaimEnrichmentControllerTest {

    @Mock
    private ClaimEnrichmentInterface claimEnrichmentService;

    @InjectMocks
    private ClaimEnrichmentController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void enrichClaims_Success() {
        // Arrange
        ClaimEnrichmentRequest request = new ClaimEnrichmentRequest();
        request.setUserId("test-user");
        request.setToken("test-token");
        request.setTargetAppId("123e4567-e89b-12d3-a456-426614174000");

        Set<String> roles = new HashSet<>();
        roles.add("ROLE_USER");
        
        ClaimEnrichmentResponse mockResponse = ClaimEnrichmentResponse.builder()
            .success(true)
            .roles(roles)
            .appName("Test App")
            .message("Access granted to Test App")
            .build();

        when(claimEnrichmentService.enrichClaims(any())).thenReturn(mockResponse);

        // Act
        ResponseEntity<ClaimEnrichmentResponse> response = controller.enrichClaims(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Test App", response.getBody().getAppName());
        assertEquals(1, response.getBody().getRoles().size());
        assertTrue(response.getBody().getRoles().contains("ROLE_USER"));
        
        verify(claimEnrichmentService, times(1)).enrichClaims(any());
    }
}
