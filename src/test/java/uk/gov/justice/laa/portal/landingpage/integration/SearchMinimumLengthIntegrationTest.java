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
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;

import uk.gov.justice.laa.portal.landingpage.controller.UserController;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

/**
 * Unit tests to verify search minimum length validation works correctly.
 * Tests that 1-character searches are now processed (not blocked by minimum length validation).
 */
@ExtendWith(MockitoExtension.class)
public class SearchMinimumLengthIntegrationTest {

    private UserController userController;
    
    @Mock private FirmService firmService;
    @Mock private LoginService loginService;
    @Mock private UserService userService;
    @Mock private OfficeService officeService;
    @Mock private EventService eventService;
    @Mock private ModelMapper mapper;
    @Mock private AccessControlService accessControlService;
    @Mock private RoleAssignmentService roleAssignmentService;
    @Mock private EmailValidationService emailValidationService;
    @Mock private Authentication authentication;

    @BeforeEach
    void setUp() {
        userController = new UserController(
            loginService, userService, officeService, eventService, 
            firmService, mapper, accessControlService, roleAssignmentService, 
            emailValidationService
        );
    }

    @Test
    public void testEmptyQueriesReturnEmpty() {
        // Empty queries should return empty lists (due to validation)
        List<Map<String, String>> result1 = userController.searchFirms("", 10);
        assertThat(result1).isEmpty();

        List<Map<String, String>> result2 = userController.searchFirms("   ", 10);
        assertThat(result2).isEmpty();
        
        // Verify that service methods were not called for empty queries
        verify(firmService, org.mockito.Mockito.never()).searchFirms(any());
    }

    @Test 
    public void testSingleCharacterQueriesWork() {
        // Setup mocks
        when(firmService.searchFirms("A")).thenReturn(List.of());
        
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(entraUser, "B")).thenReturn(List.of());
        
        // Single character queries should now work (not return empty due to minimum length restriction)
        List<Map<String, String>> result1 = userController.searchFirms("A", 10);
        assertThat(result1).isNotNull();
        
        List<FirmDto> result2 = userController.getFirms(authentication, "B");
        assertThat(result2).isNotNull();
        
        // Verify that service methods were called for single character queries
        verify(firmService).searchFirms("A");
        verify(firmService).getUserAccessibleFirms(entraUser, "B");
    }
}
