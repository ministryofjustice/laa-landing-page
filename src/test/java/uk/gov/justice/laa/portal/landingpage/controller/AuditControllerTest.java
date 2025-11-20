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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

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
        when(userService.getAuditUsers(anyString(), any(), anyString(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "", null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
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

        verify(userService, times(1)).getAuditUsers("", null, "", null, 1, 10, "name", "asc");
        verify(userService, times(1)).getAllSilasRoles();
    }

    @Test
    void displayAuditTable_withSearchTerm_filtersResults() {
        // Given
        when(userService.getAuditUsers(eq("john"), any(), anyString(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "john", "", null, "", null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("search")).isEqualTo("john");

        verify(userService, times(1)).getAuditUsers("john", null, "", null, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withValidFirmId_filtersResults() {
        // Given
        UUID firmId = UUID.randomUUID();
        when(userService.getAuditUsers(anyString(), eq(firmId), anyString(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", firmId.toString(), "", null,
                model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", firmId, "", null, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withInvalidFirmId_ignoresFilter() {
        // Given
        when(userService.getAuditUsers(anyString(), isNull(), anyString(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", "invalid-uuid", "", null,
                model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", null, "", null, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withSilasRole_filtersResults() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), eq("Global Admin"), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "Global Admin", null,
                model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("selectedSilasRole")).isEqualTo("Global Admin");

        verify(userService, times(1)).getAuditUsers("", null, "Global Admin", null, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomPageSize_usesProvidedSize() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), anyString(), any(), anyInt(), eq(25),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(25, 1, "name", "asc", "", "", null, "", null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(25);

        verify(userService, times(1)).getAuditUsers("", null, "", null, 1, 25, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomPage_usesProvidedPage() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), anyString(), any(), eq(2), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 2, "name", "asc", "", "", null, "", null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("page")).isEqualTo(2);

        verify(userService, times(1)).getAuditUsers("", null, "", null, 2, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomSort_usesProvidedSort() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), anyString(), any(), anyInt(), anyInt(),
                eq("email"), eq("desc"))).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "email", "desc", "", "", null, "", null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("sort")).isEqualTo("email");
        assertThat(model.getAttribute("direction")).isEqualTo("desc");

        verify(userService, times(1)).getAuditUsers("", null, "", null, 1, 10, "email", "desc");
    }

    @Test
    void displayAuditTable_withAllFilters_combinesAllFilters() {
        // Given
        UUID firmId = UUID.randomUUID();
        when(userService.getAuditUsers(eq("test"), eq(firmId), eq("Global Admin"), any(), eq(2), eq(25),
                eq("email"), eq("desc"))).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(25, 2, "email", "desc", "test", "", firmId.toString(),
                "Global Admin", null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("search")).isEqualTo("test");
        assertThat(model.getAttribute("selectedSilasRole")).isEqualTo("Global Admin");
        assertThat(model.getAttribute("selectedAppId")).isEqualTo("");
        assertThat(model.getAttribute("page")).isEqualTo(2);
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(25);
        assertThat(model.getAttribute("sort")).isEqualTo("email");
        assertThat(model.getAttribute("direction")).isEqualTo("desc");

        verify(userService, times(1)).getAuditUsers("test", firmId, "Global Admin", null, 2, 25, "email", "desc");
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

        when(userService.getAuditUsers(anyString(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(emptyResults);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "", null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("users")).isEqualTo(Collections.emptyList());
        assertThat(model.getAttribute("totalUsers")).isEqualTo(0L);
        assertThat(model.getAttribute("totalPages")).isEqualTo(0);
        assertThat(model.getAttribute("actualPageSize")).isEqualTo(0);
    }

    @Test
    void displayAuditTable_withFirmSearchText_setsFirmSearchForm() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "Test Firm", null, "", null,
                model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.containsAttribute("firmSearch")).isTrue();
    }

    @Test
    void displayAuditTable_withValidAppId_filtersResults() {
        // Given
        UUID appId = UUID.randomUUID();
        when(userService.getAuditUsers(anyString(), any(), anyString(), eq(appId), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "", appId.toString(),
                model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", null, "", appId, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withInvalidAppId_logsError() {
        // Given
        String selectedAppId = "notAValidUUID";
        when(userService.getAuditUsers(anyString(), any(), anyString(), eq(null), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(AuditController.class);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "", selectedAppId,
                model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", null, "", null, 1, 10, "name", "asc");
        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertThat(logEvents.size()).isEqualTo(1);
        ILoggingEvent logEvent = logEvents.getFirst();
        assertThat(logEvent.getFormattedMessage()).isEqualTo("Invalid app ID format: " + selectedAppId);
    }

    @Test
    void displayAuditTable_withWithSelectedAppIdEmpty_appIdNull() {
        // Given
        String selectedAppId = "";
        when(userService.getAuditUsers(anyString(), any(), anyString(), eq(null), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        // When
        String viewName = auditController.displayAuditTable(10, 1, "name", "asc", "", "", null, "", selectedAppId,
                model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", null, "", null, 1, 10, "name", "asc");
    }

    @Test
    void displayUserAuditDetail_withValidUserId_returnsDetailView() {
        // Given
        UUID userId = UUID.randomUUID();
        uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto mockUserDetail = uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto
                .builder()
                .userId(userId.toString())
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .fullName("John Doe")
                .isMultiFirmUser(false)
                .profiles(Collections.emptyList())
                .build();

        when(userService.getAuditUserDetail(userId)).thenReturn(mockUserDetail);

        // When
        String viewName = auditController.displayUserAuditDetail(userId, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/details");
        assertThat(model.getAttribute("user")).isEqualTo(mockUserDetail);
        verify(userService, times(1)).getAuditUserDetail(userId);
    }

    @Test
    void displayUserAuditDetail_withMultiFirmUser_returnsDetailViewWithAllProfiles() {
        // Given
        UUID userId = UUID.randomUUID();

        // Create multiple profiles for multi-firm user
        uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto.AuditProfileDto profile1 = uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto.AuditProfileDto
                .builder()
                .profileId(UUID.randomUUID().toString())
                .firmName("Smith & Associates")
                .firmCode("SA123")
                .officeRestrictions("Access to All Offices")
                .activeProfile(true)
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto.AuditProfileDto profile2 = uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto.AuditProfileDto
                .builder()
                .profileId(UUID.randomUUID().toString())
                .firmName("Jones Law Firm")
                .firmCode("JL456")
                .officeRestrictions("2 office(s) selected")
                .activeProfile(false)
                .build();

        uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto mockUserDetail = uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto
                .builder()
                .userId(userId.toString())
                .email("multi.user@example.com")
                .firstName("Multi")
                .lastName("User")
                .fullName("Multi User")
                .isMultiFirmUser(true)
                .profiles(List.of(profile1, profile2))
                .build();

        when(userService.getAuditUserDetail(userId)).thenReturn(mockUserDetail);

        // When
        String viewName = auditController.displayUserAuditDetail(userId, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/details");
        assertThat(model.getAttribute("user")).isEqualTo(mockUserDetail);

        // Verify user has multiple profiles
        uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto result = (uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto) model
                .getAttribute("user");
        assertThat(result.isMultiFirmUser()).isTrue();
        assertThat(result.getProfiles()).hasSize(2);
        assertThat(result.getProfiles().get(0).isActiveProfile()).isTrue();
        assertThat(result.getProfiles().get(1).isActiveProfile()).isFalse();

        verify(userService, times(1)).getAuditUserDetail(userId);
    }
}
