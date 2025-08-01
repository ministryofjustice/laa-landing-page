package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.mapping.Any;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import com.microsoft.graph.models.User;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.CreateUserDetailsIncompleteException;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
import uk.gov.justice.laa.portal.landingpage.forms.EditUserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.forms.OfficesForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppViewModel;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private UserController userController;

    @Mock
    private LoginService loginService;
    @Mock
    private UserService userService;
    @Mock
    private OfficeService officeService;
    @Mock
    private EventService eventService;
    @Mock
    private FirmService firmService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private HttpSession session;
    @Mock
    private Authentication authentication;
    @Mock
    private RedirectAttributes redirectAttributes;


    private Model model;

    @BeforeEach
    void setUp() {
        userController = new UserController(loginService, userService, officeService, eventService, firmService,
                new MapperConfig().modelMapper(), accessControlService);
        model = new ExtendedModelMap();
    }

    @Test
    void displayAllUsers() {
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(new ArrayList<>());
        paginatedUsers.setNextPageLink("nextPageLink");
        paginatedUsers.setPreviousPageLink("previousPageLink");
        paginatedUsers.setTotalUsers(100);
        paginatedUsers.setTotalPages(10);
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        FirmDto firmDto = new FirmDto();
        firmDto.setId(UUID.randomUUID());
        when(firmService.getUserFirm(any())).thenReturn(Optional.of(firmDto));
        when(userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(any(), anyList(), any(), anyInt(), anyInt(),
                any(), any())).thenReturn(paginatedUsers);

        String view = userController.displayAllUsers(10, 1, null, null, null, null, false, model, session,
                authentication);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(paginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        assertThat(model.getAttribute("page")).isEqualTo(1);
        assertThat(model.getAttribute("totalUsers")).isEqualTo(100L);
        assertThat(model.getAttribute("totalPages")).isEqualTo(10);
    }

    @Test
    void editUser() {
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setFullName("Test User");

        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .build();

        List<AppRoleDto> roles = List.of(new AppRoleDto());

        when(userService.getUserProfileById(anyString())).thenReturn(Optional.of(userProfile));
        when(userService.getUserAppRolesByUserId(anyString())).thenReturn(roles);

        String view = userController.editUser("userId", model);

        assertThat(view).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(userProfile);
    }

    @Test
    void givenUsersExist_whenDisplayAllUsers_thenPopulatesModelAndReturnsUsersView() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new UserProfileDto(), new UserProfileDto()));
        mockPaginatedUsers.setNextPageLink("nextLink123");
        mockPaginatedUsers.setPreviousPageLink("prevLink456");
        FirmDto firmDto = new FirmDto();
        firmDto.setId(UUID.randomUUID());
        when(firmService.getUserFirm(any())).thenReturn(Optional.of(firmDto));
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        when(userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(any(), any(), any(), eq(1), eq(10), any(),
                any())).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, null, null, null, null, false, model, session,
                authentication);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(mockPaginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        verify(userService).getPageOfUsersByNameOrEmailAndPermissionsAndFirm(isNull(), anyList(), eq(firmDto.getId()), eq(1), eq(10), isNull(),
                isNull());
    }

    @Test
    void givenNoUsers_whenDisplayAllUsers_thenReturnsEmptyListInModel() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        mockPaginatedUsers.setNextPageLink(null);
        mockPaginatedUsers.setPreviousPageLink(null);
        FirmDto firmDto = new FirmDto();
        firmDto.setId(UUID.randomUUID());
        when(firmService.getUserFirm(any())).thenReturn(Optional.of(firmDto));
        when(userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(any(), any(), any(), anyInt(), anyInt(),
                any(), any())).thenReturn(mockPaginatedUsers);
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        // Act
        String viewName = userController.displayAllUsers(10, 1, null, null, null, null, false, model, session,
                authentication);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsersByNameOrEmailAndPermissionsAndFirm(isNull(), anyList(), eq(firmDto.getId()), eq(1), eq(10), isNull(), isNull());
    }

    @Test
    void testDisplayAllUsersSearchesUsersWithSortWhenSearchTermIsGiven() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        when(userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(eq("Test"), any(), any(), anyInt(), anyInt(),
                anyString(), any())).thenReturn(mockPaginatedUsers);
        FirmDto firmDto = new FirmDto();
        firmDto.setId(UUID.randomUUID());
        when(firmService.getUserFirm(any())).thenReturn(Optional.of(firmDto));
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        // Act
        String viewName = userController.displayAllUsers(10, 1, "firstName", null, null, "Test", false, model, session,
                authentication);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsersByNameOrEmailAndPermissionsAndFirm(eq("Test"), any(), eq(firmDto.getId()), eq(1), eq(10),
                eq("firstName"), isNull());
    }

    @Test
    void testDisplayAllUsersDoesNotSearchUsersWhenSearchTermIsEmpty() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().build());
        when(userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(anyString(), any(), any(), anyInt(), anyInt(),
                any(), any())).thenReturn(mockPaginatedUsers);
        FirmDto firmDto = new FirmDto();
        firmDto.setId(UUID.randomUUID());
        when(firmService.getUserFirm(any())).thenReturn(Optional.of(firmDto));

        // Act
        String viewName = userController.displayAllUsers(10, 1, "firstname", "desc", null, "", false, model, session,
                authentication);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsersByNameOrEmailAndPermissionsAndFirm(eq(""), any(), eq(firmDto.getId()), eq(1), eq(10), eq("firstname"), eq("desc"));
    }

    @Test
    void givenValidUserId_whenEditUser_thenFetchesUserAndReturnsEditView() {
        // Arrange
        String userId = "123e4567-e89b-12d3-a456-426614174000";
        UserProfileDto mockUser = new UserProfileDto();
        mockUser.setId(UUID.fromString(userId));
        // Create EntraUserDto with fullName
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setFullName("Test User");
        mockUser.setEntraUser(entraUser);

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(mockUser));
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(List.of());

        // Act
        String viewName = userController.editUser(userId, model);

        // Assert
        assertThat(viewName).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        verify(userService).getUserProfileById(userId);
    }

    @Test
    void givenInvalidUserId_whenEditUser_thenReturnsEditViewWithErrorOrRedirect() {

        // Arrange
        String userId = "invalid-user";
        when(userService.getUserProfileById(userId)).thenReturn(Optional.empty());

        // Act
        String viewName = userController.editUser(userId, model);

        verify(userService).getUserProfileById(userId);
    }

    @Test
    void disableUsers() throws IOException {
        List<String> ids = List.of("1", "2", "3");
        String view = userController.disableUsers(ids);
        assertThat(view).isEqualTo("redirect:/users");
    }

    @Test
    void manageUser_shouldAddUserAndLastLoggedInToModelAndReturnManageUserView() {
        // Arrange
        String userId = "user42";
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId(userId);
        entraUser.setFullName("Managed User");

        UserProfileDto mockUser = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .appRoles(List.of(new AppRoleDto()))
                .offices(List.of(OfficeDto.builder()
                        .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
                        .code("Test Office")
                        .address(OfficeDto.AddressDto.builder().addressLine1("Test Address").build())
                        .build()))
                .build();

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(mockUser));

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        verify(userService).getUserProfileById(userId);
    }

    @Test
    void manageUser_whenUserNotFound_shouldAddNullUserAndReturnManageUserView() {
        // Arrange
        String userId = "notfound";
        when(userService.getUserProfileById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        Assertions.assertThrows(NoSuchElementException.class,
                () -> userController.manageUser(userId, model));

        verify(userService).getUserProfileById(userId);
    }

    @Test
    void createNewUser() {
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("selectedUserType")).thenReturn(null);
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        String view = userController.createUser(userDetailsForm, session, model);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(view).isEqualTo("add-user-details");
    }

    @Test
    void createUserFromSession() {
        EntraUserDto mockUser = new EntraUserDto();
        mockUser.setFullName("Test User");
        when(session.getAttribute("user")).thenReturn(mockUser);
        when(session.getAttribute("selectedUserType")).thenReturn(UserType.EXTERNAL_SINGLE_FIRM);
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        String view = userController.createUser(userDetailsForm, session, model);
        assertThat(model.getAttribute("user")).isNotNull();
        EntraUserDto sessionUser = (EntraUserDto) session.getAttribute("user");
        assertThat(sessionUser.getFullName()).isEqualTo("Test User");
        assertThat(view).isEqualTo("add-user-details");
    }

    @Test
    void postNewUser() {
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("firstName");
        userDetailsForm.setLastName("lastName");
        userDetailsForm.setEmail("email");
        userDetailsForm.setUserType(UserType.EXTERNAL_SINGLE_FIRM);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        HttpSession session = new MockHttpSession();
        String redirectUrl = userController.postUser(userDetailsForm, bindingResult, session, model);
        EntraUserDto sessionUser = (EntraUserDto) session.getAttribute("user");
        assertThat(sessionUser.getFirstName()).isEqualTo("firstName");
        assertThat(sessionUser.getLastName()).isEqualTo("lastName");
        assertThat(sessionUser.getFullName()).isEqualTo("firstName lastName");
        assertThat(sessionUser.getEmail()).isEqualTo("email");
        UserType selectedUserType = (UserType) session.getAttribute("selectedUserType");
        assertThat(selectedUserType).isEqualTo(UserType.EXTERNAL_SINGLE_FIRM);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/firm");
    }

    @Test
    void postSessionUser() {
        EntraUserDto mockUser = new EntraUserDto();
        mockUser.setFullName("Test User");
        HttpSession session = new MockHttpSession();
        session.setAttribute("user", mockUser);
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("firstName");
        userDetailsForm.setLastName("lastName");
        userDetailsForm.setEmail("email");
        userDetailsForm.setUserType(UserType.EXTERNAL_SINGLE_FIRM);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        String redirectUrl = userController.postUser(userDetailsForm, bindingResult, session, model);
        EntraUserDto sessionUser = (EntraUserDto) session.getAttribute("user");
        assertThat(sessionUser.getFirstName()).isEqualTo("firstName");
        assertThat(sessionUser.getLastName()).isEqualTo("lastName");
        assertThat(sessionUser.getFullName()).isEqualTo("firstName lastName");
        assertThat(sessionUser.getEmail()).isEqualTo("email");
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/firm");
        UserType selectedUserType = (UserType) session.getAttribute("selectedUserType");
        assertThat(selectedUserType).isEqualTo(UserType.EXTERNAL_SINGLE_FIRM);
    }

    @Test
    void selectUserAppsGet() {
        AppDto app = new AppDto();
        app.setId("1");
        when(userService.getAppsByUserType(any())).thenReturn(List.of(app));
        List<String> ids = List.of("1");
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", ids);
        session.setAttribute("user", new EntraUserDto());
        ApplicationsForm applicationsForm = new ApplicationsForm();
        String view = userController.selectUserApps(applicationsForm, model, session);
        assertThat(view).isEqualTo("add-user-apps");
        assertThat(model.getAttribute("apps")).isNotNull();
        List<AppViewModel> modeApps = (List<AppViewModel>) model.getAttribute("apps");
        assertThat(modeApps.getFirst().getId()).isEqualTo("1");
        assertThat(modeApps.getFirst().isSelected()).isTrue();
    }

    @Test
    void selectUserAppsGetThrowsExceptionWhenNoUserPresent() {
        AppDto app = new AppDto();
        app.setId("1");
        when(userService.getAppsByUserType(any())).thenReturn(List.of(app));
        HttpSession session = new MockHttpSession();
        ApplicationsForm applicationsForm = new ApplicationsForm();
        assertThrows(CreateUserDetailsIncompleteException.class,
                () -> userController.selectUserApps(applicationsForm, model, session));
    }

    @Test
    void setSelectedAppsPost() {
        HttpSession session = new MockHttpSession();
        List<String> ids = List.of("1");
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(ids);
        Model model = new ExtendedModelMap();
        String redirectUrl = userController.setSelectedApps(applicationsForm, model, session);
        assertThat(session.getAttribute("apps")).isNotNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/roles");
    }

    @Test
    void getSelectedRolesGet() {
        List<String> selectedApps = new ArrayList<>();
        selectedApps.add("app1");
        List<String> selectedRoles = new ArrayList<>();
        selectedRoles.add("dev");
        List<AppRoleDto> roles = new ArrayList<>();
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("tester");
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("dev");
        roles.add(userRole);
        roles.add(userRole2);
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", selectedApps);
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new EntraUserDto());
        when(userService.getAppByAppId(any())).thenReturn(Optional.of(new AppDto()));
        when(userService.getAppRolesByAppIdAndUserType(any(), any())).thenReturn(roles);
        String view = userController.getSelectedRoles(new RolesForm(), model, session);
        assertThat(view).isEqualTo("add-user-roles");
        assertThat(model.getAttribute("roles")).isNotNull();
        List<AppRoleViewModel> sessionRoles = (List<AppRoleViewModel>) model.getAttribute("roles");
        assertThat(sessionRoles.getFirst().isSelected()).isFalse();
        assertThat(sessionRoles.get(1).isSelected()).isTrue();
    }

    @Test
    void getSelectedRolesGetThrowsExceptionWhenNoAppsPresent() {
        assertThrows(CreateUserDetailsIncompleteException.class,
                () -> userController.getSelectedRoles(new RolesForm(), model, session));
    }

    @Test
    void getSelectedRolesGetThrowsExceptionWhenNoUserPresent() {
        List<String> selectedApps = new ArrayList<>();
        selectedApps.add("app1");
        List<String> selectedRoles = new ArrayList<>();
        selectedRoles.add("dev");
        List<AppRoleDto> roles = new ArrayList<>();
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("tester");
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("dev");
        roles.add(userRole);
        roles.add(userRole2);
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", selectedApps);
        session.setAttribute("roles", selectedRoles);
        when(userService.getAppByAppId(any())).thenReturn(Optional.of(new AppDto()));
        when(userService.getAppRolesByAppIdAndUserType(any(), any())).thenReturn(roles);
        assertThrows(CreateUserDetailsIncompleteException.class,
                () -> userController.getSelectedRoles(new RolesForm(), model, session));
    }

    @Test
    void setSelectedRolesPost() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(roles);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        Model model = new ExtendedModelMap();
        model.addAttribute("createUserRolesSelectedAppIndex", 0);
        session.setAttribute("userCreateRolesModel", model);
        String redirectUrl = userController.setSelectedRoles(rolesForm, bindingResult, model, session);
        assertThat(session.getAttribute("roles")).isNotNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/offices");
    }

    @Test
    void testSetSelectedRolesPostRedirectsWhenSessionModelIsNull() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(roles);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        Model model = new ExtendedModelMap();
        String redirectUrl = userController.setSelectedRoles(rolesForm, bindingResult, model, session);
        assertThat(session.getAttribute("roles")).isNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/roles");
    }

    @Test
    void testSetSelectedRolesPostRedirectsToNextPage() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(roles);
        Model model = new ExtendedModelMap();
        model.addAttribute("createUserRolesSelectedAppIndex", 0);
        session.setAttribute("userCreateRolesModel", model);
        List<String> selectedApps = List.of("app1", "app2");
        session.setAttribute("apps", selectedApps);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        String redirectUrl = userController.setSelectedRoles(rolesForm, bindingResult, model, session);
        assertThat(model.getAttribute("createUserRolesSelectedAppIndex")).isNotNull();
        assertThat(model.getAttribute("createUserRolesSelectedAppIndex")).isEqualTo(1);
        assertThat(session.getAttribute("createUserAllSelectedRoles")).isNotNull();
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("createUserAllSelectedRoles");
        assertThat(allSelectedRolesByPage.keySet().size()).isEqualTo(1);
        assertThat(allSelectedRolesByPage.get(0)).isEqualTo(List.of("1"));
        assertThat(session.getAttribute("userCreateRolesModel")).isNotNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/roles");
    }

    @Test
    void offices() {
        HttpSession session = new MockHttpSession();
        UUID firmId = UUID.randomUUID();
        UUID officeId = UUID.randomUUID();

        FirmDto selectedFirm = new FirmDto();
        selectedFirm.setId(firmId);

        OfficeData officeData = new OfficeData();
        officeData.setSelectedOffices(List.of(officeId.toString()));
        session.setAttribute("officeData", officeData);
        session.setAttribute("user", new EntraUserDto());
        session.setAttribute("firm", selectedFirm);

        Office.Address address1 = Office.Address.builder().addressLine1("addressLine1").city("city1").postcode("post_code1").build();
        Office.Address address2 = Office.Address.builder().addressLine1("addressLine2").city("city2").postcode("post_code2").build();
        Office office1 = Office.builder().id(officeId).address(address1).build();
        Office office2 = Office.builder().id(UUID.randomUUID()).address(address2).build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeService.getOfficesByFirms(List.of(firmId))).thenReturn(dbOffices);

        String view = userController.offices(new OfficesForm(), session, model);
        List<OfficeModel> modelOfficeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(modelOfficeData).isNotNull();
        assertThat(modelOfficeData.get(0).isSelected()).isTrue();
        assertThat(modelOfficeData.get(1).isSelected()).isFalse();
        assertThat(view).isEqualTo("add-user-offices");
    }

    @Test
    void postOffices() {
        MockHttpSession mockSession = new MockHttpSession();
        UUID firmId = UUID.randomUUID();
        UUID officeId1 = UUID.randomUUID();
        UUID officeId2 = UUID.randomUUID();

        FirmDto selectedFirm = new FirmDto();
        selectedFirm.setId(firmId);
        mockSession.setAttribute("firm", selectedFirm);

        List<String> selectedOffices = List.of(officeId1.toString());
        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        Office office1 = Office.builder().id(officeId1).code("of1").address(address).build();
        Office office2 = Office.builder().id(officeId2).code("of2").address(address).build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeService.getOfficesByFirms(List.of(firmId))).thenReturn(dbOffices);

        // Prepare OfficesForm and BindingResult
        OfficesForm officesForm = new OfficesForm();
        officesForm.setOffices(selectedOffices);
        BindingResult bindingResult = Mockito
                .mock(BindingResult.class);

        String redirectUrl = userController.postOffices(officesForm, bindingResult, model, mockSession);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/check-answers");
        OfficeData modelOfficeData = (OfficeData) mockSession.getAttribute("officeData");
        assertThat(modelOfficeData.getSelectedOffices()).hasSize(1);
        assertThat(modelOfficeData.getSelectedOfficesDisplay()).hasSize(1);
        assertThat(modelOfficeData.getSelectedOffices().get(0)).isEqualTo(officeId1.toString());
        assertThat(modelOfficeData.getSelectedOfficesDisplay().get(0)).isEqualTo("of1");
    }

    @Test
    void addUserCheckAnswersGet() {
        HttpSession session = new MockHttpSession();
        session.setAttribute("user", new EntraUserDto());
        FirmDto selectedFirm = new FirmDto();
        UUID firmId = UUID.randomUUID();
        selectedFirm.setId(firmId);
        selectedFirm.setName("Test Firm");
        session.setAttribute("firm", selectedFirm);
        UserType selectedUserType = UserType.EXTERNAL_SINGLE_FIRM;
        session.setAttribute("selectedUserType", selectedUserType);
        String view = userController.getUserCheckAnswers(model, session);
        assertThat(view).isEqualTo("add-user-check-answers");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("firm")).isEqualTo(selectedFirm);
        assertThat(model.getAttribute("userType")).isEqualTo(selectedUserType);
    }

    @Test
    void addUserCheckAnswersGetThrowsExceptionWhenNoUserPresent() {
        HttpSession session = new MockHttpSession();
        assertThrows(CreateUserDetailsIncompleteException.class,
                () -> userController.getUserCheckAnswers(model, session));
    }

    @Test
    void addUserCheckAnswersGetFirmAdmin() {
        HttpSession session = new MockHttpSession();
        session.setAttribute("user", new EntraUserDto());
        session.setAttribute("selectedUserType", UserType.EXTERNAL_SINGLE_FIRM);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        EntraUser user = EntraUser.builder().userProfiles(Set.of(UserProfile.builder().build())).build();
        when(userService.createUser(any(), any(), any(), any())).thenReturn(user);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
        assertThat(session.getAttribute("user")).isNotNull();
        assertThat(session.getAttribute("userProfile")).isNotNull();
    }

    @Test
    void addUserCheckAnswersGet_NoAppsProvided() {
        UserRole userRole = new UserRole();
        userRole.setAppRoleId("app1-tester");
        userRole.setAppId("app1");
        UserRole userRole2 = new UserRole();
        userRole2.setAppRoleId("app1-dev");
        userRole2.setAppId("app1");
        List<String> selectedRoles = List.of("app1-dev");
        HttpSession session = new MockHttpSession();
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new EntraUserDto());
        session.setAttribute("officeData", new OfficeData());
        session.setAttribute("firm", FirmDto.builder().build());
        session.setAttribute("selectedUserType", UserType.EXTERNAL_SINGLE_FIRM);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        EntraUser user = EntraUser.builder().userProfiles(Set.of(UserProfile.builder().build())).build();
        when(userService.createUser(any(), any(), any(), any())).thenReturn(user);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
        assertThat(session.getAttribute("user")).isNotNull();
        assertThat(session.getAttribute("userProfile")).isNotNull();
    }

    @Test
    void addUserCheckAnswersPost() {
        HttpSession session = new MockHttpSession();
        EntraUserDto userDto = new EntraUserDto();
        session.setAttribute("user", userDto);
        session.setAttribute("firm", FirmDto.builder().id(UUID.randomUUID()).name("test firm").build());
        session.setAttribute("selectedUserType", UserType.EXTERNAL_SINGLE_FIRM);
        EntraUser entraUser = EntraUser.builder().build();
        when(userService.createUser(any(), any(), any(), any())).thenReturn(entraUser);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        EntraUser user = EntraUser.builder().userProfiles(Set.of(UserProfile.builder().build())).build();
        when(userService.createUser(any(), any(), any(), any())).thenReturn(user);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
        assertThat(session.getAttribute("user")).isNotNull();
        assertThat(session.getAttribute("userProfile")).isNotNull();
        assertThat(session.getAttribute("firm")).isNull();
        assertThat(session.getAttribute("userType")).isNull();
        verify(eventService).logEvent(any());
    }

    @Test
    void addUserCheckAnswersPost_NoUserProvided() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("app1");
        session.setAttribute("roles", roles);
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        session.setAttribute("selectedUserType", UserType.EXTERNAL_SINGLE_FIRM);
        // Add list appender to logger to verify logs
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserController.class);
        String redirectUrl = userController.addUserCheckAnswers(session, authentication);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/confirmation");
        assertThat(model.getAttribute("roles")).isNull();
        assertThat(model.getAttribute("apps")).isNull();
        assertThat(session.getAttribute("userProfile")).isNull();
        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(logEvents).hasSize(1);
    }

    @Test
    void addUserCreated() {
        HttpSession session = new MockHttpSession();
        EntraUserDto user = new EntraUserDto();
        session.setAttribute("user", user);
        UserProfileDto userProfile = UserProfileDto.builder().id(UUID.randomUUID()).build();
        session.setAttribute("userProfile", userProfile);
        String view = userController.addUserCreated(model, session);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(session.getAttribute("userProfile")).isNull();
        assertThat(model.getAttribute("userProfile")).isNotNull();
        assertThat(view).isEqualTo("add-user-created");
    }

    @Test
    void addUserCreated_NoUserProvided() {
        HttpSession session = new MockHttpSession();
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserController.class);
        String view = userController.addUserCreated(model, session);
        assertThat(model.getAttribute("user")).isNull();
        assertThat(view).isEqualTo("add-user-created");
        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(logEvents).hasSize(1);
    }

    @Test
    public void testEditUserRolesOutputMatchesInput() {
        // Given
        final String userId = "12345";
        // Setup test user call
        EntraUserDto entraUser = new EntraUserDto();
        UserProfileDto testUser = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .build();
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(testUser));
        // Setup test user roles
        AppRoleDto testUserRole = new AppRoleDto();
        testUserRole.setId("testUserAppRoleId");
        List<AppRoleDto> testUserRoles = List.of(testUserRole);
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(testUserRoles);
        // Setup all available roles
        AppRoleDto testRole1 = new AppRoleDto();
        testRole1.setId("testAppRoleId1");
        testRole1.setRoleType(RoleType.EXTERNAL);
        AppRoleDto testRole2 = new AppRoleDto();
        testRole2.setId("testAppRoleId2");
        testRole2.setRoleType(RoleType.EXTERNAL);
        AppRoleDto testRole3 = new AppRoleDto();
        testRole3.setId("testAppRoleId3");
        testRole3.setRoleType(RoleType.EXTERNAL);
        AppRoleDto testRole4 = new AppRoleDto();
        testRole4.setId("testUserAppRoleId");
        testRole4.setRoleType(RoleType.EXTERNAL);
        AppDto currentApp = new AppDto();
        currentApp.setId("testAppId");
        currentApp.setName("testAppName");
        List<String> selectedApps = List.of(currentApp.getId());
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("selectedApps", selectedApps);
        List<AppRoleDto> allRoles = List.of(testRole1, testRole2, testRole3, testRole4);
        when(userService.getAppByAppId(currentApp.getId())).thenReturn(Optional.of(currentApp));
        when(userService.getAppRolesByAppIdAndUserType(eq(currentApp.getId()), any())).thenReturn(allRoles);

        // When
        String view = userController.editUserRoles(userId, 0, new RolesForm(), model, testSession);

        // Then
        Assertions.assertEquals("edit-user-roles", view);
        Assertions.assertSame(model.getAttribute("user"), testUser);
    }

    @Test
    public void testEditUserRoles_view_external_user() {
        // Given
        final String userId = "12345";
        // Setup test user call
        UserProfileDto testUserProfile = new UserProfileDto();
        testUserProfile.setUserType(UserType.EXTERNAL_SINGLE_FIRM);
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(testUserProfile));
        // Setup test user roles
        AppRoleDto testUserRole = new AppRoleDto();
        testUserRole.setId("testUserAppRoleId");
        List<AppRoleDto> testUserRoles = List.of(testUserRole);
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(testUserRoles);
        // Setup all available roles
        AppRoleDto testRole1 = new AppRoleDto();
        testRole1.setId("testAppRoleId1");
        testRole1.setRoleType(RoleType.EXTERNAL);
        AppRoleDto testRole2 = new AppRoleDto();
        testRole2.setId("testAppRoleId2");
        testRole2.setRoleType(RoleType.EXTERNAL);
        AppRoleDto testRole3 = new AppRoleDto();
        testRole3.setId("testAppRoleId3");
        testRole3.setRoleType(RoleType.INTERNAL_AND_EXTERNAL);
        AppDto currentApp = new AppDto();
        currentApp.setId("testAppId");
        currentApp.setName("testAppName");
        List<String> selectedApps = List.of(currentApp.getId());
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("selectedApps", selectedApps);
        List<AppRoleDto> allRoles = List.of(testRole1, testRole2, testRole3);
        when(userService.getAppByAppId(currentApp.getId())).thenReturn(Optional.of(currentApp));
        when(userService.getAppRolesByAppIdAndUserType(currentApp.getId(), UserType.EXTERNAL_SINGLE_FIRM)).thenReturn(allRoles);
        // When
        String view = userController.editUserRoles(userId, 0, new RolesForm(), model, testSession);

        // Then
        List<AppRoleViewModel> appRoleViewModels = (List<AppRoleViewModel>) model.getAttribute("roles");
        Assertions.assertEquals(appRoleViewModels.size(), 3);
    }

    @Test
    public void testEditUserRoles_view_internal_user() {
        // Given
        final String userId = "12345";
        // Setup test user call
        UserProfileDto testUserProfile = new UserProfileDto();
        testUserProfile.setUserType(UserType.INTERNAL);
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(testUserProfile));
        // Setup test user roles
        AppRoleDto testUserRole = new AppRoleDto();
        testUserRole.setId("testUserAppRoleId");
        List<AppRoleDto> testUserRoles = List.of(testUserRole);
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(testUserRoles);
        // Setup all available roles
        AppRoleDto testRole1 = new AppRoleDto();
        testRole1.setId("testAppRoleId1");
        testRole1.setRoleType(RoleType.EXTERNAL);
        AppRoleDto testRole2 = new AppRoleDto();
        testRole2.setId("testAppRoleId2");
        testRole2.setRoleType(RoleType.EXTERNAL);
        AppRoleDto testRole3 = new AppRoleDto();
        testRole3.setId("testAppRoleId3");
        testRole3.setRoleType(RoleType.INTERNAL_AND_EXTERNAL);
        AppRoleDto testRole4 = new AppRoleDto();
        testRole4.setId("testUserAppRoleId");
        testRole4.setRoleType(RoleType.INTERNAL);
        AppDto currentApp = new AppDto();
        currentApp.setId("testAppId");
        currentApp.setName("testAppName");
        List<String> selectedApps = List.of(currentApp.getId());
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("selectedApps", selectedApps);
        List<AppRoleDto> allRoles = List.of(testRole1, testRole2, testRole3, testRole4);
        when(userService.getAppByAppId(currentApp.getId())).thenReturn(Optional.of(currentApp));
        when(userService.getAppRolesByAppIdAndUserType(currentApp.getId(), UserType.INTERNAL)).thenReturn(allRoles);
        // When
        String view = userController.editUserRoles(userId, 0, new RolesForm(), model, testSession);

        // Then
        List<AppRoleViewModel> appRoleViewModels = (List<AppRoleViewModel>) model.getAttribute("roles");
        Assertions.assertEquals(appRoleViewModels.size(), 4);
    }

    @Test
    public void testEditUserRolesThrowsExceptionWhenNoUserProvided() {
        // Given
        final String userId = "12345";
        when(userService.getUserProfileById(userId)).thenReturn(Optional.empty());
        // When
        Assertions.assertThrows(NoSuchElementException.class,
                () -> userController.editUserRoles(userId, 0, new RolesForm(), model, session));
    }

    @Test
    public void testUpdateUserRolesReturnsCorrectView() {
        // Given
        final String userId = "12345";
        final RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("role1"));
        MockHttpSession testSession = new MockHttpSession();
        Model sessionModel = new ExtendedModelMap();
        testSession.setAttribute("userEditRolesModel", sessionModel);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("testUserName");
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        // When
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model,
                testSession);

        // Then
        verify(eventService).logEvent(any());
        verify(loginService).getCurrentUser(authentication);
        Assertions.assertEquals("redirect:/admin/users/manage/" + userId, view);
    }

    @Test
    public void testEditUserAppsReturnsCorrectViewAndAttributes() throws ServletException {
        // Given
        final UUID userId = UUID.randomUUID();
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId(userId.toString());
        entraUser.setFullName("Test User");

        UserProfileDto userProfile = UserProfileDto.builder()
                .id(userId)
                .entraUser(entraUser)
                .userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN)
                .build();

        UUID appId = UUID.randomUUID();
        AppDto testApp = new AppDto();
        testApp.setId(appId.toString());
        testApp.setName("Test App");

        when(userService.getUserProfileById(userId.toString())).thenReturn(Optional.of(userProfile));
        when(userService.getUserAppsByUserId(userId.toString())).thenReturn(Set.of(testApp));
        when(userService.getAppsByUserType(any())).thenReturn(List.of(testApp));

        // When
        String view = userController.editUserApps(userId.toString(), model);

        // Then
        assertThat(view).isEqualTo("edit-user-apps");
        assertThat(model.getAttribute("user")).isNotNull();
        UserProfileDto returnedUser = (UserProfileDto) model.getAttribute("user");
        Assertions.assertEquals(userProfile.getId(), returnedUser.getId());
        Assertions.assertEquals(userProfile.getFullName(), returnedUser.getFullName());

        // Check the correct model attribute name "apps"
        assertThat(model.getAttribute("apps")).isNotNull();
        @SuppressWarnings("unchecked")
        List<AppDto> apps = (List<AppDto>) model.getAttribute("apps");
        assertThat(apps).hasSize(1);
        assertThat(apps.get(0).isSelected()).isTrue(); // Should be selected because user has this app
    }

    @Test
    public void testSetSelectedAppsEditReturnsCorrectRedirectAndAttributes() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        List<String> apps = List.of(appId.toString());
        HttpSession session = new MockHttpSession();

        // When
        RedirectView redirectView = userController.setSelectedAppsEdit(userId.toString(), apps, authentication, session);

        // Then
        assertThat(redirectView.getUrl()).isEqualTo(String.format("/admin/users/edit/%s/roles", userId));
        assertThat(session.getAttribute("selectedApps")).isNotNull();
        List<String> returnedApps = (List<String>) session.getAttribute("selectedApps");
        assertThat(returnedApps).hasSize(1);
        assertThat(returnedApps.getFirst()).isEqualTo(appId.toString());
    }

    @Test
    public void testSetSelectedAppsEdit_shouldHandleNoAppsSelected() {
        // Given
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("tester");
        UUID userId = UUID.randomUUID();
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        HttpSession session = new MockHttpSession();
        // When - passing null for apps (simulates no checkboxes selected)
        RedirectView redirectView = userController.setSelectedAppsEdit(userId.toString(), null, authentication, session);

        // Then - should redirect to manage user page when no apps selected
        assertThat(redirectView.getUrl()).isEqualTo(String.format("/admin/users/manage/%s", userId));
        assertThat(session.getAttribute("selectedApps")).isNotNull();
        @SuppressWarnings("unchecked")
        List<String> returnedApps = (List<String>) session.getAttribute("selectedApps");
        assertThat(returnedApps).isEmpty();

        // Verify that updateUserRoles was called with empty list to persist the change
        verify(userService).updateUserRoles(userId.toString(), new ArrayList<>());
    }

    @Test
    public void testSetSelectedAppsEdit_shouldHandleEmptyAppsList() {
        // Given
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        UUID userId = UUID.randomUUID();
        List<String> apps = new ArrayList<>(); // Empty list
        HttpSession session = new MockHttpSession();
        // When
        RedirectView redirectView = userController.setSelectedAppsEdit(userId.toString(), apps, authentication, session);

        // Then - should redirect to manage user page when empty apps list
        assertThat(redirectView.getUrl()).isEqualTo(String.format("/admin/users/manage/%s", userId));
        assertThat(session.getAttribute("selectedApps")).isNotNull();
        @SuppressWarnings("unchecked")
        List<String> returnedApps = (List<String>) session.getAttribute("selectedApps");
        assertThat(returnedApps).isEmpty();

        // Verify that updateUserRoles was called with empty list to persist the change
        verify(userService).updateUserRoles(userId.toString(), new ArrayList<>());
    }

    // ===== NEW EDIT USER FUNCTIONALITY TESTS =====

    @Test
    void editUserDetails_shouldPopulateFormAndReturnView() {
        // Given
        String userId = "user123";
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId(userId);
        entraUser.setFirstName("John");
        entraUser.setLastName("Doe");
        entraUser.setEmail("john.doe@example.com");

        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .build();

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));

        // When
        String view = userController.editUserDetails(userId, model);

        // Then
        assertThat(view).isEqualTo("edit-user-details");
        assertThat(model.getAttribute("user")).isEqualTo(userProfile);

        EditUserDetailsForm form = (EditUserDetailsForm) model.getAttribute("editUserDetailsForm");
        assertThat(form).isNotNull();
        assertThat(form.getFirstName()).isEqualTo("John");
        assertThat(form.getLastName()).isEqualTo("Doe");
        assertThat(form.getEmail()).isEqualTo("john.doe@example.com");

        verify(userService).getUserProfileById(userId);
    }

    @Test
    void editUserDetails_shouldThrowExceptionWhenUserNotFound() {
        // Given
        String userId = "nonexistent";
        when(userService.getUserProfileById(userId)).thenReturn(Optional.empty());

        // When & Then
        Assertions.assertThrows(NoSuchElementException.class,
                () -> userController.editUserDetails(userId, model));
    }

    @Test
    void updateUserDetails_shouldUpdateUserAndRedirect() throws IOException {
        // Given
        final String userId = "user123";
        EditUserDetailsForm form = new EditUserDetailsForm();
        form.setFirstName("Jane");
        form.setLastName("Smith");
        form.setEmail("jane.smith@example.com");

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        MockHttpSession testSession = new MockHttpSession();

        // When
        String view = userController.updateUserDetails(userId, form, bindingResult, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        verify(userService).updateUserDetails(userId, "Jane", "Smith");
    }

    @Test
    void updateUserDetails_shouldReturnToFormOnValidationErrors() throws IOException {
        // Given
        final String userId = "user123";
        EditUserDetailsForm form = new EditUserDetailsForm();
        form.setFirstName("Jane");
        form.setLastName("Smith");
        form.setEmail("jane.smith@example.com");

        EntraUserDto entraUser = new EntraUserDto();
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .build();
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        MockHttpSession testSession = new MockHttpSession();

        // When
        String view = userController.updateUserDetails(userId, form, bindingResult, testSession);

        // Then
        assertThat(view).isEqualTo("edit-user-details");
        assertThat(testSession.getAttribute("user")).isEqualTo(userProfile);
        assertThat(testSession.getAttribute("editUserDetailsForm")).isEqualTo(form);
        verify(userService, Mockito.never()).updateUserDetails(anyString(), anyString(), anyString());
    }

    @Test
    void editUserApps_shouldPopulateAppsAndReturnView() {
        // Given
        String userId = "user123";
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId(userId);

        AppDto app1 = new AppDto();
        app1.setId("app1");
        app1.setName("App 1");

        AppDto app2 = new AppDto();
        app2.setId("app2");
        app2.setName("App 2");

        Set<AppDto> userApps = Set.of(app1);
        List<AppDto> allApps = List.of(app1, app2);

        final UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .userType(UserType.EXTERNAL_SINGLE_FIRM)
                .build();

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));
        when(userService.getUserAppsByUserId(userId)).thenReturn(userApps);
        when(userService.getAppsByUserType(UserType.EXTERNAL_SINGLE_FIRM)).thenReturn(allApps);

        // When
        String view = userController.editUserApps(userId, model);

        // Then
        assertThat(view).isEqualTo("edit-user-apps");
        assertThat(model.getAttribute("user")).isEqualTo(userProfile);

        @SuppressWarnings("unchecked")
        List<AppDto> apps = (List<AppDto>) model.getAttribute("apps");
        assertThat(apps).hasSize(2);
        assertThat(apps.get(0).isSelected()).isTrue(); // app1 should be selected
        assertThat(apps.get(1).isSelected()).isFalse(); // app2 should not be selected
    }

    @Test
    void setSelectedAppsEdit_shouldStoreAppsInSessionAndRedirect() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID
        List<String> apps = List.of("app1", "app2");
        MockHttpSession testSession = new MockHttpSession();

        // When
        RedirectView redirectView = userController.setSelectedAppsEdit(userId, apps, authentication, testSession);

        // Then
        assertThat(redirectView.getUrl()).isEqualTo("/admin/users/edit/" + userId + "/roles");
        assertThat(testSession.getAttribute("selectedApps")).isEqualTo(apps);
    }

    @Test
    void editUserOffices_shouldPopulateOfficesAndReturnView() {
        // Given
        String userId = "user123";
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId(userId);

        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .build();

        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        Office office1 = Office.builder().id(UUID.randomUUID()).address(address).build();
        OfficeDto office1Dto = OfficeDto.builder().id(office1.getId())
                .address(OfficeDto.AddressDto.builder().addressLine1(address.getAddressLine1())
                        .city(address.getCity()).postcode(address.getPostcode()).build()).build();
        Office office2 = Office.builder().id(UUID.randomUUID()).address(address).build();
        List<Office> allOffices = List.of(office1, office2);

        List<OfficeDto> userOffices = List.of(office1Dto);

        // Mock user firms for the new firmService call
        FirmDto firmDto = FirmDto.builder().id(UUID.randomUUID()).build();
        List<FirmDto> userFirms = List.of(firmDto);

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));
        when(firmService.getUserFirmsByUserId(userId)).thenReturn(userFirms);
        when(officeService.getOfficesByFirms(anyList())).thenReturn(allOffices);
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);
        MockHttpSession testSession = new MockHttpSession();

        // When
        String view = userController.editUserOffices(userId, model, testSession);

        // Then
        assertThat(view).isEqualTo("edit-user-offices");
        assertThat(model.getAttribute("user")).isEqualTo(userProfile);
        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(false);

        @SuppressWarnings("unchecked")
        List<OfficeModel> offices = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(offices).hasSize(2);
        assertThat(offices.get(0).isSelected()).isTrue(); // office1 should be selected
        assertThat(offices.get(1).isSelected()).isFalse(); // office2 should not be selected
    }

    @Test
    void editUserOffices_shouldHandleAccessToAllOffices() {
        // Given
        String userId = "user123";
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId(userId);

        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .build();

        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        Office office1 = Office.builder().id(UUID.randomUUID()).address(address).build();
        OfficeDto office1Dto = OfficeDto.builder().id(office1.getId())
                .address(OfficeDto.AddressDto.builder().addressLine1(address.getAddressLine1())
                        .city(address.getCity()).postcode(address.getPostcode()).build()).build();
        Office office2 = Office.builder().id(UUID.randomUUID()).address(address).build();
        OfficeDto office2Dto = OfficeDto.builder().id(office2.getId())
                .address(OfficeDto.AddressDto.builder().addressLine1(address.getAddressLine1())
                        .city(address.getCity()).postcode(address.getPostcode()).build()).build();
        List<Office> allOffices = List.of(office1, office2);

        List<OfficeDto> userOffices = List.of(office1Dto, office2Dto); // User has access to all offices

        // Mock user firms for the new firmService call
        FirmDto firmDto = FirmDto.builder().id(UUID.randomUUID()).build();
        List<FirmDto> userFirms = List.of(firmDto);

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));
        when(firmService.getUserFirmsByUserId(userId)).thenReturn(userFirms);
        when(officeService.getOfficesByFirms(anyList())).thenReturn(allOffices);
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);
        MockHttpSession testSession = new MockHttpSession();

        // When
        String view = userController.editUserOffices(userId, model, testSession);

        // Then
        assertThat(view).isEqualTo("edit-user-offices");
        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<OfficeModel> offices = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(offices).hasSize(2);
        // All offices should be selected when user has access to all
        assertThat(offices.get(0).isSelected()).isTrue();
        assertThat(offices.get(1).isSelected()).isTrue();
    }

    @Test
    void updateUserOffices_shouldUpdateOfficesAndRedirect() throws IOException {
        // Given
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of("office1", "office2"));

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        OfficeModel.Address address = OfficeModel.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        OfficeModel of1 = new OfficeModel();
        of1.setId("office1");
        of1.setAddress(address);
        OfficeModel of2 = new OfficeModel();
        of2.setId("office2");
        of2.setAddress(address);
        List<OfficeModel> officeData = List.of(of1, of2);
        model.addAttribute("officeData", officeData);
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("editUserOfficesModel", model);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        String userId = "user123";
        // When
        String view = userController.updateUserOffices(userId, form, bindingResult, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        verify(userService).updateUserOffices(userId, List.of("office1", "office2"));
        ArgumentCaptor<UpdateUserAuditEvent> captor = ArgumentCaptor.forClass(UpdateUserAuditEvent.class);
        verify(eventService).logEvent(captor.capture());
        UpdateUserAuditEvent updateUserAuditEvent = captor.getValue();
        assertThat(updateUserAuditEvent.getField()).isEqualTo("office");
        assertThat(updateUserAuditEvent.getChangedValues()).hasSize(2);
    }

    @Test
    void updateUserOffices_shouldHandleAccessToAllOffices() throws IOException {
        // Given
        final String userId = "user123";
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of("ALL")); // Special value for "Access to all offices"

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        MockHttpSession testSession = new MockHttpSession();
        // When
        String view = userController.updateUserOffices(userId, form, bindingResult, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        // Should pass 'All' to service when option is selected
        List<String> expectedOfficeIds = List.of("ALL");
        verify(userService).updateUserOffices(userId, expectedOfficeIds);
    }

    @Test
    void updateUserOffices_shouldReturnToFormOnValidationErrors() throws IOException {
        // Given
        final String userId = "user123";
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of("office1"));

        EntraUserDto user = new EntraUserDto();
        OfficeModel.Address address = OfficeModel.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        List<OfficeModel> officeData = List.of(new OfficeModel("Test Office", address, "office1", true));

        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("user", user);
        sessionModel.addAttribute("officeData", officeData);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("editUserOfficesModel", sessionModel);

        // When
        String view = userController.updateUserOffices(userId, form, bindingResult, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("edit-user-offices");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("officeData")).isEqualTo(officeData);
        verify(userService, Mockito.never()).updateUserOffices(anyString(), anyList());
    }

    @Test
    void cancelUserEdit_shouldClearSessionAndRedirect() {
        // Given
        String userId = "user123";
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("selectedApps", List.of("app1"));
        testSession.setAttribute("editUserRoles", List.of("role1"));
        testSession.setAttribute("userEditRolesModel", new ExtendedModelMap());
        testSession.setAttribute("editUserRolesCurrentApp", new AppDto());
        testSession.setAttribute("editUserRolesSelectedAppIndex", 0);

        // When
        String view = userController.cancelUserEdit(userId, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        assertThat(testSession.getAttribute("selectedApps")).isNull();
        assertThat(testSession.getAttribute("editUserRoles")).isNull();
        assertThat(testSession.getAttribute("userEditRolesModel")).isNull();
        assertThat(testSession.getAttribute("editUserRolesCurrentApp")).isNull();
        assertThat(testSession.getAttribute("editUserRolesSelectedAppIndex")).isNull();
    }

    @Test
    void editUserRoles_shouldAutoPopulateSelectedAppsFromUserApps() {
        // Given
        String userId = "user123";
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId(userId);

        AppDto userApp1 = new AppDto();
        userApp1.setId("app1");
        AppDto userApp2 = new AppDto();
        userApp2.setId("app2");
        Set<AppDto> userApps = new LinkedHashSet<>();
        userApps.add(userApp1);
        userApps.add(userApp2);

        AppDto currentApp = new AppDto();
        currentApp.setId("app1");
        currentApp.setName("App 1");

        AppRoleDto role = new AppRoleDto();
        role.setRoleType(RoleType.EXTERNAL);
        List<AppRoleDto> appRoles = List.of(role);

        final UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .build();

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));
        when(userService.getUserAppsByUserId(userId)).thenReturn(userApps);
        when(userService.getAppByAppId("app1")).thenReturn(Optional.of(currentApp));
        lenient().when(userService.getAppByAppId("app2")).thenReturn(Optional.of(userApp2));
        when(userService.getAppRolesByAppIdAndUserType(eq("app1"), any())).thenReturn(appRoles);
        lenient().when(userService.getAppRolesByAppId("app2")).thenReturn(List.of());
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(List.of());

        MockHttpSession testSession = new MockHttpSession(); // No selectedApps in session

        // When
        String view = userController.editUserRoles(userId, 0, new RolesForm(), model, testSession);

        // Then
        assertThat(view).isEqualTo("edit-user-roles");
        @SuppressWarnings("unchecked")
        List<String> selectedApps = (List<String>) testSession.getAttribute("selectedApps");
        assertThat(selectedApps).containsExactlyInAnyOrder("app1", "app2");
    }

    @Test
    void updateUserRoles_shouldHandleMultipleAppsNavigation() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("role1", "role2"));

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("userEditRolesModel", new ExtendedModelMap());
        testSession.setAttribute("selectedApps", List.of("app1", "app2"));

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        // When - updating roles for first app (index 0)
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model,
                testSession);

        // Then - should redirect to next app (index 1)
        assertThat(view).isEqualTo("redirect:/admin/users/edit/" + userId + "/roles?selectedAppIndex=1");

        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> allRoles = (Map<Integer, List<String>>) testSession
                .getAttribute("editUserAllSelectedRoles");
        assertThat(allRoles).isNotNull();
        assertThat(allRoles.get(0)).containsExactlyInAnyOrder("role1", "role2");
    }

    @Test
    void updateUserRoles_shouldCompleteEditingOnLastApp() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("role3"));

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("userEditRolesModel", new ExtendedModelMap());
        testSession.setAttribute("selectedApps", List.of("app1", "app2"));

        // Simulate roles for previous apps already selected
        Map<Integer, List<String>> existingRoles = new HashMap<>();
        existingRoles.put(0, List.of("role1", "role2"));
        testSession.setAttribute("editUserAllSelectedRoles", existingRoles);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("testUserName");
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        // When - updating roles for last app (index 1)
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 1, authentication, model,
                testSession);

        // Then - should complete editing and redirect to manage user
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);

        // The controller flattens all roles from all apps and passes them to
        // updateUserRoles
        List<String> allSelectedRoles = List.of("role1", "role2", "role3");
        verify(userService).updateUserRoles(userId, allSelectedRoles);
        verify(eventService).logEvent(any());
    }

    @Test
    void updateUserRoles_shouldRedirectWhenSessionModelMissing() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440000"; // Valid UUID
        RolesForm rolesForm = new RolesForm();
        MockHttpSession testSession = new MockHttpSession(); // No session model
        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        // When
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model,
                testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/edit/" + userId + "/roles");
    }

    @Test
    void displayAllUsers_shouldHandleExternalUserWithFirms() {
        // Given
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(new ArrayList<>());
        paginatedUsers.setTotalUsers(5);
        paginatedUsers.setTotalPages(1);

        EntraUser externalUser = EntraUser.builder().build();
        FirmDto userFirm = FirmDto.builder().id(UUID.randomUUID()).name("Firm 1").build();

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(externalUser);
        when(firmService.getUserFirm(externalUser)).thenReturn(Optional.of(userFirm));
        when(userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(eq(null), anyList(), any(), eq(1), eq(10), eq(null),
                eq(null)))
                .thenReturn(paginatedUsers);

        when(session.getAttribute("successMessage")).thenReturn("User added successfully");

        String view = userController.displayAllUsers(10, 1, null, null, null, null, true, model, session,
                authentication);

        // Then
        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("internal")).isEqualTo(false);
        assertThat(model.getAttribute("showFirmAdmins")).isEqualTo(true);
        assertThat(model.getAttribute("allowCreateUser")).isEqualTo(false);
        verify(firmService).getUserFirm(externalUser);
    }

    @Test
    void displayAllUsers_shouldHandleInternalUserWithUserType() {
        // Given
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(new ArrayList<>());

        EntraUser internalUser = EntraUser.builder().build();

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(internalUser);
        when(userService.getPageOfUsersByNameOrEmailAndPermissionsAndFirm(eq("admin"), anyList(), isNull(), eq(1), eq(10), isNull(), isNull()))
                .thenReturn(paginatedUsers);
        when(accessControlService.authenticatedUserHasPermission(Permission.VIEW_INTERNAL_USER)).thenReturn(true);
        when(accessControlService.authenticatedUserHasPermission(Permission.CREATE_EXTERNAL_USER)).thenReturn(true);
        when(accessControlService.authenticatedUserHasPermission(Permission.VIEW_EXTERNAL_USER)).thenReturn(false);

        // When
        String view = userController.displayAllUsers(10, 1, null, null, "internal", "admin", false, model, session, authentication);

        // Then
        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("usertype")).isEqualTo("internal");
        assertThat(model.getAttribute("internal")).isEqualTo(true);
        assertThat(model.getAttribute("allowCreateUser")).isEqualTo(true);
    }

    @Test
    void setSelectedApps_shouldRedirectToCheckAnswersWhenNoAppsSelected() {
        // Given
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(null); // No apps selected
        MockHttpSession testSession = new MockHttpSession();

        // When
        String view = userController.setSelectedApps(applicationsForm, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
        assertThat(testSession.getAttribute("apps")).isNull();
    }

    @Test
    void setSelectedApps_shouldRedirectToCheckAnswersWhenEmptyAppsList() {
        // Given
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(new ArrayList<>()); // Empty list
        MockHttpSession testSession = new MockHttpSession();

        // When
        String view = userController.setSelectedApps(applicationsForm, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
        assertThat(testSession.getAttribute("apps")).isNull();
    }

    @Test
    void setSelectedApps_shouldClearUserCreateRolesModelFromSession() {
        // Given
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(List.of("app1", "app2"));
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("userCreateRolesModel", new ExtendedModelMap());

        // When
        String view = userController.setSelectedApps(applicationsForm, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/user/create/roles");
        assertThat(testSession.getAttribute("userCreateRolesModel")).isNull();
        assertThat(testSession.getAttribute("apps")).isEqualTo(List.of("app1", "app2"));
    }

    @Test
    void getSelectedRoles_shouldUseSessionModelAppIndex() {
        // Given
        List<String> selectedApps = List.of("app1", "app2");
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("apps", selectedApps);
        testSession.setAttribute("user", new EntraUserDto());

        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("createUserRolesSelectedAppIndex", 1); // Second app
        testSession.setAttribute("userCreateRolesModel", sessionModel);

        AppDto currentApp = new AppDto();
        currentApp.setId("app2");
        currentApp.setName("App 2");

        List<AppRoleDto> roles = List.of(new AppRoleDto());

        when(userService.getAppByAppId("app2")).thenReturn(Optional.of(currentApp));
        when(userService.getAppRolesByAppIdAndUserType("app2", UserType.EXTERNAL_SINGLE_FIRM)).thenReturn(roles);

        // When
        String view = userController.getSelectedRoles(new RolesForm(), model, testSession);

        // Then
        assertThat(view).isEqualTo("add-user-roles");
        assertThat(model.getAttribute("createUserRolesSelectedAppIndex")).isEqualTo(1);
        assertThat(model.getAttribute("createUserRolesCurrentApp")).isEqualTo(currentApp);
    }

    @Test
    void postOffices_shouldHandleValidationErrorsWithNoSessionModel() {
        // Given
        OfficesForm officesForm = new OfficesForm();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        MockHttpSession testSession = new MockHttpSession();
        // No session model present

        // When
        String view = userController.postOffices(officesForm, bindingResult, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/user/create/offices");
    }

    @Test
    void getUserCheckAnswers_shouldHandleEmptySelectedApps() {
        // Given
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("apps", new ArrayList<String>()); // Empty apps list
        testSession.setAttribute("user", new EntraUserDto());
        testSession.setAttribute("officeData", new OfficeData());

        // When
        String view = userController.getUserCheckAnswers(model, testSession);

        // Then
        assertThat(view).isEqualTo("add-user-check-answers");
        assertThat(model.getAttribute("roles")).isNull();
    }

    @Test
    void disableUsers_shouldCallServiceAndRedirect() throws IOException {
        List<String> ids = List.of("id1", "id2");
        String view = userController.disableUsers(ids);
        assertThat(view).isEqualTo("redirect:/users");
        verify(userService).disableUsers(ids);
    }

    @Test
    void manageUser_shouldAddUserAppRolesAndOfficesToModel() {
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId("id1");
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .appRoles(List.of(new AppRoleDto()))
                .offices(List.of(OfficeDto.builder()
                        .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440001"))
                        .code("Test Office")
                        .address(OfficeDto.AddressDto.builder().addressLine1("Test Address").build())
                        .build()))
                .build();
        when(userService.getUserProfileById("id1")).thenReturn(Optional.of(userProfile));

        String view = userController.manageUser("id1", model);

        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(userProfile);
    }

    @Test
    void manageUser_shouldHandleUserNotPresent() {
        when(userService.getUserProfileById("id2")).thenReturn(Optional.empty());

        // Expect exception since controller calls .get() on empty Optional
        Assertions.assertThrows(NoSuchElementException.class,
                () -> userController.manageUser("id2", model));
    }

    @Test
    void createUser_shouldPopulateModelWithFirmsAndSelectedFirm() {
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("selectedUserType")).thenReturn(null);

        UserDetailsForm form = new UserDetailsForm();
        String view = userController.createUser(form, session, model);

        assertThat(view).isEqualTo("add-user-details");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("userTypes")).isNotNull();
        assertThat(model.getAttribute("selectedUserType")).isNull();
    }

    @Test
    void postUser_shouldHandleValidationErrors() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("userTypes", List.of());
        sessionModel.addAttribute("selectedUserType", null);
        sessionModel.addAttribute("user", new EntraUserDto());
        Mockito.lenient().when(session.getAttribute("createUserDetailsModel")).thenReturn(sessionModel);

        final Model model = new ExtendedModelMap();
        UserDetailsForm form = new UserDetailsForm();
        form.setUserType(UserType.EXTERNAL_SINGLE_FIRM);
        String view = userController.postUser(form, result, session, model);

        assertThat(view).isEqualTo("add-user-details");
        assertThat(model.getAttribute("userTypes")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void postUser_shouldRedirectOnNoValidationErrors() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);
        UserDetailsForm form = new UserDetailsForm();
        form.setFirstName("A");
        form.setLastName("B");
        form.setEmail("a@b.com");
        form.setUserType(UserType.EXTERNAL_SINGLE_FIRM);

        final Model model = new ExtendedModelMap();
        HttpSession session = new MockHttpSession();

        String view = userController.postUser(form, result, session, model);

        assertThat(view).isEqualTo("redirect:/admin/user/create/firm");
        assertThat(session.getAttribute("selectedUserType")).isEqualTo(UserType.EXTERNAL_SINGLE_FIRM);
        assertThat(session.getAttribute("user")).isNotNull();
    }

    @Test
    void selectUserApps_shouldAddAppsAndUserToModel() {
        AppDto appDto = new AppDto();
        appDto.setId("app1");
        when(userService.getAppsByUserType(any())).thenReturn(List.of(appDto));
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", List.of("app1"));
        session.setAttribute("user", new EntraUserDto());
        ApplicationsForm form = new ApplicationsForm();

        String view = userController.selectUserApps(form, model, session);

        assertThat(view).isEqualTo("add-user-apps");
        assertThat(model.getAttribute("apps")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void setSelectedApps_shouldRedirectToCheckAnswersIfNoApps() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(new ArrayList<>());
        HttpSession session = new MockHttpSession();
        Model model = new ExtendedModelMap();

        String view = userController.setSelectedApps(form, model, session);

        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
    }

    @Test
    void setSelectedApps_shouldSetAppsAndRedirectToRoles() {
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(List.of("app1"));
        HttpSession session = new MockHttpSession();
        final Model model = new ExtendedModelMap();

        String view = userController.setSelectedApps(form, model, session);

        assertThat(view).isEqualTo("redirect:/admin/user/create/roles");
        assertThat(session.getAttribute("apps")).isEqualTo(List.of("app1"));
    }

    @Test
    void setSelectedApps_shouldHandleValidationErrors() {
        // Given
        ApplicationsForm form = new ApplicationsForm();
        form.setApps(null); // This should trigger validation
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", new EntraUserDto());

        // When
        String view = userController.setSelectedApps(form, model, session);

        // Then - should redirect to check answers when no apps selected
        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
    }

    @Test
    void selectUserApps_shouldHandleFormPopulation() {
        // Given
        AppDto app1 = new AppDto();
        app1.setId("app1");
        app1.setName("App 1");
        
        AppDto app2 = new AppDto();
        app2.setId("app2");
        app2.setName("App 2");
        
        when(userService.getAppsByUserType(any())).thenReturn(List.of(app1, app2));
        
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("apps", List.of("app1")); // Pre-selected app
        session.setAttribute("user", new EntraUserDto());
        
        ApplicationsForm form = new ApplicationsForm();

        // When
        String view = userController.selectUserApps(form, model, session);

        // Then
        assertThat(view).isEqualTo("add-user-apps");
        assertThat(model.getAttribute("apps")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
        
        @SuppressWarnings("unchecked")
        List<AppViewModel> apps = (List<AppViewModel>) model.getAttribute("apps");
        assertThat(apps).hasSize(2);
        assertThat(apps.get(0).isSelected()).isTrue(); // app1 should be selected
        assertThat(apps.get(1).isSelected()).isFalse(); // app2 should not be selected
    }

    @Test
    void setSelectedRoles_shouldHandleValidationErrors() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);

        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("roles", List.of());
        sessionModel.addAttribute("user", new User());
        HttpSession session = new MockHttpSession();
        session.setAttribute("userCreateRolesModel", sessionModel);

        RolesForm form = new RolesForm();
        Model model = new ExtendedModelMap();
        String view = userController.setSelectedRoles(form, result, model, session);

        assertThat(view).isEqualTo("add-user-roles");
        assertThat(model.getAttribute("roles")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void setSelectedRoles_shouldSetRolesAndRedirect() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);
        HttpSession session = new MockHttpSession();
        RolesForm form = new RolesForm();
        form.setRoles(List.of("role1"));
        final Model model = new ExtendedModelMap();
        model.addAttribute("createUserRolesSelectedAppIndex", 0);
        session.setAttribute("userCreateRolesModel", model);

        String view = userController.setSelectedRoles(form, result, model, session);

        assertThat(view).isEqualTo("redirect:/admin/user/create/offices");
        assertThat(session.getAttribute("roles")).isEqualTo(List.of("role1"));
    }

    @Test
    void offices_shouldAddOfficeDataAndUserToModel() {
        UUID firmId = UUID.randomUUID();
        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        Office office = Office.builder().id(UUID.randomUUID()).address(address).build();
        when(officeService.getOfficesByFirms(List.of(firmId))).thenReturn(List.of(office));

        FirmDto selectedFirm = new FirmDto();
        selectedFirm.setId(firmId);

        HttpSession session = new MockHttpSession();
        session.setAttribute("user", new EntraUserDto());
        session.setAttribute("firm", selectedFirm);
        OfficesForm form = new OfficesForm();

        String view = userController.offices(form, session, model);

        assertThat(view).isEqualTo("add-user-offices");
        assertThat(model.getAttribute("officeData")).isNotNull();
        assertThat(model.getAttribute("user")).isNotNull();
    }

    @Test
    void offices_shouldThrowExceptionWhenUserIsNotPresent() {
        UUID firmId = UUID.randomUUID();
        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        Office office = Office.builder().id(UUID.randomUUID()).address(address).build();
        when(officeService.getOfficesByFirms(List.of(firmId))).thenReturn(List.of(office));

        FirmDto selectedFirm = new FirmDto();
        selectedFirm.setId(firmId);

        HttpSession session = new MockHttpSession();
        session.setAttribute("firm", selectedFirm);
        // Note: not setting "user" in session to trigger the exception
        OfficesForm form = new OfficesForm();

        assertThrows(CreateUserDetailsIncompleteException.class, () -> userController.offices(form, session, model));
    }

    @Test
    void postOffices_shouldHandleValidationErrors() {
        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(true);
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("user", new User());
        sessionModel.addAttribute("officeData", List.of());
        HttpSession session = new MockHttpSession();
        session.setAttribute("createUserOfficesModel", sessionModel);

        final Model model = new ExtendedModelMap();
        OfficesForm form = new OfficesForm();

        String view = userController.postOffices(form, result, model, session);

        assertThat(view).isEqualTo("add-user-offices");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("officeData")).isNotNull();
    }

    @Test
    void postOffices_shouldSetOfficeDataAndRedirect() {
        UUID firmId = UUID.randomUUID();
        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        Office office = Office.builder().id(UUID.randomUUID()).code("Office1").address(address).build();
        when(officeService.getOfficesByFirms(List.of(firmId))).thenReturn(List.of(office));

        FirmDto selectedFirm = new FirmDto();
        selectedFirm.setId(firmId);

        BindingResult result = Mockito.mock(BindingResult.class);
        when(result.hasErrors()).thenReturn(false);
        OfficesForm form = new OfficesForm();
        form.setOffices(List.of(office.getId().toString()));
        HttpSession session = new MockHttpSession();
        session.setAttribute("firm", selectedFirm);
        final Model model = new ExtendedModelMap();

        String view = userController.postOffices(form, result, model, session);

        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
        OfficeData officeData = (OfficeData) session.getAttribute("officeData");
        assertThat(officeData.getSelectedOffices()).containsExactly(office.getId().toString());
        assertThat(officeData.getSelectedOfficesDisplay()).containsExactly("Office1");
    }

    @Test
    void getUserCheckAnswers_shouldAddAttributesToModel() {
        HttpSession session = new MockHttpSession();
        session.setAttribute("user", new EntraUserDto());
        session.setAttribute("firm", FirmDto.builder().build());
        session.setAttribute("selectedUserType", UserType.EXTERNAL_SINGLE_FIRM);

        String view = userController.getUserCheckAnswers(model, session);

        assertThat(view).isEqualTo("add-user-check-answers");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("firm")).isNotNull();
        assertThat(model.getAttribute("userType")).isEqualTo(UserType.EXTERNAL_SINGLE_FIRM);
    }

    @Test
    void addUserCheckAnswers_shouldCallCreateUserAndRedirect() {
        MockHttpSession mockSession = new MockHttpSession();
        EntraUserDto user = new EntraUserDto();
        mockSession.setAttribute("user", user);
        mockSession.setAttribute("selectedUserType", UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
        // No other session attributes set

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);

        EntraUser entraUser = EntraUser.builder().userProfiles(Set.of(UserProfile.builder().build())).build();
        when(userService.createUser(eq(user), any(FirmDto.class), any(UserType.class), eq("tester")))
                .thenReturn(entraUser);

        // When
        String view = userController.addUserCheckAnswers(mockSession, authentication);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/user/create/confirmation");
        verify(userService).createUser(eq(user), any(FirmDto.class), any(UserType.class), eq("tester"));
    }

    @Test
    void cancelUserCreation_shouldClearAllSessionAttributes() {
        // Given
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("user", new EntraUserDto());
        testSession.setAttribute("firm", new FirmDto());
        testSession.setAttribute("isFirmAdmin", true);
        testSession.setAttribute("apps", List.of("app1"));
        testSession.setAttribute("roles", List.of("role1"));
        testSession.setAttribute("officeData", new OfficeData());

        // When
        String view = userController.cancelUserCreation(testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(testSession.getAttribute("user")).isNull();
        assertThat(testSession.getAttribute("userProfile")).isNull();
        assertThat(testSession.getAttribute("firm")).isNull();
        assertThat(testSession.getAttribute("isFirmAdmin")).isNull();
        assertThat(testSession.getAttribute("apps")).isNull();
        assertThat(testSession.getAttribute("roles")).isNull();
        assertThat(testSession.getAttribute("officeData")).isNull();
    }

    @Test
    void editUserRoles_shouldRedirectWhenNoAppsAssigned() {
        // Given
        String userId = "user123";
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setId(userId);
        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"))
                .entraUser(entraUser)
                .build();

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));
        when(userService.getUserAppsByUserId(userId)).thenReturn(Set.of()); // No apps

        MockHttpSession testSession = new MockHttpSession();

        // When
        String view = userController.editUserRoles(userId, 0, new RolesForm(), model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
    }

    @Test
    void editUserRoles_shouldHandleSelectedAppIndexOutOfBounds() {
        // Given
        String userId = "user123";
        EntraUserDto user = new EntraUserDto();
        user.setId(userId);

        UserProfileDto userProfile = UserProfileDto.builder()
                .entraUser(user)
                .build();
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("selectedApps", List.of("app1"));

        // Mock for the logic when selectedAppIndex is reset to 0
        AppDto currentApp = new AppDto();
        currentApp.setId("app1");
        when(userService.getAppByAppId("app1")).thenReturn(Optional.of(currentApp));
        when(userService.getAppRolesByAppIdAndUserType(eq("app1"), any())).thenReturn(List.of());
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(List.of());

        // When - passing selectedAppIndex of 5 which is out of bounds
        String view = userController.editUserRoles(userId, 5, new RolesForm(), model, testSession);

        // Then
        assertThat(view).isEqualTo("edit-user-roles");
        assertThat(model.getAttribute("editUserRolesSelectedAppIndex")).isEqualTo(0); // Should reset to 0

        // Verify that the calls were made as expected
        verify(userService).getAppByAppId("app1");
        verify(userService).getAppRolesByAppIdAndUserType(eq("app1"), any());
        verify(userService).getUserAppRolesByUserId(userId);
    }

    @Test
    void updateUserRoles_shouldHandleValidationErrorsWithRoleDeselection() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440000";
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(null); // No roles selected (validation error scenario)

        final MockHttpSession testSession = new MockHttpSession();
        final Model sessionModel = new ExtendedModelMap();

        // Create some roles with one initially selected
        AppRoleViewModel role1 = new AppRoleViewModel();
        role1.setId("role1");
        role1.setSelected(true);
        AppRoleViewModel role2 = new AppRoleViewModel();
        role2.setId("role2");
        role2.setSelected(false);

        sessionModel.addAttribute("roles", List.of(role1, role2));
        sessionModel.addAttribute("user", new EntraUserDto());
        sessionModel.addAttribute("editUserRolesSelectedAppIndex", 0);
        sessionModel.addAttribute("editUserRolesCurrentApp", new AppDto());

        testSession.setAttribute("userEditRolesModel", sessionModel);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String view = userController.updateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model,
                testSession);

        // Then
        assertThat(view).isEqualTo("edit-user-roles");
        @SuppressWarnings("unchecked")
        List<AppRoleViewModel> roles = (List<AppRoleViewModel>) model.getAttribute("roles");
        assertThat(roles).hasSize(2);
        // Both roles should be deselected since no roles were provided in form
        assertThat(roles.get(0).isSelected()).isFalse();
        assertThat(roles.get(1).isSelected()).isFalse();
    }

    @Test
    void updateUserOffices_shouldHandleNullOfficesForm() throws IOException {
        // Given
        final String userId = "user123";
        OfficesForm form = new OfficesForm();
        form.setOffices(null); // Null offices list

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("tester");
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        MockHttpSession testSession = new MockHttpSession();
        // When
        String view = userController.updateUserOffices(userId, form, bindingResult, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        verify(userService).updateUserOffices(userId, new ArrayList<>());
        assertThat(testSession.getAttribute("editUserOfficesModel")).isNull();
    }

    @Test
    void editUser_shouldNotAddRolesAttributeWhenUserNotFound() {
        // Given
        String userId = "nonexistent";
        when(userService.getUserProfileById(userId)).thenReturn(Optional.empty());

        // When
        String view = userController.editUser(userId, model);

        // Then
        assertThat(view).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isNull();
        assertThat(model.getAttribute("roles")).isNull();
        verify(userService, Mockito.never()).getUserAppRolesByUserId(anyString());
    }

    @Test
    void createUser_shouldHandleExistingUserAndFirmInSession() {
        // Given
        EntraUserDto existingUser = new EntraUserDto();
        existingUser.setFirstName("Existing");
        existingUser.setLastName("User");

        UserType existingUserType = UserType.EXTERNAL_SINGLE_FIRM;

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("user", existingUser);
        testSession.setAttribute("selectedUserType", existingUserType);

        // When
        String view = userController.createUser(new UserDetailsForm(), testSession, model);

        // Then
        assertThat(view).isEqualTo("add-user-details");
        assertThat(model.getAttribute("selectedUserType")).isEqualTo(existingUserType);
        UserDetailsForm form = (UserDetailsForm) model.getAttribute("userDetailsForm");
        assertThat(form.getFirstName()).isEqualTo("Existing");
        assertThat(form.getLastName()).isEqualTo("User");
    }

    @Test
    void postUser_shouldHandleEmailValidationWithExistingUser() {
        // Given
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setEmail("existing@example.com");
        userDetailsForm.setFirstName("Test");
        userDetailsForm.setLastName("User");

        EntraUserDto existingUser = new EntraUserDto();
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("user", existingUser);

        when(userService.userExistsByEmail("existing@example.com")).thenReturn(true);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        // When
        userController.postUser(userDetailsForm, bindingResult, testSession, model);

        // Then
        verify(bindingResult).rejectValue("email", "error.email", "Email address already exists");
        EntraUserDto sessionUser = (EntraUserDto) testSession.getAttribute("user");
        assertThat(sessionUser.getFirstName()).isEqualTo("Test");
        assertThat(sessionUser.getLastName()).isEqualTo("User");
        assertThat(sessionUser.getFullName()).isEqualTo("Test User");
        assertThat(sessionUser.getEmail()).isEqualTo("existing@example.com");
    }

    @Test
    void offices_shouldHandleExistingOfficeDataInSession() {
        // Given
        UUID firmId = UUID.randomUUID();
        UUID office1Id = UUID.randomUUID();
        UUID office2Id = UUID.randomUUID();

        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        final Office office1 = Office.builder().id(office1Id).address(address).build();
        final Office office2 = Office.builder().id(office2Id).address(address).build();

        OfficeData existingOfficeData = new OfficeData();
        existingOfficeData.setSelectedOffices(List.of(office1Id.toString()));

        FirmDto selectedFirm = new FirmDto();
        selectedFirm.setId(firmId);

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("officeData", existingOfficeData);
        testSession.setAttribute("user", new EntraUserDto());
        testSession.setAttribute("firm", selectedFirm);

        when(officeService.getOfficesByFirms(List.of(firmId))).thenReturn(List.of(office1, office2));

        // When
        String view = userController.offices(new OfficesForm(), testSession, model);

        // Then
        assertThat(view).isEqualTo("add-user-offices");
        @SuppressWarnings("unchecked")
        List<OfficeModel> officeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(officeData).hasSize(2);
        assertThat(officeData.get(0).isSelected()).isTrue();
        assertThat(officeData.get(1).isSelected()).isFalse();
    }

    @Test
    void setSelectedRoles_shouldHandleLastAppInMultiAppScenario() {
        // Given
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("finalRole"));

        MockHttpSession testSession = new MockHttpSession();
        List<String> selectedApps = List.of("app1", "app2");
        testSession.setAttribute("apps", selectedApps);

        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("createUserRolesSelectedAppIndex", 1); // Last app index
        testSession.setAttribute("userCreateRolesModel", sessionModel);

        // Existing roles for previous apps
        Map<Integer, List<String>> existingRoles = new HashMap<>();
        existingRoles.put(0, List.of("role1", "role2"));
        testSession.setAttribute("createUserAllSelectedRoles", existingRoles);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        String view = userController.setSelectedRoles(rolesForm, bindingResult, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/user/create/offices");
        @SuppressWarnings("unchecked")
        List<String> allRoles = (List<String>) testSession.getAttribute("roles");
        assertThat(allRoles).containsExactlyInAnyOrder("role1", "role2", "finalRole");
        assertThat(testSession.getAttribute("userCreateRolesModel")).isNull();
        assertThat(testSession.getAttribute("createUserAllSelectedRoles")).isNull();
    }

    @Test
    void displayAllUsers_shouldHandleNullSuccessMessage() {
        // Given
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setUsers(new ArrayList<>());

        when(loginService.getCurrentEntraUser(authentication)).thenReturn(EntraUser.builder().build());
        when(session.getAttribute("successMessage")).thenReturn(null);

        // When
        String view = userController.displayAllUsers(10, 1, null, null, null, null, false, model, session, authentication);

        // Then
        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("successMessage")).isNull();
        verify(session, Mockito.never()).removeAttribute("successMessage");
    }

    @Test
    void manageUser_shouldHandleNullUserAppRoles() {
        // Given
        String userId = "user123";
        EntraUserDto user = new EntraUserDto();
        user.setId(userId);

        UserProfileDto userProfile = UserProfileDto.builder()
                .id(UUID.randomUUID()) // Add ID to prevent null pointer
                .entraUser(user)
                .appRoles(List.of()) // Empty list instead of null
                .offices(List.of()) // Empty list instead of null
                .build();
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));

        // When
        String view = userController.manageUser(userId, model);

        // Then
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(userProfile); // Controller adds UserProfileDto, not EntraUserDto
        assertThat(model.getAttribute("userAppRoles")).isNotNull(); // Will be empty list, not null
        assertThat(model.getAttribute("userOffices")).isNotNull(); // Will be empty list, not null
    }


    @Test
    void addUserCreated_shouldRemoveUserFromSession() {
        // Given
        EntraUserDto user = new EntraUserDto();
        user.setFirstName("Test");
        user.setLastName("User");

        UserProfileDto userProfile = UserProfileDto.builder().id(UUID.randomUUID()).build();

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("user", user);
        testSession.setAttribute("userProfile", userProfile);

        // When
        String view = userController.addUserCreated(model, testSession);

        // Then
        assertThat(view).isEqualTo("add-user-created");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(testSession.getAttribute("user")).isNull();
        assertThat(testSession.getAttribute("userProfile")).isNull();
        assertThat(model.getAttribute("userProfile")).isNotNull();
    }

    @Test
    void editUserOffices_shouldHandlePartialOfficeAccess() {
        // Given
        String userId = "user123";
        EntraUserDto user = new EntraUserDto();
        user.setId(userId);

        UUID office1Id = UUID.randomUUID();
        UUID office2Id = UUID.randomUUID();
        UUID office3Id = UUID.randomUUID();

        Office.Address address = Office.Address.builder().addressLine1("addressLine1").city("city").postcode("pst_code").build();
        Office office1 = Office.builder().id(office1Id).address(address).build();
        Office office2 = Office.builder().id(office2Id).address(address).build();
        Office office3 = Office.builder().id(office3Id).address(address).build();
        OfficeDto office1Dto = OfficeDto.builder().id(office1.getId())
                .address(OfficeDto.AddressDto.builder().addressLine1(address.getAddressLine1())
                        .city(address.getCity()).postcode(address.getPostcode()).build()).build();
        OfficeDto office3Dto = OfficeDto.builder().id(office3.getId())
                .address(OfficeDto.AddressDto.builder().addressLine1(address.getAddressLine1())
                        .city(address.getCity()).postcode(address.getPostcode()).build()).build();

        List<OfficeDto> userOffices = List.of(office1Dto, office3Dto); // User has access to 2 out of 3 offices
        List<Office> allOffices = List.of(office1, office2, office3);

        // Mock user firms for the new firmService call
        FirmDto firmDto = FirmDto.builder().id(UUID.randomUUID()).build();
        List<FirmDto> userFirms = List.of(firmDto);

        UserProfileDto userProfile = UserProfileDto.builder()
                .entraUser(user)
                .build();
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfile));
        when(firmService.getUserFirmsByUserId(userId)).thenReturn(userFirms);
        when(officeService.getOfficesByFirms(anyList())).thenReturn(allOffices);
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);
        MockHttpSession testSession = new MockHttpSession();

        // When
        String view = userController.editUserOffices(userId, model, testSession);

        // Then
        assertThat(view).isEqualTo("edit-user-offices");
        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(false);

        @SuppressWarnings("unchecked")
        List<OfficeModel> officeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(officeData).hasSize(3);
        assertThat(officeData.get(0).isSelected()).isTrue(); // office1
        assertThat(officeData.get(1).isSelected()).isFalse(); // office2
        assertThat(officeData.get(2).isSelected()).isTrue(); // office3

        OfficesForm officesForm = (OfficesForm) model.getAttribute("officesForm");
        assertThat(officesForm.getOffices()).containsExactlyInAnyOrder(
                office1Id.toString(), office3Id.toString());
    }

    @Test
    void whenHandleException_thenRedirectToErrorPage() {

        // Arrange & Act
        RedirectView result = userController.handleException(new Exception());

        // Assert
        assertThat(result.getUrl()).isEqualTo("/error");
    }

    // ===== GRANT ACCESS FLOW TESTS =====

    @Test
    void grantUserAccess_shouldRedirectToGrantAccessApps() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440000";

        // When
        String result = userController.grantUserAccess(userId);

        // Then
        assertThat(result).isEqualTo("redirect:/admin/users/grant-access/" + userId + "/apps");
    }

    @Test
    void grantAccessEditUserApps_shouldPopulateModelAndReturnView() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440001";
        UserProfileDto user = new UserProfileDto();
        user.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        user.setUserType(UserType.EXTERNAL_SINGLE_FIRM);

        AppDto app1 = new AppDto();
        app1.setId("app1");
        app1.setName("App 1");
        AppDto app2 = new AppDto();
        app2.setId("app2");
        app2.setName("App 2");

        Set<AppDto> userApps = Set.of(app1);
        List<AppDto> availableApps = List.of(app1, app2);

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(user));
        when(userService.getUserAppsByUserId(userId)).thenReturn(userApps);
        when(userService.getAppsByUserType(UserType.EXTERNAL_SINGLE_FIRM)).thenReturn(availableApps);

        // When
        String view = userController.grantAccessEditUserApps(userId, new ApplicationsForm(), model, new MockHttpSession());

        // Then
        assertThat(view).isEqualTo("grant-access-user-apps");
        assertThat(model.getAttribute("user")).isEqualTo(user);

        @SuppressWarnings("unchecked")
        List<AppDto> apps = (List<AppDto>) model.getAttribute("apps");
        assertThat(apps).hasSize(2);
        assertThat(apps.get(0).isSelected()).isTrue(); // app1 should be selected
        assertThat(apps.get(1).isSelected()).isFalse(); // app2 should not be selected
    }

    @Test
    void grantAccessSetSelectedApps_shouldRedirectToRolesWhenAppsSelected() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(List.of("app1", "app2"));
        MockHttpSession testSession = new MockHttpSession();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        String result = userController.grantAccessSetSelectedApps(userId, applicationsForm, bindingResult, authentication, model, testSession);

        // Then
        assertThat(result).isEqualTo("redirect:/admin/users/grant-access/" + userId + "/roles");
        assertThat(testSession.getAttribute("grantAccessSelectedApps")).isEqualTo(List.of("app1", "app2"));
    }

    @Test
    void grantAccessSetSelectedApps_shouldRedirectToManageUserWhenNoAppsSelected() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440000";
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(null); // No apps selected
        final MockHttpSession testSession = new MockHttpSession();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        UserProfileDto userProfileDto = new UserProfileDto();
        EntraUserDto entraUser = new EntraUserDto();
        userProfileDto.setEntraUser(entraUser);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("test user");

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfileDto));
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);

        // When
        String result = userController.grantAccessSetSelectedApps(userId, applicationsForm, bindingResult, authentication, model, testSession);

        // Then
        assertThat(result).isEqualTo("redirect:/admin/users/manage/" + userId);
        verify(userService).updateUserRoles(userId, new ArrayList<>());
        verify(eventService).logEvent(any(UpdateUserAuditEvent.class));
    }

    @Test
    void grantAccessSetSelectedApps_shouldReturnToFormOnValidationErrors() {
        // Given
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(null); // This will trigger validation error
        
        // Mock session model with apps data
        UserProfileDto user = new UserProfileDto();
        user.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        
        AppDto app1 = new AppDto();
        app1.setId("app1");
        app1.setName("App 1");
        List<AppDto> apps = List.of(app1);
        
        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("user", user);
        sessionModel.addAttribute("apps", apps);
        
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("grantAccessUserAppsModel", sessionModel);
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        String result = userController.grantAccessSetSelectedApps(userId, applicationsForm, bindingResult, authentication, model, testSession);

        // Then
        assertThat(result).isEqualTo("grant-access-user-apps");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("apps")).isEqualTo(apps);
    }

    @Test
    void grantAccessSetSelectedApps_shouldRedirectWhenValidationErrorsAndNoSessionModel() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440000";
        ApplicationsForm applicationsForm = new ApplicationsForm();
        applicationsForm.setApps(null); // This will trigger validation error
        
        MockHttpSession testSession = new MockHttpSession();
        // No session model present
        
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String result = userController.grantAccessSetSelectedApps(userId, applicationsForm, bindingResult, authentication, model, testSession);

        // Then
        assertThat(result).isEqualTo("redirect:/admin/users/grant-access/" + userId + "/apps");
    }

    @Test
    void grantAccessEditUserRoles_shouldPopulateModelAndReturnView() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440002";
        UserProfileDto user = new UserProfileDto();
        user.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        AppDto currentApp = new AppDto();
        currentApp.setId("app1");
        currentApp.setName("App 1");

        AppRoleDto role1 = new AppRoleDto();
        role1.setId("role1");
        role1.setName("Role 1");
        final List<AppRoleDto> roles = List.of(role1);

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("grantAccessSelectedApps", List.of("app1"));

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(user));
        when(userService.getAppByAppId("app1")).thenReturn(Optional.of(currentApp));
        when(userService.getAppRolesByAppIdAndUserType(eq("app1"), any())).thenReturn(roles);
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(List.of());

        // When
        String view = userController.grantAccessEditUserRoles(userId, 0, new RolesForm(), model, testSession);

        // Then
        assertThat(view).isEqualTo("grant-access-user-roles");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("grantAccessSelectedAppIndex")).isEqualTo(0);
        assertThat(model.getAttribute("grantAccessCurrentApp")).isEqualTo(currentApp);
        assertThat(testSession.getAttribute("grantAccessUserRolesModel")).isNotNull();
    }

    @Test
    void grantAccessEditUserRoles_shouldRedirectToManageUserWhenNoAppsAssigned() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440003";
        UserProfileDto user = new UserProfileDto();
        user.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        MockHttpSession testSession = new MockHttpSession();
        // No apps in session, and user has no apps

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(user));
        when(userService.getUserAppsByUserId(userId)).thenReturn(Set.of()); // No apps

        // When
        String view = userController.grantAccessEditUserRoles(userId, 0, new RolesForm(), model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
    }

    @Test
    void grantAccessUpdateUserRoles_shouldRedirectToNextAppWhenNotLastApp() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440000";
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("role1"));

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("grantAccessSelectedApps", List.of("app1", "app2"));

        Model sessionModel = new ExtendedModelMap();
        sessionModel.addAttribute("grantAccessSelectedAppIndex", 0);
        testSession.setAttribute("grantAccessUserRolesModel", sessionModel);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        // When
        String view = userController.grantAccessUpdateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model, testSession);

        // Then
        assertThat(view).startsWith("redirect:/admin/users/grant-access/");
        assertThat(view).contains("/roles?selectedAppIndex=1");

        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> allRoles = (Map<Integer, List<String>>) testSession.getAttribute("grantAccessAllSelectedRoles");
        assertThat(allRoles).isNotNull();
        assertThat(allRoles.get(0)).containsExactly("role1");
    }

    @Test
    void grantAccessUpdateUserRoles_shouldCompleteRolesAndRedirectToOfficesOnLastApp() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440004";
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(List.of("role2"));

        UserProfileDto user = new UserProfileDto();
        EntraUserDto entraUser = new EntraUserDto();
        user.setEntraUser(entraUser);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("test user");

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("grantAccessSelectedApps", List.of("app1", "app2"));

        // Existing roles for previous app
        Map<Integer, List<String>> existingRoles = new HashMap<>();
        existingRoles.put(0, List.of("role1"));
        testSession.setAttribute("grantAccessAllSelectedRoles", existingRoles);

        Model sessionModel = new ExtendedModelMap();
        testSession.setAttribute("grantAccessUserRolesModel", sessionModel);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(user));
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);

        // When - updating roles for last app (index 1)
        String view = userController.grantAccessUpdateUserRoles(userId, rolesForm, bindingResult, 1, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/grant-access/" + userId + "/offices");
        verify(userService).updateUserRoles(userId, List.of("role1", "role2"));
        verify(eventService).logEvent(any(UpdateUserAuditEvent.class));
        assertThat(testSession.getAttribute("grantAccessUserRolesModel")).isNull();
        assertThat(testSession.getAttribute("grantAccessAllSelectedRoles")).isNull();
    }

    @Test
    void grantAccessUpdateUserRoles_shouldReturnToFormOnValidationErrors() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440005";
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(null); // Validation error

        Model sessionModel = new ExtendedModelMap();
        AppRoleViewModel role1 = new AppRoleViewModel();
        role1.setId("role1");
        role1.setSelected(true);
        sessionModel.addAttribute("roles", List.of(role1));
        sessionModel.addAttribute("user", new UserProfileDto());
        sessionModel.addAttribute("grantAccessSelectedAppIndex", 0);
        sessionModel.addAttribute("grantAccessCurrentApp", new AppDto());

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("grantAccessUserRolesModel", sessionModel);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String view = userController.grantAccessUpdateUserRoles(userId, rolesForm, bindingResult, 0, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("grant-access-user-roles");
        @SuppressWarnings("unchecked")
        List<AppRoleViewModel> roles = (List<AppRoleViewModel>) model.getAttribute("roles");
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).isSelected()).isFalse(); // Should be deselected due to validation error
    }

    @Test
    void grantAccessEditUserOffices_shouldPopulateModelAndReturnView() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440006";
        UserProfileDto user = new UserProfileDto();
        user.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        UUID office1Id = UUID.randomUUID();
        UUID office2Id = UUID.randomUUID();
        Office office1 = Office.builder().id(office1Id).code("Office 1")
                .address(Office.Address.builder().addressLine1("Address 1").build()).build();
        Office office2 = Office.builder().id(office2Id).code("Office 2")
                .address(Office.Address.builder().addressLine1("Address 2").build()).build();
        OfficeDto office1Dto = OfficeDto.builder().id(office1.getId())
                .address(OfficeDto.AddressDto.builder().addressLine1(office1.getAddress().getAddressLine1())
                        .build()).build();

        List<OfficeDto> userOffices = List.of(office1Dto); // User has access to office1 only
        List<Office> allOffices = List.of(office1, office2);

        FirmDto firmDto = FirmDto.builder().id(UUID.randomUUID()).build();
        List<FirmDto> userFirms = List.of(firmDto);

        MockHttpSession testSession = new MockHttpSession();

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(user));
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);
        when(firmService.getUserFirmsByUserId(userId)).thenReturn(userFirms);
        when(officeService.getOfficesByFirms(anyList())).thenReturn(allOffices);

        // When
        String view = userController.grantAccessEditUserOffices(userId, model, testSession);

        // Then
        assertThat(view).isEqualTo("grant-access-user-offices");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(false);

        @SuppressWarnings("unchecked")
        List<OfficeModel> officeData = (List<OfficeModel>) model.getAttribute("officeData");
        assertThat(officeData).hasSize(2);
        assertThat(officeData.get(0).isSelected()).isTrue(); // office1 should be selected
        assertThat(officeData.get(1).isSelected()).isFalse(); // office2 should not be selected

        OfficesForm form = (OfficesForm) model.getAttribute("officesForm");
        assertThat(form.getOffices()).contains(office1Id.toString());
        assertThat(testSession.getAttribute("grantAccessUserOfficesModel")).isNotNull();
    }

    @Test
    void grantAccessEditUserOffices_shouldHandleAccessToAllOffices() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440007";
        UserProfileDto user = new UserProfileDto();
        user.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        UUID office1Id = UUID.randomUUID();
        UUID office2Id = UUID.randomUUID();
        Office office1 = Office.builder().id(office1Id).code("Office 1")
                .address(Office.Address.builder().addressLine1("Address 1").build()).build();
        Office office2 = Office.builder().id(office2Id).code("Office 2")
                .address(Office.Address.builder().addressLine1("Address 2").build()).build();
        OfficeDto office1Dto = OfficeDto.builder().id(office1.getId())
                .address(OfficeDto.AddressDto.builder().addressLine1(office1.getAddress().getAddressLine1()).build()).build();
        OfficeDto office2Dto = OfficeDto.builder().id(office2.getId())
                .address(OfficeDto.AddressDto.builder().addressLine1(office2.getAddress().getAddressLine1()).build()).build();

        List<OfficeDto> userOffices = List.of(office1Dto, office2Dto); // User has access to all offices
        List<Office> allOffices = List.of(office1, office2);

        FirmDto firmDto = FirmDto.builder().id(UUID.randomUUID()).build();
        final List<FirmDto> userFirms = List.of(firmDto);

        MockHttpSession testSession = new MockHttpSession();

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(user));
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);
        when(firmService.getUserFirmsByUserId(userId)).thenReturn(userFirms);
        when(officeService.getOfficesByFirms(anyList())).thenReturn(allOffices);

        // When
        String view = userController.grantAccessEditUserOffices(userId, model, testSession);

        // Then
        assertThat(view).isEqualTo("grant-access-user-offices");
        assertThat(model.getAttribute("hasAllOffices")).isEqualTo(true);

        OfficesForm form = (OfficesForm) model.getAttribute("officesForm");
        assertThat(form.getOffices()).contains("ALL");
    }

    @Test
    void grantAccessUpdateUserOffices_shouldUpdateOfficesAndRedirectToCheckAnswers() throws IOException {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440008";
        OfficesForm officesForm = new OfficesForm();
        officesForm.setOffices(List.of("office1", "office2"));

        UserProfileDto userProfileDto = new UserProfileDto();
        EntraUserDto entraUser = new EntraUserDto();
        userProfileDto.setEntraUser(entraUser);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("test user");

        MockHttpSession testSession = new MockHttpSession();

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfileDto));
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);

        // When
        String view = userController.grantAccessUpdateUserOffices(userId, officesForm, bindingResult, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/grant-access/" + userId + "/check-answers");
        verify(userService).updateUserOffices(userId, List.of("office1", "office2"));
        verify(eventService).logEvent(any(UpdateUserAuditEvent.class));

        // Verify session cleanup
        assertThat(testSession.getAttribute("grantAccessUserOfficesModel")).isNull();
        assertThat(testSession.getAttribute("grantAccessSelectedApps")).isNull();
        assertThat(testSession.getAttribute("grantAccessUserRoles")).isNull();
        assertThat(testSession.getAttribute("grantAccessUserRolesModel")).isNull();
        assertThat(testSession.getAttribute("grantAccessAllSelectedRoles")).isNull();
    }

    @Test
    void grantAccessUpdateUserOffices_shouldHandleAllOfficesSelection() throws IOException {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440009";
        OfficesForm officesForm = new OfficesForm();
        officesForm.setOffices(List.of("ALL")); // Special "ALL" value

        UserProfileDto userProfileDto = new UserProfileDto();
        EntraUserDto entraUser = new EntraUserDto();
        userProfileDto.setEntraUser(entraUser);
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("test user");

        MockHttpSession testSession = new MockHttpSession();

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfileDto));
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);

        // When
        String view = userController.grantAccessUpdateUserOffices(userId, officesForm, bindingResult, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/grant-access/" + userId + "/check-answers");

        // Should pass 'ALL' to service when option is selected
        List<String> expectedOfficeIds = List.of("ALL");
        verify(userService).updateUserOffices(userId, expectedOfficeIds);
        verify(eventService).logEvent(any(UpdateUserAuditEvent.class));
    }

    @Test
    void grantAccessUpdateUserOffices_shouldReturnToFormOnValidationErrors() throws IOException {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440010";
        OfficesForm officesForm = new OfficesForm();
        officesForm.setOffices(null); // Validation error

        Model sessionModel = new ExtendedModelMap();
        OfficeModel office1 = new OfficeModel("Office 1",
                OfficeModel.Address.builder().addressLine1("Address 1").build(), "office1", true);
        sessionModel.addAttribute("user", new UserProfileDto());
        sessionModel.addAttribute("officeData", List.of(office1));

        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("grantAccessUserOfficesModel", sessionModel);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String view = userController.grantAccessUpdateUserOffices(userId, officesForm, bindingResult, authentication, model, testSession);

        // Then
        assertThat(view).isEqualTo("grant-access-user-offices");
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("officeData")).isNotNull();
        verify(userService, Mockito.never()).updateUserOffices(anyString(), anyList());
    }

    @Test
    void grantAccessCheckAnswers_shouldPopulateModelAndReturnView() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440011";
        UserProfileDto user = new UserProfileDto();
        user.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        AppRoleDto appRole = new AppRoleDto();
        appRole.setId("role1");
        appRole.setName("Role 1");
        List<AppRoleDto> userAppRoles = List.of(appRole);

        Office office = Office.builder().id(UUID.randomUUID()).code("Office 1").build();
        OfficeDto officeDto = OfficeDto.builder().id(office.getId()).code(office.getCode()).build();
        List<OfficeDto> userOffices = List.of(officeDto);

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(user));
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(userAppRoles);
        when(userService.getUserOfficesByUserId(userId)).thenReturn(userOffices);

        // When
        String view = userController.grantAccessCheckAnswers(userId, model);

        // Then
        assertThat(view).isEqualTo("grant-access-check-answers");
        assertThat(model.getAttribute("user")).isEqualTo(user);
        assertThat(model.getAttribute("userAppRoles")).isEqualTo(userAppRoles);
        assertThat(model.getAttribute("userOffices")).isEqualTo(userOffices);
    }

    @Test
    void grantAccessProcessCheckAnswers_shouldCompleteGrantAccessAndRedirectToConfirmation() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440012";
        UserProfileDto userProfileDto = new UserProfileDto();
        EntraUserDto entraUser = new EntraUserDto();
        entraUser.setFullName("Test User");
        userProfileDto.setEntraUser(entraUser);

        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        currentUserDto.setName("admin user");

        MockHttpSession testSession = new MockHttpSession();

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(userProfileDto));
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);

        // When
        String view = userController.grantAccessProcessCheckAnswers(userId, authentication, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/grant-access/" + userId + "/confirmation");
        verify(userService).grantAccess(userId, currentUserDto.getName());
        verify(eventService).logEvent(any(UpdateUserAuditEvent.class));

        // Verify session cleanup
        assertThat(testSession.getAttribute("grantAccessUserOfficesModel")).isNull();
        assertThat(testSession.getAttribute("grantAccessSelectedApps")).isNull();
        assertThat(testSession.getAttribute("grantAccessUserRoles")).isNull();
        assertThat(testSession.getAttribute("grantAccessUserRolesModel")).isNull();
        assertThat(testSession.getAttribute("grantAccessAllSelectedRoles")).isNull();
    }

    @Test
    void grantAccessConfirmation_shouldPopulateModelAndReturnView() {
        // Given
        final String userId = "550e8400-e29b-41d4-a716-446655440013";
        UserProfileDto user = new UserProfileDto();
        user.setId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        when(userService.getUserProfileById(userId)).thenReturn(Optional.of(user));

        // When
        String view = userController.grantAccessConfirmation(userId, model);

        // Then
        assertThat(view).isEqualTo("grant-access-confirmation");
        assertThat(model.getAttribute("user")).isEqualTo(user);
    }

    @Test
    void cancelGrantAccess_shouldClearSessionAndRedirectToManageUser() {
        // Given
        String userId = "550e8400-e29b-41d4-a716-446655440014";
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("grantAccessSelectedApps", List.of("app1"));
        testSession.setAttribute("grantAccessUserRoles", List.of("role1"));
        testSession.setAttribute("grantAccessUserRolesModel", new ExtendedModelMap());
        testSession.setAttribute("grantAccessAllSelectedRoles", new HashMap<>());
        testSession.setAttribute("grantAccessUserOfficesModel", new ExtendedModelMap());
        testSession.setAttribute("successMessage", "test message");

        // When
        String view = userController.cancelGrantAccess(userId, testSession);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/users/manage/" + userId);
        assertThat(testSession.getAttribute("grantAccessSelectedApps")).isNull();
        assertThat(testSession.getAttribute("grantAccessUserRoles")).isNull();
        assertThat(testSession.getAttribute("grantAccessUserRolesModel")).isNull();
        assertThat(testSession.getAttribute("grantAccessAllSelectedRoles")).isNull();
        assertThat(testSession.getAttribute("grantAccessUserOfficesModel")).isNull();
        assertThat(testSession.getAttribute("successMessage")).isNull();
    }

    @Test
    void testCreateUserFirm_ShouldReturnAddUserFirmView() {
        // Given
        FirmSearchForm firmSearchForm = new FirmSearchForm();
        MockHttpSession testSession = new MockHttpSession();
        Model testModel = new ExtendedModelMap();

        // When
        String view = userController.createUserFirm(firmSearchForm, testSession, testModel);

        // Then
        assertThat(view).isEqualTo("add-user-firm");
        assertThat(testModel.getAttribute("firmSearchForm")).isEqualTo(firmSearchForm);
        assertThat(testSession.getAttribute("firm")).isNull();
        assertThat(testSession.getAttribute("selectedFirm")).isNull();
    }

    @Test
    void testCreateUserFirm_WithExistingFormInSession_ShouldUseExistingForm() {
        // Given
        FirmSearchForm existingForm = new FirmSearchForm();
        existingForm.setFirmSearch("Existing Firm");
        existingForm.setSelectedFirmId("existing-id");

        FirmSearchForm newForm = new FirmSearchForm();
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("firmSearchForm", existingForm);
        Model testModel = new ExtendedModelMap();

        // When
        String view = userController.createUserFirm(newForm, testSession, testModel);

        // Then
        assertThat(view).isEqualTo("add-user-firm");
        assertThat(testModel.getAttribute("firmSearchForm")).isEqualTo(existingForm);
        assertThat(testSession.getAttribute("firmSearchForm")).isNull(); // Should be removed after use
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
                        .build()
        );

        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = userController.searchFirms(query);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("name")).isEqualTo("Test Firm 1");
        assertThat(result.get(0).get("code")).isEqualTo("TF001");
        assertThat(result.get(1).get("name")).isEqualTo("Test Firm 2");
        assertThat(result.get(1).get("code")).isEqualTo("TF002");
        verify(firmService).searchFirms(query);
    }

    @Test
    void testSearchFirms_WithEmptyQuery_ShouldReturnAllFirms() {
        // Given
        String query = "";
        List<FirmDto> mockFirms = List.of(
                FirmDto.builder()
                        .id(UUID.randomUUID())
                        .name("All Firms Test")
                        .code("AFT001")
                        .build()
        );

        when(firmService.searchFirms(query)).thenReturn(mockFirms);

        // When
        List<Map<String, String>> result = userController.searchFirms(query);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("All Firms Test");
        assertThat(result.get(0).get("code")).isEqualTo("AFT001");
        verify(firmService).searchFirms(query);
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
        List<Map<String, String>> result = userController.searchFirms(query);

        // Then
        assertThat(result).hasSize(10); // Should be limited to 10 results
        verify(firmService).searchFirms(query);
    }

    @Test
    void testPostUserFirm_WithValidSelectedFirmId_ShouldRedirectToCheckAnswers() {
        // Given
        FirmSearchForm firmSearchForm = new FirmSearchForm();
        firmSearchForm.setSelectedFirmId(UUID.randomUUID().toString());
        firmSearchForm.setFirmSearch("Test Firm");

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        MockHttpSession testSession = new MockHttpSession();
        Model testModel = new ExtendedModelMap();

        FirmDto mockFirm = FirmDto.builder()
                .id(UUID.fromString(firmSearchForm.getSelectedFirmId()))
                .name("Test Firm")
                .code("TF001")
                .build();

        when(firmService.getFirm(firmSearchForm.getSelectedFirmId())).thenReturn(mockFirm);

        // When
        String view = userController.postUserFirm(firmSearchForm, bindingResult, testSession, testModel);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
        assertThat(testSession.getAttribute("firm")).isEqualTo(mockFirm);
        assertThat(testSession.getAttribute("firmSearchTerm")).isEqualTo("Test Firm");
        verify(firmService).getFirm(firmSearchForm.getSelectedFirmId());
    }

    @Test
    void testPostUserFirm_WithValidationErrors_ShouldReturnToFirmPage() {
        // Given
        FirmSearchForm firmSearchForm = new FirmSearchForm();
        firmSearchForm.setFirmSearch("Test Firm");

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(true);

        MockHttpSession testSession = new MockHttpSession();
        Model testModel = new ExtendedModelMap();

        // When
        String view = userController.postUserFirm(firmSearchForm, bindingResult, testSession, testModel);

        // Then
        assertThat(view).isEqualTo("add-user-firm");
        assertThat(testSession.getAttribute("firmSearchForm")).isEqualTo(firmSearchForm);
    }

    @Test
    void testPostUserFirm_WithInvalidFirmId_ShouldReturnErrorToFirmPage() {
        // Given
        FirmSearchForm firmSearchForm = new FirmSearchForm();
        firmSearchForm.setSelectedFirmId("invalid-uuid");
        firmSearchForm.setFirmSearch("Test Firm");

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        MockHttpSession testSession = new MockHttpSession();
        Model testModel = new ExtendedModelMap();

        // No need to stub firmService.getFirm() since UUID.fromString() will throw first

        // When
        String view = userController.postUserFirm(firmSearchForm, bindingResult, testSession, testModel);

        // Then
        assertThat(view).isEqualTo("add-user-firm");
        verify(bindingResult).rejectValue("firmSearch", "error.firm", "Invalid firm selection. Please try again.");
    }

    @Test
    void testPostUserFirm_WithNoSelectedFirmId_FallbackToNameSearch_Success() {
        // Given
        FirmSearchForm firmSearchForm = new FirmSearchForm();
        firmSearchForm.setFirmSearch("Test Firm");
        firmSearchForm.setSelectedFirmId(null);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        MockHttpSession testSession = new MockHttpSession();
        Model testModel = new ExtendedModelMap();

        List<FirmDto> mockFirms = List.of(
                FirmDto.builder()
                        .id(UUID.randomUUID())
                        .name("Test Firm")
                        .code("TF001")
                        .build()
        );

        when(firmService.getFirms()).thenReturn(mockFirms);

        // When
        String view = userController.postUserFirm(firmSearchForm, bindingResult, testSession, testModel);

        // Then
        assertThat(view).isEqualTo("redirect:/admin/user/create/check-answers");
        assertThat(testSession.getAttribute("firm")).isEqualTo(mockFirms.get(0));
        assertThat(testSession.getAttribute("firmSearchTerm")).isEqualTo("Test Firm");
        verify(firmService).getFirms();
    }

    @Test
    void testPostUserFirm_WithNoSelectedFirmId_FallbackToNameSearch_NotFound() {
        // Given
        FirmSearchForm firmSearchForm = new FirmSearchForm();
        firmSearchForm.setFirmSearch("Nonexistent Firm");
        firmSearchForm.setSelectedFirmId(null);

        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(false);

        MockHttpSession testSession = new MockHttpSession();
        Model testModel = new ExtendedModelMap();

        List<FirmDto> mockFirms = List.of(
                FirmDto.builder()
                        .id(UUID.randomUUID())
                        .name("Different Firm")
                        .code("DF001")
                        .build()
        );

        when(firmService.getFirms()).thenReturn(mockFirms);

        // When
        String view = userController.postUserFirm(firmSearchForm, bindingResult, testSession, testModel);

        // Then
        assertThat(view).isEqualTo("add-user-firm");
        verify(bindingResult).rejectValue("firmSearch", "error.firm", "No firm found with that name. Please select from the dropdown.");
        verify(firmService).getFirms();
    }
    
    @Test
    void has_accessControl() throws NoSuchMethodException {
        Class clazz = UserController.class;
        List<String> canEditMethods = List.of("editUser",
                "editUserApps", "setSelectedAppsEdit",
                "editUserRoles", "updateUserRoles");
        List<String> canAcessMethods = List.of("manageUser");
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            if (canEditMethods.contains(method.getName())) {
                PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
                assertThat(anno.value()).isEqualTo("@accessControlService.canEditUser(#id)");
                continue;
            }
            if (canAcessMethods.contains(method.getName())) {
                PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
                assertThat(anno.value()).isEqualTo("@accessControlService.canAccessUser(#id)");
                continue;
            }
            // Methods with more complicated permissions
            if ("editUserDetails".equals(method.getName())) {
                PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
                assertThat(anno.value()).isEqualTo("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_DETAILS)"
                        + " && @accessControlService.canEditUser(#id)");
            }
            if ("updateUserDetails".equals(method.getName())) {
                PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
                assertThat(anno.value()).isEqualTo("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_DETAILS)"
                        + " && @accessControlService.canEditUser(#id)");
            }
            if ("editUserOffices".equals(method.getName())) {
                PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
                assertThat(anno.value()).isEqualTo("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_OFFICE)"
                       + " && @accessControlService.canEditUser(#id)");
            }
            if ("updateUserOffices".equals(method.getName())) {
                PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
                assertThat(anno.value()).isEqualTo("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_OFFICE)"
                        + " && @accessControlService.canEditUser(#id)");
            }
        }
    }
}