package uk.gov.justice.laa.portal.landingpage.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import uk.gov.justice.laa.portal.landingpage.controller.FirmSearchController;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

/**
 * Unit tests to verify search minimum length validation works correctly.
 * Tests that 1-character searches are now processed (not blocked by minimum length validation).
 */
@ExtendWith(MockitoExtension.class)
public class SearchMinimumLengthIntegrationTest {

    private FirmSearchController firmSearchController;
    
    @Mock private FirmService firmService;
    @Mock private LoginService loginService;
    @Mock private Authentication authentication;
    @Mock private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        firmSearchController = new FirmSearchController(
            loginService, firmService, accessControlService
        );
    }

    @Test
    public void testEmptyQueriesReturnEmpty() {
        // Empty queries should return empty lists (due to validation)
        List<Map<String, String>> result1 = firmSearchController.searchFirms(authentication, "", 10);
        assertThat(result1).isEmpty();

        List<Map<String, String>> result2 = firmSearchController.searchFirms(authentication, "   ", 10);
        assertThat(result2).isEmpty();
        
        // Verify that service methods were not called for empty queries
        verify(firmService, org.mockito.Mockito.never()).searchFirms(any());
    }

    @Test 
    public void testSingleCharacterQueriesWorkInternal() {
        // Setup mocks
        when(firmService.searchFirms("A")).thenReturn(List.of());
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);
        
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        
        // Single character queries should now work (not return empty due to minimum length restriction)
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, "A", 10);
        assertThat(result).isNotNull();
        
        // Verify that service methods were called for single character queries
        verify(firmService).searchFirms("A");

    }

    @Test
    public void testSingleCharacterQueriesWorkExternal() {
        // Setup mocks

        when(accessControlService.authenticatedUserIsInternal()).thenReturn(false);
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(entraUser, "B")).thenReturn(List.of());

        // Single character queries should now work (not return empty due to minimum length restriction)
        List<FirmDto> result = firmSearchController.getFirms(authentication, "B");
        assertThat(result).isNotNull();

        // Verify that service methods were called for single character queries
        verify(firmService).getUserAccessibleFirms(entraUser, "B");
    }
}
