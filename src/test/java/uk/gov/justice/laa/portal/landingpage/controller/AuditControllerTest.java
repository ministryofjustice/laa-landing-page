package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import uk.gov.justice.laa.portal.landingpage.auth.AuthenticatedUser;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditTableSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.UserTypeForm;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.AuditExportService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private AuditController auditController;

    @Mock
    private UserService userService;

    @Mock
    private LoginService loginService;

    @Mock
    private EventService eventService;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private AuthenticatedUser authenticatedUser;

    @Mock
    private AuditExportService auditExportService;

    private PaginatedAuditUsers mockPaginatedUsers;
    private List<AppRoleDto> mockSilasRoles;
    private Permission mockPermission;
    private Model model;

    @BeforeEach
    void setUp() {
        auditController = new AuditController(userService, loginService, eventService, accessControlService,
                authenticatedUser, auditExportService);
        model = new ExtendedModelMap();

        // Setup mock audit users
        AuditUserDto user1 = AuditUserDto.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .userId(UUID.randomUUID().toString())
                .userType("External")
                .firmAssociation("Test Firm")
                .firmCode("123456")
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
        when(userService.getAuditUsers(anyString(), any(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

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

        verify(userService, times(1)).getAuditUsers("", null, null, null, null,  1, 10, "name", "asc");
        verify(userService, times(1)).getAllSilasRoles();
    }

    @Test
    void displayAuditTable_withSearchTerm_filtersResults() {
        // Given
        when(userService.getAuditUsers(eq("john"), any(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSearch("john");

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("search")).isEqualTo("john");

        verify(userService, times(1)).getAuditUsers("john", null, null, null, null,  1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withValidFirmId_filtersResults() {
        // Given
        UUID firmId = UUID.randomUUID();
        when(userService.getAuditUsers(anyString(), eq(firmId), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedFirmId(firmId.toString());

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", firmId, null, null, null,  1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withInvalidFirmId_ignoresFilter() {
        // Given
        when(userService.getAuditUsers(anyString(), isNull(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedFirmId("invalid-uuid");

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", null, null, null, null,  1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withSilasRole_filtersResults() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), eq("Global Admin"), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSilasRole("Global Admin");

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("selectedSilasRole")).isEqualTo("Global Admin");

        verify(userService, times(1)).getAuditUsers("", null, "Global Admin", null, null, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomPageSize_usesProvidedSize() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), any(), any(), any(), anyInt(), eq(25),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSize(25);

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(25);

        verify(userService, times(1)).getAuditUsers("", null, null, null, null, 1, 25, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomPage_usesProvidedPage() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), any(), any(), any(), eq(2), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setPage(2);

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("page")).isEqualTo(2);

        verify(userService, times(1)).getAuditUsers("", null, null, null, null, 2, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withCustomSort_usesProvidedSort() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), anyString(), any(), any(), anyInt(), anyInt(),
                eq("email"), eq("desc"))).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSilasRole("");
        criteria.setSort("email");
        criteria.setDirection("desc");

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("sort")).isEqualTo("email");
        assertThat(model.getAttribute("direction")).isEqualTo("desc");

        verify(userService, times(1)).getAuditUsers("", null, "", null, null, 1, 10, "email", "desc");
    }

    @Test
    void displayAuditTable_withAllFilters_combinesAllFilters() {
        // Given
        UUID firmId = UUID.randomUUID();
        when(userService.getAuditUsers(eq("test"), eq(firmId), eq("Global Admin"), any(), any(), eq(2), eq(25),
                eq("email"), eq("desc"))).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSearch("test");
        criteria.setSelectedFirmId(firmId.toString());
        criteria.setSilasRole("Global Admin");
        criteria.setPage(2);
        criteria.setSize(25);
        criteria.setSort("email");
        criteria.setDirection("desc");

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("search")).isEqualTo("test");
        assertThat(model.getAttribute("selectedSilasRole")).isEqualTo("Global Admin");
        assertThat(model.getAttribute("selectedAppId")).isEqualTo("");
        assertThat(model.getAttribute("page")).isEqualTo(2);
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(25);
        assertThat(model.getAttribute("sort")).isEqualTo("email");
        assertThat(model.getAttribute("direction")).isEqualTo("desc");

        verify(userService, times(1)).getAuditUsers("test", firmId, "Global Admin", null, null,  2, 25, "email",
                "desc");
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

        when(userService.getAuditUsers(anyString(), any(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(emptyResults);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

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
        when(userService.getAuditUsers(anyString(), any(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setFirmSearch("Test Firm");

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.containsAttribute("firmSearch")).isTrue();
    }

    @Test
    void displayAuditTable_withValidAppId_filtersResults() {
        // Given
        UUID appId = UUID.randomUUID();
        when(userService.getAuditUsers(anyString(), any(), any(), eq(appId), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedAppId(appId.toString());

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", null, null, appId, null, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withInvalidAppId_logsError() {
        // Given
        String selectedAppId = "notAValidUUID";
        when(userService.getAuditUsers(anyString(), any(), any(), eq(null), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        ListAppender<ILoggingEvent> listAppender = LogMonitoring
                .addListAppenderToLogger(AuditTableSearchCriteria.class);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedAppId(selectedAppId);

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertThat(logEvents.size()).isEqualTo(1);
        ILoggingEvent logEvent = logEvents.getFirst();
        assertThat(logEvent.getFormattedMessage()).isEqualTo("Invalid app ID format: " + selectedAppId);
        verify(userService, times(1)).getAuditUsers("", null, null, null, null, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withInvalidUserType_logsError() {
        // Given
        String userType = "invalidUserType";
        when(userService.getAuditUsers(anyString(), any(), any(), any(), eq(null), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        ListAppender<ILoggingEvent> listAppender = LogMonitoring
                .addListAppenderToLogger(AuditTableSearchCriteria.class);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedUserType(userType);

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.WARN);
        assertThat(logEvents.size()).isEqualTo(1);
        ILoggingEvent logEvent = logEvents.getFirst();
        assertThat(logEvent.getFormattedMessage()).isEqualTo("Invalid user type provided: " + userType);
        verify(userService, times(1)).getAuditUsers("", null, null, null, null,  1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withUserTypeNull_logsError() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), any(), any(), eq(null), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);

        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedUserType(null);

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", null, null, null, null,  1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withUserType_filtersResults() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), any(), any(), eq(UserTypeForm.INTERNAL), anyInt(),
                anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedUserType(UserTypeForm.INTERNAL.name());

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("selectedUserType")).isEqualTo("INTERNAL");

        verify(userService, times(1)).getAuditUsers("", null, null, null, UserTypeForm.INTERNAL, 1, 10, "name",
                "asc");
    }


    @Test
    void displayAuditTable_withMultiFirm_filtersResults() {
        // Given
        when(userService.getAuditUsers(anyString(), any(), any(), any(), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedUserType(UserTypeForm.MULTI_FIRM.name());

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");
        assertThat(model.getAttribute("selectedUserType")).isEqualTo("MULTI_FIRM");

        verify(userService, times(1)).getAuditUsers("", null, null, null, UserTypeForm.MULTI_FIRM, 1, 10, "name", "asc");
    }

    @Test
    void displayAuditTable_withWithSelectedAppIdEmpty_appIdNull() {
        // Given
        String selectedAppId = "";
        when(userService.getAuditUsers(anyString(), any(), any(), eq(null), any(), anyInt(), anyInt(),
                anyString(), anyString())).thenReturn(mockPaginatedUsers);
        when(userService.getAllSilasRoles()).thenReturn(mockSilasRoles);
        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSelectedAppId(selectedAppId);

        // When
        String viewName = auditController.displayAuditTable(criteria, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/users");

        verify(userService, times(1)).getAuditUsers("", null, null, null, null, 1, 10, "name", "asc");
    }

    @Test
    void displayUserAuditDetail_withValidUserId_returnsDetailView() {
        // Given
        UUID userId = UUID.randomUUID();
        AuditUserDetailDto mockUserDetail = AuditUserDetailDto
                .builder()
                .userId(userId.toString())
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .fullName("John Doe")
                .isMultiFirmUser(false)
                .profiles(Collections.emptyList())
                .build();

        when(userService.getAuditUserDetail(userId, 1, 5)).thenReturn(mockUserDetail);

        // When
        String viewName = auditController.displayUserAuditDetail(userId, 1, 5, false, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/details");
        assertThat(model.getAttribute("user")).isEqualTo(mockUserDetail);
        verify(userService, times(1)).getAuditUserDetail(userId, 1, 5);
    }

    @Test
    void displayUserAuditDetail_withMultiFirmUser_returnsDetailViewWithAllProfiles() {
        // Given
        UUID userId = UUID.randomUUID();
        AppRoleDto appRoleDto = AppRoleDto.builder()
                .id(UUID.randomUUID().toString())
                .name("Role 1")
                .userTypeRestriction(new UserType[] { UserType.EXTERNAL })
                .app(AppDto.builder()
                        .name("App 1")
                        .build())
                .build();

        // Create multiple profiles for multi-firm user
        AuditUserDetailDto.AuditProfileDto profile1 = AuditUserDetailDto.AuditProfileDto
                .builder()
                .profileId(UUID.randomUUID().toString())
                .firmName("Smith & Associates")
                .firmCode("SA123")
                .officeRestrictions("Access to All Offices")
                .activeProfile(true)
                .roles(List.of(appRoleDto))
                .build();

        AuditUserDetailDto.AuditProfileDto profile2 = AuditUserDetailDto.AuditProfileDto
                .builder()
                .profileId(UUID.randomUUID().toString())
                .firmName("Jones Law Firm")
                .firmCode("JL456")
                .officeRestrictions("2 office(s) selected")
                .activeProfile(false)
                .roles(List.of(appRoleDto))
                .build();

        AuditUserDetailDto mockUserDetail = AuditUserDetailDto
                .builder()
                .userId(userId.toString())
                .email("multi.user@example.com")
                .firstName("Multi")
                .lastName("User")
                .fullName("Multi User")
                .isMultiFirmUser(true)
                .profiles(List.of(profile1, profile2))
                .build();
        Map<String, List<String>> expectedList = new HashMap<>();
        expectedList.put("App 1", List.of("Role 1"));
        when(userService.getAuditUserDetail(userId, 1, 10)).thenReturn(mockUserDetail);

        // When
        String viewName = auditController.displayUserAuditDetail(userId, 1, 10, false, model);

        // Then
        AuditUserDetailDto resultUserDetails = (AuditUserDetailDto) model.getAttribute("user");
        assertThat(viewName).isEqualTo("user-audit/details");
        assertThat(resultUserDetails).isEqualTo(mockUserDetail);
        assertThat(resultUserDetails.getProfiles().get(0).getRolesGroupByAppName()).isEqualTo(expectedList);
        assertThat(resultUserDetails.getProfiles().get(1).getRolesGroupByAppName()).isEqualTo(expectedList);

        // Verify user has multiple profiles
        AuditUserDetailDto result = (AuditUserDetailDto) model
                .getAttribute("user");
        assertThat(result.isMultiFirmUser()).isTrue();
        assertThat(result.getProfiles()).hasSize(2);
        assertThat(result.getProfiles().get(0).isActiveProfile()).isTrue();
        assertThat(result.getProfiles().get(1).isActiveProfile()).isFalse();

        verify(userService, times(1)).getAuditUserDetail(userId, 1, 10);
    }

    @Test
    void displayUserAuditDetail_withPaginationParams_callsServiceWithParams() {
        // Given
        UUID userId = UUID.randomUUID();
        int profilePage = 2;
        int profileSize = 5;

        AuditUserDetailDto mockUserDetail = AuditUserDetailDto
                .builder()
                .userId(userId.toString())
                .email("multi.user@example.com")
                .firstName("Multi")
                .lastName("User")
                .fullName("Multi User")
                .isMultiFirmUser(true)
                .profiles(Collections.emptyList())
                .totalProfiles(25)
                .totalProfilePages(5)
                .currentProfilePage(2)
                .build();

        when(userService.getAuditUserDetail(userId, profilePage, profileSize)).thenReturn(mockUserDetail);

        // When
        String viewName = auditController.displayUserAuditDetail(userId, profilePage, profileSize, false, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/details");
        assertThat(model.getAttribute("user")).isEqualTo(mockUserDetail);
        verify(userService, times(1)).getAuditUserDetail(userId, profilePage, profileSize);
    }

    @Test
    void deleteUserWithoutProfileConfirm_shouldReturnConfirmationView() {
        // Given
        String entraUserId = UUID.randomUUID().toString();
        AuditUserDetailDto userDetail = AuditUserDetailDto.builder()
                .userId(null)
                .firstName("John")
                .lastName("Doe")
                .fullName("John Doe")
                .email("john.doe@example.com")
                .profiles(Collections.emptyList())
                .hasNoProfile(true)
                .build();

        when(userService.getAuditUserDetailByEntraId(UUID.fromString(entraUserId)))
                .thenReturn(userDetail);

        // When
        String viewName = auditController.deleteUserWithoutProfileConfirm(entraUserId, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/delete-user-without-profile-reason");
        assertThat(model.getAttribute("user")).isEqualTo(userDetail);
        assertThat(model.getAttribute("pageTitle")).isEqualTo("Remove access - John Doe");
        verify(userService).getAuditUserDetailByEntraId(UUID.fromString(entraUserId));
    }

    @Test
    void deleteUserWithoutProfile_withValidReason_shouldDeleteAndReturnSuccess() {
        // Given
        String entraUserId = UUID.randomUUID().toString();
        String reason = "User no longer needs access";
        UUID currentUserId = UUID.randomUUID();

        AuditUserDetailDto userDetail = AuditUserDetailDto.builder()
                .userId(null)
                .firstName("Jane")
                .lastName("Smith")
                .fullName("Jane Smith")
                .email("jane.smith@example.com")
                .profiles(Collections.emptyList())
                .hasNoProfile(true)
                .build();

        uk.gov.justice.laa.portal.landingpage.entity.EntraUser currentUser = uk.gov.justice.laa.portal.landingpage.entity.EntraUser
                .builder()
                .id(currentUserId)
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .build();

        uk.gov.justice.laa.portal.landingpage.model.DeletedUser deletedUser = uk.gov.justice.laa.portal.landingpage.model.DeletedUser
                .builder()
                .deletedUserId(UUID.fromString(entraUserId))
                .removedRolesCount(0)
                .detachedOfficesCount(0)
                .build();

        when(userService.getAuditUserDetailByEntraId(UUID.fromString(entraUserId)))
                .thenReturn(userDetail);
        when(loginService.getCurrentEntraUser(any())).thenReturn(currentUser);
        when(userService.deleteEntraUserWithoutProfile(entraUserId, reason, currentUserId))
                .thenReturn(deletedUser);

        // When
        String viewName = auditController.deleteUserWithoutProfile(
                entraUserId, reason, null, null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/delete-user-success");
        assertThat(model.getAttribute("deletedUserFullName")).isEqualTo("Jane Smith");
        assertThat(model.getAttribute("pageTitle")).isEqualTo("User deleted");
        verify(userService).deleteEntraUserWithoutProfile(entraUserId, reason, currentUserId);
        verify(eventService).logEvent(any(uk.gov.justice.laa.portal.landingpage.dto.DeleteUserSuccessAuditEvent.class));
    }

    @Test
    void deleteUserWithoutProfile_withShortReason_shouldReturnValidationError() {
        // Given
        String entraUserId = UUID.randomUUID().toString();
        String shortReason = "short";

        AuditUserDetailDto userDetail = AuditUserDetailDto.builder()
                .userId(null)
                .firstName("Bob")
                .lastName("Jones")
                .fullName("Bob Jones")
                .email("bob.jones@example.com")
                .profiles(Collections.emptyList())
                .hasNoProfile(true)
                .build();

        when(userService.getAuditUserDetailByEntraId(UUID.fromString(entraUserId)))
                .thenReturn(userDetail);

        // When
        String viewName = auditController.deleteUserWithoutProfile(
                entraUserId, shortReason, null, null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/delete-user-without-profile-reason");
        assertThat(model.getAttribute("user")).isEqualTo(userDetail);
        assertThat(model.getAttribute("fieldErrorMessage"))
                .isEqualTo("Please enter a reason (minimum 10 characters).");
        assertThat(model.getAttribute("pageTitle")).isEqualTo("Remove access - Bob Jones");
        verify(userService, times(0)).deleteEntraUserWithoutProfile(anyString(), anyString(), any());
    }

    @Test
    void deleteUserWithoutProfile_withNullReason_shouldReturnValidationError() {
        // Given
        String entraUserId = UUID.randomUUID().toString();

        AuditUserDetailDto userDetail = AuditUserDetailDto.builder()
                .userId(null)
                .firstName("Alice")
                .lastName("Brown")
                .fullName("Alice Brown")
                .email("alice.brown@example.com")
                .profiles(Collections.emptyList())
                .hasNoProfile(true)
                .build();

        when(userService.getAuditUserDetailByEntraId(UUID.fromString(entraUserId)))
                .thenReturn(userDetail);

        // When
        String viewName = auditController.deleteUserWithoutProfile(
                entraUserId, null, null, null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/delete-user-without-profile-reason");
        assertThat(model.getAttribute("fieldErrorMessage"))
                .isEqualTo("Please enter a reason (minimum 10 characters).");
        verify(userService, times(0)).deleteEntraUserWithoutProfile(anyString(), anyString(), any());
    }

    @Test
    void deleteUserWithoutProfile_whenDeleteFails_shouldReturnErrorView() {
        // Given
        String entraUserId = UUID.randomUUID().toString();
        String reason = "Valid deletion reason";
        UUID currentUserId = UUID.randomUUID();

        AuditUserDetailDto userDetail = AuditUserDetailDto.builder()
                .userId(null)
                .firstName("Charlie")
                .lastName("Wilson")
                .fullName("Charlie Wilson")
                .email("charlie.wilson@example.com")
                .profiles(Collections.emptyList())
                .hasNoProfile(true)
                .build();

        uk.gov.justice.laa.portal.landingpage.entity.EntraUser currentUser = uk.gov.justice.laa.portal.landingpage.entity.EntraUser
                .builder()
                .id(currentUserId)
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .build();

        when(userService.getAuditUserDetailByEntraId(UUID.fromString(entraUserId)))
                .thenReturn(userDetail);
        when(loginService.getCurrentEntraUser(any())).thenReturn(currentUser);
        when(userService.deleteEntraUserWithoutProfile(entraUserId, reason, currentUserId))
                .thenThrow(new RuntimeException("Failed to delete user from Entra"));

        // When
        String viewName = auditController.deleteUserWithoutProfile(
                entraUserId, reason, null, null, model);

        // Then
        assertThat(viewName).isEqualTo("user-audit/delete-user-without-profile-reason");
        assertThat(model.getAttribute("user")).isEqualTo(userDetail);
        assertThat(model.getAttribute("globalErrorMessage"))
                .isEqualTo("User delete failed, please try again later");
        assertThat(model.getAttribute("pageTitle")).isEqualTo("Remove access - Charlie Wilson");
        verify(eventService).logEvent(any(uk.gov.justice.laa.portal.landingpage.dto.DeleteUserAttemptAuditEvent.class));
    }

    @Test
    void downloadAuditCsv_shouldReturnSuccess() {

        AuditTableSearchCriteria criteria = new AuditTableSearchCriteria();
        criteria.setSearch("TestSearch");
        criteria.setSort("name");
        criteria.setDirection("asc");

        AuditUserDto page1User =
                AuditUserDto.builder().name("P1").email("p1@example.com").build();
        List<AuditUserDto> page1Users = Collections.nCopies(500, page1User);

        AuditUserDto page2User =
                AuditUserDto.builder().name("P2").email("p2@example.com").build();
        List<AuditUserDto> page2Users = List.of(page2User);

        PaginatedAuditUsers page1 = PaginatedAuditUsers.builder()
                .users(page1Users)
                .currentPage(1)
                .pageSize(500)
                .build();

        PaginatedAuditUsers page2 = PaginatedAuditUsers.builder()
                .users(page2Users)
                .currentPage(2)
                .pageSize(500)
                .build();

        when(userService.getAuditUsers(
                eq("TestSearch"), any(), any(), any(), any(), eq(1), eq(500), eq("name"), eq("asc"))).thenReturn(page1);

        when(userService.getAuditUsers(
                eq("TestSearch"), any(), any(), any(), any(), eq(2), eq(500), eq("name"), eq("asc"))).thenReturn(page2);

        byte[] csvBytes = "Name,Email\nP1,p1@example.com\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        AuditExportService.AuditCsvExport export = new AuditExportService.AuditCsvExport("audit.csv", csvBytes);
        when(auditExportService.downloadAuditCsv(any())).thenReturn(export);

        ResponseEntity<byte[]> response = auditController.downloadAuditCsv(criteria);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(csvBytes);

        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.parseMediaType("text/csv"));
        assertThat(headers.getContentDisposition().getType()).isEqualTo("attachment");
        assertThat(headers.getContentDisposition().getFilename()).isEqualTo("audit.csv");

        verify(userService, times(1)).getAuditUsers("TestSearch", null, null, null, null, 1, 500, "name", "asc");
        verify(userService, times(1)).getAuditUsers("TestSearch", null, null, null, null, 2, 500, "name", "asc");
        verify(auditExportService, times(1)).downloadAuditCsv(any());
    }
}
