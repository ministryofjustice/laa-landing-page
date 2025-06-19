package uk.gov.justice.laa.portal.landingpage.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.microsoft.graph.models.User;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
import uk.gov.justice.laa.portal.landingpage.forms.OfficesForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserModel;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.LogMonitoring;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private UserController userController;

    @Mock
    private UserService userService;
    @Mock
    private OfficeService officeService;
    @Mock
    private FirmService firmService;
    @Mock
    private HttpSession session;

    private Model model;

    @BeforeEach
    void setUp() {
        userController = new UserController(userService, officeService, new MapperConfig().modelMapper(), firmService);
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

        when(userService.getPageOfUsers(anyInt(), anyInt())).thenReturn(paginatedUsers);

        String view = userController.displayAllUsers(10, 1, null, model, session);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(paginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        assertThat(model.getAttribute("page")).isEqualTo(1);
        assertThat(model.getAttribute("totalUsers")).isEqualTo(100L);
        assertThat(model.getAttribute("totalPages")).isEqualTo(10);
    }

    @Test
    void editUser() {
        EntraUserDto user = new EntraUserDto();
        user.setFullName("Test User");

        when(userService.getEntraUserById(anyString())).thenReturn(Optional.of(user));

        String view = userController.editUser("userId", model);

        assertThat(view).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(user);
    }

    @Test
    void givenUsersExist_whenDisplayAllUsers_thenPopulatesModelAndReturnsUsersView() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(List.of(new EntraUserDto(), new EntraUserDto()));
        mockPaginatedUsers.setNextPageLink("nextLink123");
        mockPaginatedUsers.setPreviousPageLink("prevLink456");
        when(userService.getPageOfUsers(eq(1), eq(10))).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, null, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(mockPaginatedUsers.getUsers());
        assertThat(model.getAttribute("requestedPageSize")).isEqualTo(10);
        verify(userService).getPageOfUsers(1, 10);
    }

    @Test
    void givenNoUsers_whenDisplayAllUsers_thenReturnsEmptyListInModel() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        mockPaginatedUsers.setNextPageLink(null);
        mockPaginatedUsers.setPreviousPageLink(null);
        when(userService.getPageOfUsers(anyInt(), anyInt())).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, null, model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsers(1, 10);
    }

    @Test
    void testDisplayAllUsersSearchesUsersWhenSearchTermIsGiven() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        when(userService.getPageOfUsersByNameOrEmail(anyInt(), anyInt(), eq("Test"))).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, "Test", model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsersByNameOrEmail(1, 10, "Test");
    }

    @Test
    void testDisplayAllUsersDoesNotSearchUsersWhenSearchTermIsEmpty() {
        // Arrange
        PaginatedUsers mockPaginatedUsers = new PaginatedUsers();
        mockPaginatedUsers.setUsers(new ArrayList<>());
        when(userService.getPageOfUsers(anyInt(), anyInt())).thenReturn(mockPaginatedUsers);

        // Act
        String viewName = userController.displayAllUsers(10, 1, "", model, session);

        // Assert
        assertThat(viewName).isEqualTo("users");
        assertThat(model.getAttribute("users")).isEqualTo(new ArrayList<>());
        verify(userService).getPageOfUsers(1, 10);
    }

    @Test
    void givenValidUserId_whenEditUser_thenFetchesUserAndReturnsEditView() {

        // Arrange
        String userId = "user123";
        EntraUserDto mockUser = new EntraUserDto();
        mockUser.setId(userId);
        mockUser.setFullName("Test User");
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));

        // Act
        String viewName = userController.editUser(userId, model);

        // Assert
        assertThat(viewName).isEqualTo("edit-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        verify(userService).getEntraUserById(userId);
    }

    @Test
    void givenInvalidUserId_whenEditUser_thenReturnsEditViewWithErrorOrRedirect() {

        // Arrange
        String userId = "invalid-user";
        when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());

        // Act
        String viewName = userController.editUser(userId, model);

        verify(userService).getEntraUserById(userId);
    }

    @Test
    void displaySavedUsers() {

        when(userService.getSavedUsers()).thenReturn(new ArrayList<>());
        String view = userController.displaySavedUsers(model);

        assertThat(view).isEqualTo("users");
        assertThat(model.getAttribute("users")).isNotNull();

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
        EntraUserDto mockUser = new EntraUserDto();
        mockUser.setId(userId);
        mockUser.setFullName("Managed User");
        List<AppRoleDto> appRoles = List.of(new AppRoleDto());
        List<Office> offices = List.of(Office.builder().build());

        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(mockUser));
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(appRoles);
        when(officeService.getOffices()).thenReturn(offices);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isEqualTo(mockUser);
        assertThat(model.getAttribute("userAppRoles")).isEqualTo(appRoles);
        assertThat(model.getAttribute("offices")).isEqualTo(offices);
        verify(userService).getEntraUserById(userId);
    }

    @Test
    void manageUser_whenUserNotFound_shouldAddNullUserAndReturnManageUserView() {
        // Arrange
        String userId = "notfound";
        when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(null);

        // Act
        String view = userController.manageUser(userId, model);

        // Assert
        assertThat(view).isEqualTo("manage-user");
        assertThat(model.getAttribute("user")).isNull();
        assertThat(model.getAttribute("lastLoggedIn")).isNull();
        assertThat(model.getAttribute("appRoles")).isNull();
        verify(userService).getEntraUserById(userId);
        verify(userService).getUserAppRolesByUserId(userId);
    }

    @Test
    void createNewUser() {
        when(session.getAttribute("user")).thenReturn(null);
        when(session.getAttribute("firm")).thenReturn(null);
        FirmDto firm1 = FirmDto.builder().build();
        FirmDto firm2 = FirmDto.builder().build();
        when(firmService.getFirms()).thenReturn(List.of(firm1, firm2));
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        String view = userController.createUser(userDetailsForm, session, model);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(view).isEqualTo("add-user-details");
    }

    @Test
    void createUserFromSession() {
        User mockUser = new User();
        mockUser.setDisplayName("Test User");
        when(session.getAttribute("user")).thenReturn(mockUser);
        when(session.getAttribute("firm")).thenReturn(FirmDto.builder().name("Test firm").build());
        FirmDto firm1 = FirmDto.builder().build();
        FirmDto firm2 = FirmDto.builder().build();
        when(firmService.getFirms()).thenReturn(List.of(firm1, firm2));
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        String view = userController.createUser(userDetailsForm, session, model);
        assertThat(model.getAttribute("user")).isNotNull();
        assertThat(model.getAttribute("firms")).isNotNull();
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getDisplayName()).isEqualTo("Test User");
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        assertThat(selectedFirm.getName()).isEqualTo("Test firm");
        assertThat(view).isEqualTo("add-user-details");
    }

    @Test
    void postNewUser() {
        HttpSession session = new MockHttpSession();
        when(firmService.getFirm(anyString())).thenReturn(FirmDto.builder().name("Test Firm").build());
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("firstName");
        userDetailsForm.setLastName("lastName");
        userDetailsForm.setEmail("email");
        userDetailsForm.setFirmId("firmId");
        userDetailsForm.setIsFirmAdmin(false);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        String redirectUrl = userController.postUser(userDetailsForm, bindingResult, "firmId", session, model);
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getGivenName()).isEqualTo("firstName");
        assertThat(sessionUser.getSurname()).isEqualTo("lastName");
        assertThat(sessionUser.getDisplayName()).isEqualTo("firstName lastName");
        assertThat(sessionUser.getMail()).isEqualTo("email");
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        assertThat(selectedFirm.getName()).isEqualTo("Test Firm");
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/services");
        assertThat((Boolean) session.getAttribute("isFirmAdmin")).isFalse();
    }

    @Test
    void postSessionUser() {
        User mockUser = new User();
        mockUser.setDisplayName("Test User");
        HttpSession session = new MockHttpSession();
        session.setAttribute("user", mockUser);
        session.setAttribute("firm", FirmDto.builder().name("oldFirm").build());
        when(firmService.getFirm(eq("newFirm"))).thenReturn(FirmDto.builder().name("Test Firm").build());
        UserDetailsForm userDetailsForm = new UserDetailsForm();
        userDetailsForm.setFirstName("firstName");
        userDetailsForm.setLastName("lastName");
        userDetailsForm.setEmail("email");
        userDetailsForm.setFirmId("newFirm");
        userDetailsForm.setIsFirmAdmin(true);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        String redirectUrl = userController.postUser(userDetailsForm, bindingResult, "newFirm", session, model);
        User sessionUser = (User) session.getAttribute("user");
        assertThat(sessionUser.getGivenName()).isEqualTo("firstName");
        assertThat(sessionUser.getSurname()).isEqualTo("lastName");
        assertThat(sessionUser.getDisplayName()).isEqualTo("firstName lastName");
        assertThat(sessionUser.getMail()).isEqualTo("email");
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/services");
        boolean firmAdmin = (Boolean) session.getAttribute("isFirmAdmin");
        assertThat(firmAdmin).isTrue();
        String selectedFirmName = ((FirmDto) session.getAttribute("firm")).getName();
        assertThat(selectedFirmName).isEqualTo("Test Firm");
    }

    @Test
    void selectUserAppsGet() {
        AppDto app = new AppDto();
        app.setId("1");
        when(userService.getApps()).thenReturn(List.of(app));
        List<String> ids = List.of("1");
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", ids);
        ApplicationsForm applicationsForm = new ApplicationsForm();
        String view = userController.selectUserApps(applicationsForm, model, session);
        assertThat(view).isEqualTo("add-user-apps");
        assertThat(model.getAttribute("apps")).isNotNull();
        List<AppViewModel> modeApps = (List<AppViewModel>) model.getAttribute("apps");
        assertThat(modeApps.getFirst().getId()).isEqualTo("1");
        assertThat(modeApps.getFirst().isSelected()).isTrue();
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
        when(userService.getAllAvailableRolesForApps(eq(selectedApps))).thenReturn(roles);
        HttpSession session = new MockHttpSession();
        session.setAttribute("apps", selectedApps);
        session.setAttribute("roles", selectedRoles);
        String view = userController.getSelectedRoles(new RolesForm(), model, session);
        assertThat(view).isEqualTo("add-user-roles");
        assertThat(model.getAttribute("roles")).isNotNull();
        List<AppRoleViewModel> sessionRoles = (List<AppRoleViewModel>) model.getAttribute("roles");
        assertThat(sessionRoles.getFirst().isSelected()).isFalse();
        assertThat(sessionRoles.get(1).isSelected()).isTrue();
    }

    @Test
    void setSelectedRolesPost() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("1");
        RolesForm rolesForm = new RolesForm();
        rolesForm.setRoles(roles);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        Model model = new ExtendedModelMap();
        String redirectUrl = userController.setSelectedRoles(rolesForm, bindingResult, model, session);
        assertThat(session.getAttribute("roles")).isNotNull();
        assertThat(redirectUrl).isEqualTo("redirect:/admin/user/create/offices");
    }

    @Test
    void offices() {
        HttpSession session = new MockHttpSession();
        UUID officeId = UUID.randomUUID();
        OfficeData officeData = new OfficeData();
        officeData.setSelectedOffices(List.of(officeId.toString()));
        session.setAttribute("officeData", officeData);
        Office office1 = Office.builder().id(officeId).build();
        Office office2 = Office.builder().id(UUID.randomUUID()).build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeService.getOffices()).thenReturn(dbOffices);
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
        UUID officeId1 = UUID.randomUUID();
        UUID officeId2 = UUID.randomUUID();
        List<String> selectedOffices = List.of(officeId1.toString());
        Office office1 = Office.builder().id(officeId1).name("of1").build();
        Office office2 = Office.builder().id(officeId2).name("of2").build();
        List<Office> dbOffices = List.of(office1, office2);
        when(officeService.getOffices()).thenReturn(dbOffices);

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
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("app1-tester");
        AppDto app1 = new AppDto();
        app1.setId("app1");
        userRole.setApp(app1);
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("app1-dev");
        userRole2.setApp(app1);
        Mockito.lenient().when(userService.getAllAvailableRolesForApps(eq(selectedApps))).thenReturn(List.of(userRole, userRole2));
        List<String> selectedRoles = List.of("app1-dev");
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        String view = userController.addUserCheckAnswers(model, session);
        assertThat(view).isEqualTo("redirect:/admin/users");
    }

    @Test
    void addUserCheckAnswersGetFirmAdmin() {
        HttpSession session = new MockHttpSession();
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        AppRoleDto userRole = new AppRoleDto();
        userRole.setId("app1-tester");
        AppDto app1 = new AppDto();
        app1.setId("app1");
        userRole.setApp(app1);
        AppRoleDto userRole2 = new AppRoleDto();
        userRole2.setId("app1-dev");
        userRole2.setApp(app1);
        Mockito.lenient().when(userService.getAllAvailableRolesForApps(eq(selectedApps))).thenReturn(List.of(userRole, userRole2));
        List<String> selectedRoles = List.of("app1-dev");
        session.setAttribute("roles", selectedRoles);
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        session.setAttribute("isFirmAdmin", true);
        String view = userController.addUserCheckAnswers(model, session);
        assertThat(view).isEqualTo("redirect:/admin/users");
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
        session.setAttribute("user", new User());
        session.setAttribute("officeData", new OfficeData());
        session.setAttribute("firm", FirmDto.builder().build());
        String view = userController.addUserCheckAnswers(model, session);
        assertThat(view).isEqualTo("redirect:/admin/users");
    }

    @Test
    void addUserCheckAnswersPost() {
        HttpSession session = new MockHttpSession();
        User user = new User();
        session.setAttribute("user", user);
        List<String> roles = List.of("app1");
        session.setAttribute("roles", roles);
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        session.setAttribute("officeData", new OfficeData());
        session.setAttribute("firm", FirmDto.builder().id(UUID.randomUUID()).name("test firm").build());
        session.setAttribute("isFirmAdmin", false);
        when(userService.createUser(any(), any(), any(), any(), eq(false))).thenReturn(user);
        String redirectUrl = userController.addUserCheckAnswers(model, session);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/users");
        assertThat(session.getAttribute("roles")).isNull();
        assertThat(session.getAttribute("apps")).isNull();
        assertThat(session.getAttribute("officeData")).isNull();
        assertThat(session.getAttribute("firm")).isNull();
    }

    @Test
    void addUserCheckAnswersPost_NoUserProvided() {
        HttpSession session = new MockHttpSession();
        List<String> roles = List.of("app1");
        session.setAttribute("roles", roles);
        List<String> selectedApps = List.of("app1");
        session.setAttribute("apps", selectedApps);
        // Add list appender to logger to verify logs
        ListAppender<ILoggingEvent> listAppender = LogMonitoring.addListAppenderToLogger(UserController.class);
        String redirectUrl = userController.addUserCheckAnswers(model, session);
        assertThat(redirectUrl).isEqualTo("redirect:/admin/users");
        assertThat(model.getAttribute("roles")).isNull();
        assertThat(model.getAttribute("apps")).isNull();
        List<ILoggingEvent> logEvents = LogMonitoring.getLogsByLevel(listAppender, Level.ERROR);
        assertThat(logEvents).hasSize(1);
    }

    @Test
    void addUserCreated() {
        HttpSession session = new MockHttpSession();
        User user = new User();
        session.setAttribute("user", user);
        String view = userController.addUserCreated(model, session);
        assertThat(model.getAttribute("user")).isNotNull();
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
        EntraUserDto testUser = new EntraUserDto();
        when(userService.getEntraUserById(userId)).thenReturn(Optional.of(testUser));
        // Setup test user roles
        AppRoleDto testUserRole = new AppRoleDto();
        testUserRole.setId("testUserAppRoleId");
        List<AppRoleDto> testUserRoles = List.of(testUserRole);
        when(userService.getUserAppRolesByUserId(userId)).thenReturn(testUserRoles);
        // Setup all available roles
        AppRoleDto testRole1 = new AppRoleDto();
        testRole1.setId("testAppRoleId1");
        AppRoleDto testRole2 = new AppRoleDto();
        testRole2.setId("testAppRoleId2");
        AppRoleDto testRole3 = new AppRoleDto();
        testRole3.setId("testAppRoleId3");
        AppRoleDto testRole4 = new AppRoleDto();
        testRole3.setId("testUserAppRoleId");
        List<AppRoleDto> allRoles = List.of(testRole1, testRole2, testRole3, testRole4);
        when(userService.getAppRolesByAppIds(any())).thenReturn(allRoles);

        // When
        String view = userController.editUserRoles(userId, model, session);

        // Then
        Assertions.assertEquals("edit-user-roles", view);
        Assertions.assertSame(model.getAttribute("user"), testUser);
        Assertions.assertSame(allRoles, model.getAttribute("availableRoles"));
        Set<?> userAssignedRoles = (Set<?>) model.getAttribute("userAssignedRoles");
        Assertions.assertNotNull(userAssignedRoles);
        Assertions.assertTrue(userAssignedRoles.stream().findFirst().isPresent());
        String returnedAssignedAppRoleId = (String) userAssignedRoles.stream().findFirst().get();
        Assertions.assertEquals(testUserRole.getId(), returnedAssignedAppRoleId);
    }

    @Test
    public void testEditUserRolesThrowsExceptionWhenNoUserProvided() {
        // Given
        final String userId = "12345";
        when(userService.getEntraUserById(userId)).thenReturn(Optional.empty());
        // When
        assertThrows(NoSuchElementException.class, () -> userController.editUserRoles(userId, model, session));
    }

    @Test
    public void testUpdateUserRolesReturnsCorrectView() {
        // Given
        final String userId = "12345";
        final List<String> selectedRoles = new ArrayList<>();

        // When
        RedirectView view = userController.updateUserRoles(userId, selectedRoles);

        // Then
        Assertions.assertEquals("/admin/users", view.getUrl());
    }

    @Test
    public void testEditUserAppsReturnsCorrectViewAndAttributes() {
        // Given
        final UUID userId = UUID.randomUUID();
        EntraUserDto testUser = new EntraUserDto();
        testUser.setId(userId.toString());
        testUser.setFullName("Test User");

        UUID appId = UUID.randomUUID();
        AppDto testApp = new AppDto();
        testApp.setId(appId.toString());
        testApp.setName("Test App");

        when(userService.getEntraUserById(userId.toString())).thenReturn(Optional.of(testUser));
        when(userService.getUserAppsByUserId(userId.toString())).thenReturn(Set.of(testApp));
        when(userService.getApps()).thenReturn(List.of(testApp));

        // When
        String view = userController.editUserApps(userId.toString(), model);

        // Then
        assertThat(view).isEqualTo("edit-user-apps");
        assertThat(model.getAttribute("user")).isNotNull();
        EntraUserDto returnedUser = (EntraUserDto) model.getAttribute("user");
        Assertions.assertEquals(testUser.getId(), returnedUser.getId());
        Assertions.assertEquals(testUser.getFullName(), returnedUser.getFullName());
        assertThat(model.getAttribute("userAssignedApps")).isNotNull();
        Set<AppDto> assignedApps = (Set<AppDto>) model.getAttribute("userAssignedApps");
        assertThat(assignedApps).hasSize(1);
        assertThat(model.getAttribute("availableApps")).isNotNull();
        List<AppDto> availableApps = (List<AppDto>) model.getAttribute("availableApps");
        assertThat(availableApps).hasSize(1);
    }

    @Test
    public void testSetSelectedAppsEditReturnsCorrectRedirectAndAttributes() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID appId = UUID.randomUUID();
        List<String> apps = List.of(appId.toString());
        HttpSession session = new MockHttpSession();

        // When
        RedirectView redirectView = userController.setSelectedAppsEdit(userId.toString(), apps, session);

        // Then
        assertThat(redirectView.getUrl()).isEqualTo(String.format("/admin/users/edit/%s/roles", userId));
        assertThat(session.getAttribute("selectedApps")).isNotNull();
        List<String> returnedApps = (List<String>) session.getAttribute("selectedApps");
        assertThat(returnedApps).hasSize(1);
        assertThat(returnedApps.getFirst()).isEqualTo(appId.toString());
    }

    @Test
    public void testSetSelectedAppsEditThrowsExceptionWhenIdIsNotValidUuid() {
        // Given
        String userId = "testUserId";
        HttpSession session = new MockHttpSession();

        // When
        assertThrows(IllegalArgumentException.class,
                () -> userController.setSelectedAppsEdit(userId, List.of(), session));

    }

}