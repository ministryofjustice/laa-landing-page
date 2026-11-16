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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.CreateUserApiService;
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
    @Mock
    private EntraUser entraUser;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private DataApiFeatureFlagService dataApiFeatureFlagService;
    @Mock
    private CreateUserApiService createUserApiService;

    @BeforeEach
    void setUp() {
        firmSearchController = new FirmSearchController(loginService, firmService, accessControlService,
                dataApiFeatureFlagService, createUserApiService);
        authentication = mock(Authentication.class);
        entraUser = mock(EntraUser.class);

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

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);
        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, query, 10);

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

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);
        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, query, 15);

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

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);
        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, query, 5);

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

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);
        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, query, 101);

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
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, query, 10);

        // Then - Should return empty and never call service
        assertThat(result).isEmpty();
        verify(firmService, never()).searchFirms(any());
    }

    @Test
    void testSearchFirms_WithLargeResultSet_ShouldLimitResults() {
        // Given
        List<FirmDto> mockFirms = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            mockFirms.add(FirmDto.builder()
                    .id(UUID.randomUUID())
                    .name("Firm " + i)
                    .code("F" + String.format("%03d", i))
                    .build());
        }

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);

        String query = "Firm";
        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, query, 10);

        // Then
        assertThat(result).hasSize(10); // Should be limited to 10 results
        verify(firmService).searchFirms(query);
    }

    @Test
    void testSearchFirms_ShouldUseAccessibleFirms_ForExternalUser() {
        // Given
        String query = "Test Firm";

        List<FirmDto> mockFirms = List.of(
                FirmDto.builder()
                        .id(UUID.randomUUID())
                        .name("Accessible Firm")
                        .code("AF001")
                        .build());

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(false);
        when(firmService.getUserAccessibleFirms(entraUser, query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, query, 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("Accessible Firm");
        assertThat(result.get(0).get("code")).isEqualTo("AF001");

        verify(firmService).getUserAccessibleFirms(entraUser, query);
        verify(firmService, never()).searchFirms(anyString());
    }

    @Test
    void testSearchFirms_ShouldLimitResults_ForExternalUser() {
        // Given
        String query = "Test Firm";

        List<FirmDto> mockFirms = IntStream.rangeClosed(1, 20)
                .mapToObj(i -> FirmDto.builder()
                        .id(UUID.randomUUID())
                        .name("Firm " + i)
                        .code(String.format("TF%03d", i))
                        .build())
                .collect(Collectors.toList());

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(false);
        when(firmService.getUserAccessibleFirms(entraUser, query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result =
                firmSearchController.searchFirms(authentication, query, 5);

        // Then
        assertThat(result).hasSize(10);

        verify(firmService).getUserAccessibleFirms(entraUser, query);
        verify(firmService, never()).searchFirms(anyString());
    }

    @Test
    void testSearchFirms_ShouldNotUseGlobalSearch_ForExternalUser() {
        String query = "Test Firm";

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(entraUser);
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(false);
        when(firmService.getUserAccessibleFirms(entraUser, query)).thenReturn(List.of());

        // When
        firmSearchController.searchFirms(authentication, query, 10);

        // Then
        verify(firmService).getUserAccessibleFirms(entraUser, query);
        verify(firmService, never()).searchFirms(anyString());
    }

    @Test
    void searchFirms_withCreateUserDataApiEnabled_usesApiSearchOnly() {
        UUID firmId = UUID.randomUUID();
        when(dataApiFeatureFlagService.isCreateUserDataApiEnabled()).thenReturn(true);
        when(createUserApiService.searchFirms("Firm", 10))
                .thenReturn(List.of(FirmDto.builder().id(firmId).name("Firm A").code("111").build()));

        List<Map<String, String>> result = firmSearchController.searchFirms(authentication, " Firm ", 10);

        assertThat(result).containsExactly(Map.of("id", firmId.toString(), "name", "Firm A", "code", "111"));
        verify(firmService, never()).searchFirms(anyString());
        verify(firmService, never()).getUserAccessibleFirms(any(), anyString());
        verify(loginService, never()).getCurrentEntraUser(any());
    }

    @Test
    void handleDataApiException_mapsStatusWithoutBody() {
        assertThat(firmSearchController.handleDataApiException(new UserDataApiClientException("x", 403))
                .getStatusCode().value()).isEqualTo(403);
        assertThat(firmSearchController.handleDataApiException(new UserDataApiClientException("x", 404))
                .getStatusCode().value()).isEqualTo(404);
        assertThat(firmSearchController.handleDataApiException(new UserDataApiClientException("x", 503))
                .getStatusCode().value()).isEqualTo(502);
    }

    @Test
    void handleClientAuthorizationRequired_returnsSignInUrl() {
        var response = firmSearchController.handleClientAuthorizationRequired(
                new org.springframework.security.oauth2.client.ClientAuthorizationRequiredException("azure"));

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).containsEntry("redirectUrl", "/oauth2/authorization/azure");
    }
}
