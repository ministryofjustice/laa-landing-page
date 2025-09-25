package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
 * Unit tests to verify that search validation works with 1 character minimum.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Search Validation - 1 Character Minimum")
public class SearchValidationMinimumOneTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private LoginService loginService;

    @Mock
    private FirmService firmService;

    @Mock
    private Authentication authentication;

    @Test
    @DisplayName("Should return empty results only for empty queries")
    void testEmptyQueriesReturnEmpty() {
        // Empty queries should return empty without calling service
        List<FirmDto> result1 = userController.getFirms(authentication, "");
        assertThat(result1).isEmpty();

        List<FirmDto> result2 = userController.getFirms(authentication, "   ");
        assertThat(result2).isEmpty();

        List<Map<String, String>> searchResult1 = userController.searchFirms("");
        assertThat(searchResult1).isEmpty();

        List<Map<String, String>> searchResult2 = userController.searchFirms("   ");
        assertThat(searchResult2).isEmpty();

        // Verify service was never called for empty queries
        verify(firmService, never()).getUserAccessibleFirms(any(), any());
        verify(firmService, never()).searchFirms(any());
    }

    @Test
    @DisplayName("Should process single character queries")
    void testSingleCharacterQueriesWork() {
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

        // Act - single character queries should now work
        List<FirmDto> result1 = userController.getFirms(authentication, "a");
        List<Map<String, String>> result2 = userController.searchFirms("b");

        // Assert
        assertThat(result1).hasSize(1);
        assertThat(result1.get(0).getName()).isEqualTo("Test Firm");
        assertThat(result2).hasSize(1);
        assertThat(result2.get(0).get("name")).isEqualTo("Test Firm");

        // Verify services were called
        verify(firmService).getUserAccessibleFirms(entraUser, "a");
        verify(firmService).searchFirms("b");
    }
}
