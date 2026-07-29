package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

/**
 * Unit tests to verify that search validation is working correctly
 * across all endpoints that implement minimum search length requirements.
 * Tests the 1-character minimum search requirement.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Search Validation Unit Tests")
public class SearchValidationTest {

    @InjectMocks
    private FirmSearchController firmSearchController;

    @Mock
    private LoginService loginService;

    @Mock
    private FirmService firmService;

    @Mock
    private Authentication authentication;

    @Mock
    private AccessControlService accessControlService;

    @BeforeEach
    void setUp() {
        // Reset mocks between tests to avoid cross-test interference
        reset(firmService, loginService, accessControlService);
    }

    @Test
    @DisplayName("Should return empty results only for empty queries in firm search endpoints")
    void testMinimumSearchLengthValidationForFirmEndpoints() {
        // Test /admin/user/firms/search endpoint
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(false);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(any(), any())).thenReturn(List.of());

        // Test with empty query - should not call service
        List<FirmDto> result1 = firmSearchController.getFirms(authentication, "");
        assertThat(result1).isEmpty();

        // Test with whitespace only - should not call service
        List<FirmDto> result4 = firmSearchController.getFirms(authentication, "   ");
        assertThat(result4).isEmpty();

        // Verify that service was not called for empty queries
        verify(firmService, never()).getUserAccessibleFirms(any(), eq(""));
        verify(firmService, never()).getUserAccessibleFirms(any(), eq("   "));

        // Test with single character - should now call service (1-character minimum)
        List<FirmDto> result2 = firmSearchController.getFirms(authentication, "a");
        assertThat(result2).isEmpty(); // Empty result but service was called

        // Test with two characters - should call service
        List<FirmDto> result3 = firmSearchController.getFirms(authentication, "ab");
        assertThat(result3).isEmpty(); // Empty result but service was called

        // Verify that service was called for valid queries (1+ characters)
        verify(firmService).getUserAccessibleFirms(entraUser, "a");
        verify(firmService).getUserAccessibleFirms(entraUser, "ab");

        // Test /admin/user/create/firm/search endpoint
        when(firmService.getUserAccessibleFirms(any(), any())).thenReturn(List.of());
        // Empty query - should not call service
        List<Map<String, String>> searchResult1 = firmSearchController.searchFirms(authentication, "", 10);
        assertThat(searchResult1).isEmpty();

        // Single character - should call service
        List<Map<String, String>> searchResult2 = firmSearchController.searchFirms(authentication, "x", 10);
        assertThat(searchResult2).isEmpty(); // Empty result but service was called

        // Two characters - should call service
        List<Map<String, String>> searchResult3 = firmSearchController.searchFirms(authentication, "xy", 10);
        assertThat(searchResult3).isEmpty(); // Empty result but service was called

        // Verify service calls for searchFirms
        verify(firmService, never()).getUserAccessibleFirms(any(), eq(""));
        verify(firmService).getUserAccessibleFirms(entraUser, "x");
        verify(firmService).getUserAccessibleFirms(entraUser, "xy");
    }

    @Test
    @DisplayName("Should process queries that meet the 1 character minimum requirement")
    void testValidQueriesProcessedCorrectly() {
        // Arrange
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(false);
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        FirmDto testFirm = FirmDto.builder()
                .id(UUID.randomUUID())
                .name("Test Firm")
                .code("TF001")
                .build();

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(entraUser, "a")).thenReturn(List.of(testFirm));
        when(firmService.getUserAccessibleFirms(entraUser, "b")).thenReturn(List.of(testFirm));


        // Test /admin/user/firms/search endpoint with single character (now valid)
        List<FirmDto> result1 = firmSearchController.getFirms(authentication, "a");
        assertThat(result1).hasSize(1);
        assertThat(result1.get(0).getName()).isEqualTo("Test Firm");

        // Test /admin/user/create/firm/search endpoint with single character (now valid)
        List<Map<String, String>> result2 = firmSearchController.searchFirms(authentication, "b", 10);
        assertThat(result2).hasSize(1);
        assertThat(result2.get(0).get("name")).isEqualTo("Test Firm");

        // Verify services were called for valid queries
        verify(firmService).getUserAccessibleFirms(entraUser, "a");
        verify(firmService).getUserAccessibleFirms(entraUser, "b");
        verify(firmService, never()).searchFirms(anyString());
    }

    @Test
    @DisplayName("Should handle edge cases correctly for external users")
    void testEdgeCases() {
        // Test exactly 1 character (should be processed)
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(false);
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        lenient().when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(entraUser, "1")).thenReturn(List.of());
        when(firmService.getUserAccessibleFirms(entraUser, "2")).thenReturn(List.of());

        List<FirmDto> result1 = firmSearchController.getFirms(authentication, "1");
        assertThat(result1).isEmpty(); // Empty but service was called

        List<Map<String, String>> result2 = firmSearchController.searchFirms(authentication, "2", 10);
        assertThat(result2).isEmpty(); // Empty but service was called

        verify(firmService).getUserAccessibleFirms(entraUser, "1");

        // Test query with leading/trailing spaces
        // getFirms passes the original query to service, but searchFirms trims first
        when(firmService.getUserAccessibleFirms(entraUser, "  a  ")).thenReturn(List.of());
        when(firmService.getUserAccessibleFirms(entraUser, "b")).thenReturn(List.of());

        List<FirmDto> result3 = firmSearchController.getFirms(authentication, "  a  ");
        assertThat(result3).isEmpty(); // Empty but service was called (with original "  a  ")

        List<Map<String, String>> result4 = firmSearchController.searchFirms(authentication, "  b  ", 10);
        assertThat(result4).isEmpty(); // Empty but service was called (with trimmed "b")

        // Verify the service was called - getFirms passes untrimmed, searchFirms passes trimmed
        verify(firmService).getUserAccessibleFirms(entraUser, "  a  ");
        verify(firmService).getUserAccessibleFirms(entraUser, "b");
    }

    @Test
    @DisplayName("Should use global firm search for internal users")
    void testSearchFirmsUsesGlobalSearchForInternalUser() {
        // Given
        EntraUser entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .build();

        FirmDto firm = FirmDto.builder()
                .id(UUID.randomUUID())
                .name("Internal Firm")
                .code("IF001")
                .build();

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);
        when(firmService.searchFirms("test")).thenReturn(List.of(firm));

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, "test", 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name"))
                .isEqualTo("Internal Firm");

        verify(firmService).searchFirms("test");
        verify(firmService, never())
                .getUserAccessibleFirms(any(), any());
    }

    @Test
    @DisplayName("Should trim search query for internal users")
    void testSearchFirmsTrimsQueryForInternalUser() {
        // Given
        EntraUser entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .build();

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);
        when(firmService.searchFirms("test")).thenReturn(List.of());

        // When
        firmSearchController.searchFirms(authentication, "  test  ", 10);

        // Then
        verify(firmService).searchFirms("test");
        verify(firmService, never()).searchFirms("  test  ");
        verify(firmService, never()).getUserAccessibleFirms(any(), any());
    }
}
