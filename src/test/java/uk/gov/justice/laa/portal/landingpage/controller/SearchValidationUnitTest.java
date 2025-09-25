package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

/**
 * Unit tests to verify that search validation is working correctly
 * across all endpoints that implement minimum search length requirements.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Search Validation Unit Tests - 1 Character Minimum")
public class SearchValidationUnitTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private LoginService loginService;

    @Mock
    private FirmService firmService;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Reset mocks between tests to avoid cross-test interference
        reset(firmService, loginService);
    }

    @Test
    @DisplayName("Should return empty results for empty queries in firm search endpoints")
    void testMinimumSearchLengthValidationForFirmEndpoints() {
        // Test /admin/user/firms/search endpoint - only empty queries should return empty without calling service
        List<FirmDto> result1 = userController.getFirms(authentication, "");
        assertThat(result1).isEmpty();

        List<FirmDto> result2 = userController.getFirms(authentication, "   ");
        assertThat(result2).isEmpty();

        // Verify that service was never called for empty queries
        verify(firmService, never()).getUserAccessibleFirms(any(), any());

        // Test /admin/user/create/firm/search endpoint
        List<Map<String, String>> searchResult1 = userController.searchFirms("");
        assertThat(searchResult1).isEmpty();

        List<Map<String, String>> searchResult2 = userController.searchFirms("   ");
        assertThat(searchResult2).isEmpty();

        // Verify that service was never called for empty queries
        verify(firmService, never()).searchFirms(any());
    }

    @Test
    @DisplayName("Should process queries that meet the 1 character minimum requirement")
    void testValidQueriesProcessedCorrectly() {
        // Arrange
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        FirmDto testFirm = FirmDto.builder()
                .id(UUID.randomUUID())
                .name("Test Firm")
                .code("TF001")
                .build();

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(entraUser, "a")).thenReturn(List.of(testFirm));
        when(firmService.searchFirms("b")).thenReturn(List.of(testFirm));

        // Test /admin/user/firms/search endpoint with valid query
        List<FirmDto> result1 = userController.getFirms(authentication, "a");
        assertThat(result1).hasSize(1);
        assertThat(result1.get(0).getName()).isEqualTo("Test Firm");

        // Test /admin/user/create/firm/search endpoint with valid query
        List<Map<String, String>> result2 = userController.searchFirms("b");
        assertThat(result2).hasSize(1);
        assertThat(result2.get(0).get("name")).isEqualTo("Test Firm");

        // Verify services were called for valid queries
        verify(firmService).getUserAccessibleFirms(entraUser, "a");
        verify(firmService).searchFirms("b");
    }

    @Test
    @DisplayName("Should handle edge cases correctly")
    void testEdgeCases() {
        // Test exactly 1 character (should be processed)
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(entraUser, "a")).thenReturn(List.of());
        when(firmService.searchFirms("b")).thenReturn(List.of());

        List<FirmDto> result1 = userController.getFirms(authentication, "a");
        assertThat(result1).isEmpty(); // Empty but service was called

        List<Map<String, String>> result2 = userController.searchFirms("b");
        assertThat(result2).isEmpty(); // Empty but service was called

        verify(firmService).getUserAccessibleFirms(entraUser, "a");
        verify(firmService).searchFirms("b");

        // Test query with leading/trailing spaces that becomes empty when trimmed
        List<FirmDto> result3 = userController.getFirms(authentication, "   ");
        assertThat(result3).isEmpty();

        List<Map<String, String>> result4 = userController.searchFirms("   ");
        assertThat(result4).isEmpty();

        // These should not have called the service because trimmed length is < 1
        verify(firmService, never()).getUserAccessibleFirms(any(), eq(""));
        verify(firmService, never()).searchFirms(eq(""));
    }
}
