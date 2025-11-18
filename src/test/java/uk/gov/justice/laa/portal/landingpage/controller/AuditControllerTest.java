package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private AuditController auditController;

    @Mock
    private UserService userService;

    private PaginatedAuditUsers mockPaginatedUsers;
    private List<AppRoleDto> mockSilasRoles;
    private Model model;

    @BeforeEach
    void setUp() {
        auditController = new AuditController(userService);
        model = new ExtendedModelMap();
        // Setup mock audit users
        AuditUserDto user1 = AuditUserDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .userId(UUID.randomUUID().toString())
                .userType("External")
                .firmAssociation("Test Firm")
                .accountStatus("Active")
                .isMultiFirmUser(false)
                .profileCount(1)
                .build();

        mockPaginatedUsers = PaginatedAuditUsers.builder()
                .users(List.of(user1))
                .totalUsers(1L)
                .totalPages(1)
                .currentPage(1)
                .pageSize(10)
                .build();

        // Setup mock SiLAS roles
        AppRoleDto role1 = new AppRoleDto();
        role1.setId(UUID.randomUUID().toString());
        role1.setName("Global Admin");

        mockSilasRoles = List.of(role1);
    }

    @Test
    void displayAuditTable_withNoFilters_returnsAuditView() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), anyString(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.getAttribute("users")).isEqualTo(mockPaginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        assertThat(model.getAttribute("actualPageSize")).isEqualTo(1);
        assertThat(model.getAttribute("page")).isEqualTo(1);
        assertThat(model.getAttribute("totalUsers")).isEqualTo(1L);
        assertThat(model.getAttribute("totalPages")).isEqualTo(1);
        assertThat(model.getAttribute("search")).isEqualTo("");
        assertThat(model.getAttribute("selectedSilasRole")).isEqualTo("");
        assertThat(model.getAttribute("sort")).isEqualTo("name");
        assertThat(model.getAttribute("direction")).isEqualTo("asc");
        assertThat(model.getAttribute("silasRoles")).isEqualTo(mockSilasRoles);

        verify(userService, times(1)).getAuditUsers("", null, "", 1, 10, "name", "asc");
        verify(userService, times(1)).getAllSilasRoles();
    }

    @Test
    void displayAuditTable_withSearchTerm_filtersResults() {
        // Given
        when(userService.getAuditUsers(eq("john"), any(), anyString(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "john", "", null, "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.getAttribute("search")).isEqualTo("john");

        verify(userService, times(1)).getAuditUsers("john", null, "", 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withValidFirmId_filtersResults() {
        // Given
        UUID firmId = UUID.randomUUID();
        when(userService.getAuditUsers(anyString(), eq(firmId), anyString(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", firmId.toString(), "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");

        verify(userService, times(1)).getAuditUsers("", firmId, "", 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withInvalidFirmId_ignoresFilter() {
        // Given
        when(userService.getAuditUsers(anyString(), isNull(), anyString(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", "invalid-uuid", "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");

        verify(userService, times(1)).getAuditUsers("", null, "", 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withSilasRole_filtersResults() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), eq("Global Admin"), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "Global Admin", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.getAttribute("selectedSilasRole")).isEqualTo("Global Admin");

        verify(userService, times(1)).getAuditUsers("", null, "Global Admin", 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomPageSize_usesProvidedSize() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), anyString(), anyInt(), eq(25),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(25, 1, "name", "asc", "", "", null, "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(25);

        verify(userService, times(1)).getAuditUsers("", null, "", 1, 25, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomPage_usesProvidedPage() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), anyString(), eq(2), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 2, "name", "asc", "", "", null, "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.getAttribute("page")).isEqualTo(2);

        verify(userService, times(1)).getAuditUsers("", null, "", 2, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomSort_usesProvidedSort() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), anyString(), anyInt(), anyInt(),
                eq("email"), eq("desc"))).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "email", "desc", "", "", null, "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.getAttribute("sort")).isEqualTo("email");
        assertThat(model.getAttribute("direction")).isEqualTo("desc");

        verify(userService, times(1)).getAuditUsers("", null, "", 1, 10, "email", "desc");
    }

    @Test
    void displayAuditTable_withAllFilters_combinesAllFilters() {
        // Given
        UUID firmId = UUID.randomUUID();
        when(userService.getAuditUsers(eq("test"), eq(firmId), eq("Global Admin"), eq(2), eq(25),
                eq("email"), eq("desc"))).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(25, 2, "email", "desc", "test", "", firmId.toString(),
                "Global Admin", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.getAttribute("search")).isEqualTo("test");
        assertThat(model.getAttribute("selectedSilasRole")).isEqualTo("Global Admin");
        assertThat(model.getAttribute("page")).isEqualTo(2);
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(25);
        assertThat(model.getAttribute("sort")).isEqualTo("email");
        assertThat(model.getAttribute("direction")).isEqualTo("desc");

        verify(userService, times(1)).getAuditUsers("test", firmId, "Global Admin", 2, 25, "email", "desc");
    }

    @Test
    void displayAuditTable_withEmptyResults_returnsEmptyList() {
        // Given
        PaginatedAuditUsers emptyResults = PaginatedAuditUsers.builder()
                .users(Collections.emptyList())
                .totalUsers(0L)
                .totalPages(0)
                .currentPage(1)
                .pageSize(10)
                .build();

        when(userService.getAuditUsers(anyString(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(emptyResults);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.getAttribute("users")).isEqualTo(Collections.emptyList());
        assertThat(model.getAttribute("totalUsers")).isEqualTo(0L);
        assertThat(model.getAttribute("totalPages")).isEqualTo(0);
        assertThat(model.getAttribute("actualPageSize")).isEqualTo(0);
    }

    @Test
    void displayAuditTable_withFirmSearchText_setsFirmSearchForm() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "Test Firm", null, "", model);

        // Then
        assertThat(viewName).isEqualTo("user-audit");
        assertThat(model.containsAttribute("firmSearch")).isTrue();
    }
}
