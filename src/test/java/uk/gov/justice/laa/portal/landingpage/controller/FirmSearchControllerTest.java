package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

@ExtendWith(MockitoExtension.class)
class FirmSearchControllerTest {

    private FirmSearchController firmSearchController;

    @Mock
    private LoginService loginService;
    @Mock
    private FirmService firmService;
    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        firmSearchController = new FirmSearchController(loginService, firmService);
    }

    @Test
    void getFirms_WithSearchQuery_ReturnsFilteredFirms() {
        // Arrange
        String searchQuery = "Firm 1";
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        FirmDto firm1 = new FirmDto(UUID.randomUUID(), "Test Firm 1", "F1", null, false, false, false);

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(entraUser, searchQuery)).thenReturn(List.of(firm1));

        // Act
        List<FirmDto> result = firmSearchController.getFirms(authentication, searchQuery);

        // Assert
        assertThat(result).isEqualTo(List.of(firm1));
        verify(firmService).getUserAccessibleFirms(entraUser, searchQuery);
    }

    @Test
    void getFirms_WithSearchQuery_ReturnsFilteredFirms_By_Code() {
        // Arrange
        String searchQuery = "F2";
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        FirmDto firm2 = new FirmDto(UUID.randomUUID(), "Test Firm 2", "F2", null, false, false, false);

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(firmService.getUserAccessibleFirms(entraUser, searchQuery)).thenReturn(List.of(firm2));

        // Act
        List<FirmDto> result = firmSearchController.getFirms(authentication, searchQuery);

        // Assert
        assertThat(result).isEqualTo(List.of(firm2));
        verify(firmService).getUserAccessibleFirms(entraUser, searchQuery);
    }

    @Test
    void getFirms_WithoutSearchQuery_ReturnsAllFirms() {
        // Arrange
        String searchQuery = "";
        EntraUser entraUser = EntraUser.builder().id(UUID.randomUUID()).build();
        List<FirmDto> expectedFirms = List.of(
                new FirmDto(UUID.randomUUID(), "Firm A", "F1", null, false, false, false),
                new FirmDto(UUID.randomUUID(), "Firm B", "F2", null, false, false, false));

        // Act
        List<FirmDto> result = firmSearchController.getFirms(authentication, searchQuery);

        // Assert
        assertThat(result).isEmpty();
        verify(firmService, never()).getUserAccessibleFirms(any(), any());
    }

    @Test
    void testSearchFirms_ShouldReturnFirmList() {
        // Given
        String query = "Test Firm";
        List<FirmDto> mockFirms = List.of(
                FirmDto.builder()
                        .id(UUID.randomUUID())
                        .name("Test Firm 1")
                        .code("TF001")
                        .build(),
                FirmDto.builder()
                        .id(UUID.randomUUID())
                        .name("Test Firm 2")
                        .code("TF002")
                        .build());

        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(query, 10);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("name")).isEqualTo("Test Firm 1");
        assertThat(result.get(0).get("code")).isEqualTo("TF001");
        assertThat(result.get(1).get("name")).isEqualTo("Test Firm 2");
        assertThat(result.get(1).get("code")).isEqualTo("TF002");
        verify(firmService).searchFirms(query);
    }

    @Test
    void testSearchFirms_ShouldReturnSubsetOfFirmList() {
        // Given
        String query = "Test Firm";
        List<FirmDto> mockFirms = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> FirmDto.builder().id(UUID.randomUUID()).name("Test Firm " + i)
                        .code(String.format("TF%03d", i)).build())
                .collect(Collectors.toList());

        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(query, 15);

        // Then
        assertThat(result).hasSize(15);
        verify(firmService).searchFirms(query);
    }

    @Test
    void testSearchFirms_ShouldReturnDefaultCountOfTenFirmsList() {
        // Given
        String query = "Test Firm";
        List<FirmDto> mockFirms = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> FirmDto.builder().id(UUID.randomUUID()).name("Test Firm " + i)
                        .code(String.format("TF%03d", i)).build())
                .collect(Collectors.toList());

        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(query, 5);

        // Then
        assertThat(mockFirms).hasSize(20);
        assertThat(result).hasSize(10);
        verify(firmService).searchFirms(query);
    }

    @Test
    void testSearchFirms_ShouldReturnMaxAllowedCountOfHundredFirmsList() {
        // Given
        String query = "Test Firm";
        List<FirmDto> mockFirms = IntStream.rangeClosed(1, 200)
                .mapToObj(i -> FirmDto.builder().id(UUID.randomUUID()).name("Test Firm " + i)
                        .code(String.format("TF%03d", i)).build())
                .collect(Collectors.toList());

        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(query, 101);

        // Then
        assertThat(mockFirms).hasSize(200);
        assertThat(result).hasSize(100);
        verify(firmService).searchFirms(query);
    }

    @Test
    void testSearchFirms_WithEmptyQuery_ShouldReturnAllFirms() {
        // Given - Empty query should now return empty result without calling service
        String query = "";

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(query, 10);

        // Then - Should return empty and never call service
        assertThat(result).isEmpty();
        verify(firmService, never()).searchFirms(any());
    }

    @Test
    void testSearchFirms_WithLargeResultSet_ShouldLimitResults() {
        // Given
        String query = "Firm";
        List<FirmDto> mockFirms = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            mockFirms.add(FirmDto.builder()
                    .id(UUID.randomUUID())
                    .name("Firm " + i)
                    .code("F" + String.format("%03d", i))
                    .build());
        }

        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(query, 10);

        // Then
        assertThat(result).hasSize(10); // Should be limited to 10 results
        verify(firmService).searchFirms(query);
    }

}
