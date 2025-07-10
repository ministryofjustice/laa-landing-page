package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.CreateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateRoleAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.CreateUserDetailsIncompleteException;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
import uk.gov.justice.laa.portal.landingpage.forms.EditUserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.OfficesForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getListFromHttpSession;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;
import uk.gov.justice.laa.portal.landingpage.utils.UserUtils;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppViewModel;

/**
 * User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class UserController {

    private final LoginService loginService;
    private final UserService userService;
    private final OfficeService officeService;
    private final EventService eventService;
    private final FirmService firmService;
    private final ModelMapper mapper;

    /**
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/users")
    public String displayAllUsers(
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String usertype,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) boolean showFirmAdmins,
            Model model, HttpSession session, Authentication authentication) {

        PaginatedUsers paginatedUsers;
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        boolean internal = userService.isInternal(entraUser);
        if (!internal) {
            List<UUID> userFirms = firmService.getUserFirms(entraUser).stream().map(FirmDto::getId).toList();
            paginatedUsers = getPageOfUsersForExternal(userFirms, search, showFirmAdmins, page, size, sort, direction);
        } else {
            if (Objects.isNull(usertype)) {
                usertype = "external";
            }
            paginatedUsers = getPageOfUsersForInternal(usertype, search, showFirmAdmins, page, size, sort, direction);
        }

        String successMessage = (String) session.getAttribute("successMessage");
        if (successMessage != null) {
            model.addAttribute("successMessage", successMessage);
            // Clear the success message from the session to avoid showing it again
            session.removeAttribute("successMessage");
        }

        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("requestedPageSize", size);
        model.addAttribute("actualPageSize", paginatedUsers.getUsers().size());
        model.addAttribute("page", page);
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("usertype", usertype);
        model.addAttribute("internal", internal);
        model.addAttribute("showFirmAdmins", showFirmAdmins);

        return "users";
    }

    protected PaginatedUsers getPageOfUsersForExternal(List<UUID> userFirms, String searchTerm, boolean showFirmAdmins,
            int page, int size, String sort, String direction) {
        return userService.getPageOfUsersByNameOrEmail(searchTerm, false, showFirmAdmins, userFirms, page, size, sort,
                direction);
    }

    protected PaginatedUsers getPageOfUsersForInternal(String userType, String searchTerm, boolean showFirmAdmins,
            int page, int size, String sort, String direction) {
        boolean isInternal = !userType.equals("external");
        return userService.getPageOfUsersByNameOrEmail(searchTerm, isInternal, showFirmAdmins, null, page, size, sort,
                direction);
    }

    @GetMapping("/users/edit/{id}")
    public String editUser(@PathVariable String id, Model model) {
        Optional<EntraUserDto> optionalUser = userService.getEntraUserById(id);
        if (optionalUser.isPresent()) {
            EntraUserDto user = optionalUser.get();
            List<AppRoleDto> roles = userService.getUserAppRolesByUserId(user.getId());
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
        }
        return "edit-user";
    }

    /**
     * Disable group of users via graph SDK
     */
    @PreAuthorize("hasAuthority('SCOPE_User.EnableDisableAccount.All')")
    @PostMapping("/users/disable")
    public String disableUsers(@RequestParam("disable-user") List<String> id) throws IOException {
        userService.disableUsers(id);
        return "redirect:/users";
    }

    /**
     * Manage user via graph SDK
     */
    @GetMapping("/users/manage/{id}")
    public String manageUser(@PathVariable String id, Model model) {
        Optional<EntraUserDto> optionalUser = userService.getEntraUserById(id);
        List<AppRoleDto> userAppRoles = userService.getUserAppRolesByUserId(id);
        List<Office> userOffices = userService.getUserOfficesByUserId(id);
        optionalUser.ifPresent(user -> model.addAttribute("user", user));
        model.addAttribute("userAppRoles", userAppRoles);
        model.addAttribute("userOffices", userOffices);
        return "manage-user";
    }

    @GetMapping("/user/create/details")
    public String createUser(UserDetailsForm userDetailsForm, HttpSession session, Model model) {
        EntraUserDto user = (EntraUserDto) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new EntraUserDto();
        }
        List<FirmDto> firms = firmService.getFirms();
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        model.addAttribute("firms", firms);
        model.addAttribute("selectedFirm", selectedFirm);
        // If user is already in session, populate the form with existing user details
        userDetailsForm = UserUtils.populateUserDetailsFormWithSession(userDetailsForm, user, session);
        model.addAttribute("userDetailsForm", userDetailsForm);
        model.addAttribute("user", user);
        model.addAttribute("userDetailsForm", userDetailsForm);


        // Store the model in session to handle validation errors later
        session.setAttribute("createUserDetailsModel", model);
        return "add-user-details";
    }

    @PostMapping("/user/create/details")
    public String postUser(
            @Valid UserDetailsForm userDetailsForm, BindingResult result,
            @RequestParam(required = false) String isFirmAdmin,
            HttpSession session, Model model) {

        EntraUserDto user = (EntraUserDto) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new EntraUserDto();
        }

        if (userService.userExistsByEmail(userDetailsForm.getEmail())) {
            result.rejectValue("email", "error.email", "Email address already exists");
        }
        // Set user details from the form
        user.setFirstName(userDetailsForm.getFirstName());
        user.setLastName(userDetailsForm.getLastName());
        user.setFullName(userDetailsForm.getFirstName() + " " + userDetailsForm.getLastName());
        user.setEmail(userDetailsForm.getEmail());
        session.setAttribute("user", user);

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while creating user: {}", result.getAllErrors());

            Model modelFromSession = (Model) session.getAttribute("createUserDetailsModel");
            if (modelFromSession == null) {
                return "redirect:/admin/user/create/details";
            }

            model.addAttribute("firms", modelFromSession.getAttribute("firms"));
            model.addAttribute("selectedFirm", modelFromSession.getAttribute("selectedFirm"));
            model.addAttribute("user", modelFromSession.getAttribute("user"));
            return "add-user-details";
        }

        // Set firm and admin status
        FirmDto firm = firmService.getFirm(userDetailsForm.getFirmId());
        session.setAttribute("firm", firm);
        session.setAttribute("isFirmAdmin", userDetailsForm.getIsFirmAdmin());

        // Clear the createUserDetailsModel from session to avoid stale data
        session.removeAttribute("createUserDetailsModel");
        return "redirect:/admin/user/create/services";
    }

    @GetMapping("/user/create/services")
    public String selectUserApps(ApplicationsForm applicationsForm, Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        // TODO: Make this use the selected user type rather than a hard-coded type. Our
        // user creation flow is only for external users right now.
        List<AppViewModel> apps = userService.getAppsByUserType(UserType.EXTERNAL_SINGLE_FIRM).stream()
                .map(appDto -> {
                    AppViewModel appViewModel = mapper.map(appDto, AppViewModel.class);
                    appViewModel.setSelected(selectedApps.contains(appDto.getId()));
                    return appViewModel;
                }).toList();
        model.addAttribute("apps", apps);
        EntraUserDto user = getObjectFromHttpSession(session, "user", EntraUserDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        model.addAttribute("user", user);
        return "add-user-apps";
    }

    @PostMapping("/user/create/services")
    public String setSelectedApps(ApplicationsForm applicationsForm, Model model,
            HttpSession session) {
        if (applicationsForm.getApps() == null || applicationsForm.getApps().isEmpty()) {
            return "redirect:/admin/user/create/check-answers";
        }
        session.setAttribute("apps", applicationsForm.getApps());
        session.removeAttribute("userCreateRolesModel");
        return "redirect:/admin/user/create/roles";
    }

    @GetMapping("/user/create/roles")
    public String getSelectedRoles(RolesForm rolesForm, Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        Model modelFromSession = (Model) session.getAttribute("userCreateRolesModel");
        Integer selectedAppIndex;
        if (modelFromSession != null && modelFromSession.getAttribute("createUserRolesSelectedAppIndex") != null) {
            selectedAppIndex = (Integer) modelFromSession.getAttribute("createUserRolesSelectedAppIndex");
        } else {
            selectedAppIndex = 0;
        }
        AppDto currentApp = userService.getAppByAppId(selectedApps.get(selectedAppIndex)).orElseThrow();
        // TODO: Make this use the selected user type rather than a hard-coded type. Our
        // user creation flow is only for external users right now.
        List<AppRoleDto> roles = userService.getAppRolesByAppIdAndUserType(selectedApps.get(selectedAppIndex),
                UserType.EXTERNAL_SINGLE_FIRM);
        List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
        List<AppRoleViewModel> appRoleViewModels = roles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).toList();
        EntraUserDto user = getObjectFromHttpSession(session, "user", EntraUserDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        model.addAttribute("user", user);
        model.addAttribute("roles", appRoleViewModels);
        model.addAttribute("createUserRolesSelectedAppIndex", selectedAppIndex);
        model.addAttribute("createUserRolesCurrentApp", currentApp);

        // Store the model in session to handle validation errors later and track
        // currently selected app.
        session.setAttribute("userCreateRolesModel", model);
        return "add-user-roles";
    }

    @PostMapping("/user/create/roles")
    public String setSelectedRoles(@Valid RolesForm rolesForm, BindingResult result,
            Model model, HttpSession session) {
        Model modelFromSession = (Model) session.getAttribute("userCreateRolesModel");
        if (modelFromSession == null) {
            return "redirect:/admin/user/create/roles";
        }
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while setting user roles: {}", result.getAllErrors());
            // If there are validation errors, return to the roles page with errors
            model.addAttribute("roles", modelFromSession.getAttribute("roles"));
            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("createUserRolesSelectedAppIndex",
                    modelFromSession.getAttribute("createUserRolesSelectedAppIndex"));
            model.addAttribute("createUserRolesCurrentApp", modelFromSession.getAttribute("createUserRolesCurrentApp"));

            return "add-user-roles";
        }
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("createUserAllSelectedRoles");
        if (allSelectedRolesByPage == null) {
            allSelectedRolesByPage = new HashMap<>();
        }
        int selectedAppIndex = (Integer) modelFromSession.getAttribute("createUserRolesSelectedAppIndex");

        // Add the roles for the currently selected app to a map for lookup.
        allSelectedRolesByPage.put(selectedAppIndex, rolesForm.getRoles());
        if (selectedAppIndex >= selectedApps.size() - 1) {
            // Clear the userCreateRolesModel and page roles from session to avoid stale
            // data
            session.removeAttribute("userCreateRolesModel");
            session.removeAttribute("createUserAllSelectedRoles");
            // Flatten the map to a single list of all selected roles across all pages.
            List<String> allSelectedRoles = allSelectedRolesByPage.values().stream()
                    .flatMap(List::stream)
                    .toList();
            // Set selected roles in session
            session.setAttribute("roles", allSelectedRoles);
            return "redirect:/admin/user/create/offices";
        } else {
            modelFromSession.addAttribute("createUserRolesSelectedAppIndex", selectedAppIndex + 1);
            session.setAttribute("createUserAllSelectedRoles", allSelectedRolesByPage);
            session.setAttribute("userCreateRolesModel", modelFromSession);
            return "redirect:/admin/user/create/roles";
        }

    }

    @GetMapping("/user/create/offices")
    public String offices(OfficesForm officesForm, HttpSession session, Model model) {
        OfficeData selectedOfficeData = getObjectFromHttpSession(session, "officeData", OfficeData.class)
                .orElseGet(OfficeData::new);
        FirmDto selectedFirm = getObjectFromHttpSession(session, "firm", FirmDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        List<Office> offices = officeService.getOfficesByFirms(List.of(selectedFirm.getId()));
        List<OfficeModel> officeData = offices.stream()
                .map(office -> new OfficeModel(office.getName(), office.getAddress(),
                        office.getId().toString(), Objects.nonNull(selectedOfficeData.getSelectedOffices())
                                && selectedOfficeData.getSelectedOffices().contains(office.getId().toString())))
                .collect(Collectors.toList());
        model.addAttribute("officeData", officeData);
        EntraUserDto user = getObjectFromHttpSession(session, "user", EntraUserDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        model.addAttribute("user", user);

        // Store the model in session to handle validation errors later
        session.setAttribute("createUserOfficesModel", model);
        return "add-user-offices";
    }

    @PostMapping("/user/create/offices")
    public String postOffices(@Valid OfficesForm officesForm, BindingResult result, Model model, HttpSession session) {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while selecting offices: {}", result.getAllErrors());
            // If there are validation errors, return to the offices page with errors
            Model modelFromSession = (Model) session.getAttribute("createUserOfficesModel");
            if (modelFromSession == null) {
                return "redirect:/admin/user/create/offices";
            }

            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("officeData", modelFromSession.getAttribute("officeData"));
            return "add-user-offices";
        }

        OfficeData officeData = new OfficeData();
        List<String> selectedOffices = officesForm.getOffices();
        officeData.setSelectedOffices(officesForm.getOffices());
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        List<Office> offices = officeService.getOfficesByFirms(List.of(selectedFirm.getId()));
        List<String> selectedDisplayNames = new ArrayList<>();
        for (Office office : offices) {
            if (Objects.nonNull(selectedOffices)
                    && selectedOffices.contains(office.getId().toString())) {
                selectedDisplayNames.add(office.getName());
            }
        }
        officeData.setSelectedOfficesDisplay(selectedDisplayNames);
        session.setAttribute("officeData", officeData);

        // Clear the createUserOfficesModel from session to avoid stale data
        session.removeAttribute("createUserOfficesModel");
        return "redirect:/admin/user/create/check-answers";
    }

    @GetMapping("/user/create/check-answers")
    public String getUserCheckAnswers(Model model, HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        if (!selectedApps.isEmpty()) {
            List<AppRoleDto> roles = userService.getAllAvailableRolesForApps(selectedApps);
            List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class)
                    .orElseGet(ArrayList::new);
            Map<String, List<AppRoleViewModel>> cyaRoles = new HashMap<>();
            List<String> displayRoles = new ArrayList<>();
            for (AppRoleDto role : roles) {
                if (selectedRoles.contains(role.getId())) {
                    List<AppRoleViewModel> appRoles = cyaRoles.getOrDefault(role.getApp().getId(), new ArrayList<>());
                    AppRoleViewModel appRoleViewModel = mapper.map(role, AppRoleViewModel.class);
                    appRoleViewModel.setAppName(role.getApp().getName());
                    appRoles.add(appRoleViewModel);
                    cyaRoles.put(role.getApp().getId(), appRoles);
                    displayRoles.add(role.getName());
                }
            }
            session.setAttribute("displayRoles", String.join(", ", displayRoles));
            model.addAttribute("roles", cyaRoles);
        }

        EntraUserDto user = getObjectFromHttpSession(session, "user", EntraUserDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        model.addAttribute("user", user);

        OfficeData officeData = getObjectFromHttpSession(session, "officeData", OfficeData.class)
                .orElseGet(OfficeData::new);
        model.addAttribute("officeData", officeData);

        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        model.addAttribute("firm", selectedFirm);

        Boolean isFirmAdmin = (Boolean) session.getAttribute("isFirmAdmin");
        model.addAttribute("isFirmAdmin", isFirmAdmin);
        return "add-user-check-answers";
    }

    @PostMapping("/user/create/check-answers")
    // @PreAuthorize("hasAuthority('SCOPE_User.ReadWrite.All') and
    // hasAuthority('SCOPE_Directory.ReadWrite.All')")
    public String addUserCheckAnswers(HttpSession session, Authentication authentication) {
        Optional<EntraUserDto> userOptional = getObjectFromHttpSession(session, "user", EntraUserDto.class);
        Optional<List<String>> selectedRolesOptional = getListFromHttpSession(session, "roles", String.class);
        Optional<FirmDto> firmOptional = Optional.ofNullable((FirmDto) session.getAttribute("firm"));
        Optional<Boolean> isFirmAdminOptional = Optional.ofNullable((Boolean) session.getAttribute("isFirmAdmin"));
        Optional<OfficeData> optionalSelectedOfficeData = getObjectFromHttpSession(session, "officeData",
                OfficeData.class);

        if (userOptional.isPresent()) {
            EntraUserDto user = userOptional.get();
            List<String> selectedRoles = selectedRolesOptional.orElseGet(ArrayList::new);
            Optional<String> displayRolesOption = getObjectFromHttpSession(session, "displayRoles", String.class);
            String displayRoles = displayRolesOption.map(String::toString).orElse("");

            FirmDto selectedFirm = firmOptional.orElseGet(FirmDto::new);
            Boolean isFirmAdmin = isFirmAdminOptional.orElse(Boolean.FALSE);
            List<String> selectedOffices = optionalSelectedOfficeData.map(OfficeData::getSelectedOffices)
                    .orElseGet(ArrayList::new);
            List<String> selectedOfficesDisplay = optionalSelectedOfficeData.map(OfficeData::getSelectedOfficesDisplay)
                    .orElseGet(ArrayList::new);
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            EntraUser entraUser = userService.createUser(user, selectedRoles, selectedOffices, selectedFirm,
                    isFirmAdmin, currentUserDto.getName());
            CreateUserAuditEvent createUserAuditEvent = new CreateUserAuditEvent(currentUserDto, entraUser, displayRoles, selectedOfficesDisplay, selectedFirm.getName());
            eventService.logEvent(createUserAuditEvent);

            String successMessage = user.getFirstName() + " " + user.getLastName()
                    + " has been added to the system"
                    + (isFirmAdmin ? " as a Firm Admin" : "")
                    + ". An email invitation has been sent. They must accept the invitation to gain access.";
            session.setAttribute("successMessage", successMessage);
        } else {
            log.error("No user attribute was present in request. User not created.");
        }

        session.removeAttribute("firm");
        session.removeAttribute("isFirmAdmin");
        session.removeAttribute("apps");
        session.removeAttribute("roles");
        session.removeAttribute("officeData");

        return "redirect:/admin/user/create/confirmation";
    }

    @GetMapping("/user/create/confirmation")
    public String addUserCreated(Model model, HttpSession session) {
        Optional<EntraUserDto> userOptional = getObjectFromHttpSession(session, "user", EntraUserDto.class);
        if (userOptional.isPresent()) {
            EntraUserDto user = userOptional.get();
            model.addAttribute("user", user);
        } else {
            log.error("No user attribute was present in request. User not added to model.");
        }
        session.removeAttribute("user");
        return "add-user-created";
    }

    @GetMapping("/user/create/cancel")
    public String cancelUserCreation(HttpSession session) {
        session.removeAttribute("user");
        session.removeAttribute("firm");
        session.removeAttribute("isFirmAdmin");
        session.removeAttribute("apps");
        session.removeAttribute("roles");
        session.removeAttribute("officeData");
        return "redirect:/admin/users";
    }

    /**
     * Get User Details for editing
     * 
     * @param id    User ID
     * @param model Model to hold user details form and user data
     * @return View name for editing user details
     * @throws IOException              If an error occurs during user retrieval
     * @throws IllegalArgumentException If the user ID is invalid or not found
     */

    @GetMapping("/users/edit/{id}/details")
    public String editUserDetails(@PathVariable String id, Model model) {
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        EditUserDetailsForm editUserDetailsForm = new EditUserDetailsForm();
        editUserDetailsForm.setFirstName(user.getFirstName());
        editUserDetailsForm.setLastName(user.getLastName());
        editUserDetailsForm.setEmail(user.getEmail());
        model.addAttribute("editUserDetailsForm", editUserDetailsForm);
        model.addAttribute("user", user);
        return "edit-user-details";
    }

    /**
     * Update user details
     * 
     * @param id              User ID
     * @param editUserDetailsForm User details form
     * @param result          Binding result for validation errors
     * @param session         HttpSession to store user details
     * @return Redirect to user management page
     * @throws IOException              If an error occurs during user update
     * @throws IllegalArgumentException If the user ID is invalid or not found
     */
    @PostMapping("/users/edit/{id}/details")
    public String updateUserDetails(@PathVariable String id,
            @Valid EditUserDetailsForm editUserDetailsForm, BindingResult result,
            HttpSession session) throws IOException {
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while updating user details: {}", result.getAllErrors());
            // If there are validation errors, return to the edit user details page with
            // errors
            EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
            session.setAttribute("user", user);
            session.setAttribute("editUserDetailsForm", editUserDetailsForm);
            return "edit-user-details";
        }
        // Update user details
        userService.updateUserDetails(id, editUserDetailsForm.getFirstName(), editUserDetailsForm.getLastName());
        return "redirect:/admin/users/manage/" + id;
    }

    /**
     * Retrieves available apps for user and their currently assigned apps.
     */
    @GetMapping("/users/edit/{id}/apps")
    public String editUserApps(@PathVariable String id, Model model) {
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        UserType userType = userService.getUserTypeByUserId(id).orElseThrow();
        Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(id);
        List<AppDto> availableApps = userService.getAppsByUserType(userType);

        // Add selected attribute to available apps based on user assigned apps
        availableApps.forEach(app -> {
            app.setSelected(userAssignedApps.stream()
                    .anyMatch(userApp -> userApp.getId().equals(app.getId())));
        });

        model.addAttribute("user", user);
        model.addAttribute("apps", availableApps);

        return "edit-user-apps";
    }

    @PostMapping("/users/edit/{id}/apps")
    public RedirectView setSelectedAppsEdit(@PathVariable String id,
            @RequestParam(value = "apps", required = false) List<String> apps,
            HttpSession session) {
        // Handle case where no apps are selected (apps will be null)
        List<String> selectedApps = apps != null ? apps : new ArrayList<>();
        session.setAttribute("selectedApps", selectedApps);

        // If no apps are selected, persist empty roles to database and redirect to
        // manage user page
        if (selectedApps.isEmpty()) {
            // Update user to have no roles (empty list)
            userService.updateUserRoles(id, new ArrayList<>());
            // Ensure passed in ID is a valid UUID to avoid open redirects.
            UUID uuid = UUID.fromString(id);
            return new RedirectView(String.format("/admin/users/manage/%s", uuid));
        }

        // Ensure passed in ID is a valid UUID to avoid open redirects.
        UUID uuid = UUID.fromString(id);
        return new RedirectView(String.format("/admin/users/edit/%s/roles", uuid));
    }

    /**
     * Retrieves available roles for user and their currently assigned roles.
     * 
     * @param id               User ID
     * @param selectedAppIndex Index of the currently selected app
     * @param model            Model to hold user and role data
     * @param session          HttpSession to store selected apps and roles
     * @return View name for editing user roles
     * @throws IllegalArgumentException If the user ID is invalid or not found
     * @throws IOException              If an error occurs during user retrieval
     */
    @GetMapping("/users/edit/{id}/roles")
    public String editUserRoles(@PathVariable String id,
            @RequestParam(defaultValue = "0") Integer selectedAppIndex,
            RolesForm rolesForm,
            Model model, HttpSession session) {

        final EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class)
                .orElseGet(() -> {
                    // If no selectedApps in session, get user's current apps
                    Set<AppDto> userApps = userService.getUserAppsByUserId(id);
                    List<String> userAppIds = userApps.stream()
                            .map(AppDto::getId)
                            .collect(Collectors.toList());
                    session.setAttribute("selectedApps", userAppIds);
                    return userAppIds;
                });

        // Ensure the selectedAppIndex is within bounds
        if (selectedApps.isEmpty()) {
            // No apps assigned to user, redirect back to manage page
            return "redirect:/admin/users/manage/" + id;
        }

        Model modelFromSession = (Model) session.getAttribute("userEditRolesModel");
        Integer currentSelectedAppIndex;
        if (modelFromSession != null && modelFromSession.getAttribute("editUserRolesSelectedAppIndex") != null) {
            currentSelectedAppIndex = (Integer) modelFromSession.getAttribute("editUserRolesSelectedAppIndex");
        } else {
            currentSelectedAppIndex = selectedAppIndex != null ? selectedAppIndex : 0;
        }

        // Ensure the index is within bounds
        if (currentSelectedAppIndex >= selectedApps.size()) {
            currentSelectedAppIndex = 0;
        }

        AppDto currentApp = userService.getAppByAppId(selectedApps.get(currentSelectedAppIndex)).orElseThrow();
        List<AppRoleDto> roles = userService.getAppRolesByAppId(selectedApps.get(currentSelectedAppIndex));
        List<AppRoleDto> userRoles = userService.getUserAppRolesByUserId(id);

        // Get currently selected roles from session or use user's existing roles
        List<String> selectedRoles = getListFromHttpSession(session, "editUserRoles", String.class)
                .orElseGet(() -> userRoles.stream().map(AppRoleDto::getId).collect(Collectors.toList()));

        List<AppRoleViewModel> appRoleViewModels = roles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).toList();

        model.addAttribute("user", user);
        model.addAttribute("roles", appRoleViewModels);
        model.addAttribute("editUserRolesSelectedAppIndex", currentSelectedAppIndex);
        model.addAttribute("editUserRolesCurrentApp", currentApp);

        // Store the model in session to handle validation errors later and track
        // currently selected app.
        session.setAttribute("userEditRolesModel", model);
        return "edit-user-roles";
    }

    /**
     * Update user roles for a specific app.
     * 
     * @param id               User ID
     * @param selectedAppIndex Index of the currently selected app
     * @param authentication   Authentication object for the current user
     * @param session          HttpSession to store selected apps and roles
     * @return Redirect view to the user management page or next app roles page
     * @throws IllegalArgumentException If the user ID is invalid or not found
     * @throws IOException              If an error occurs during user role update
     */
    @PostMapping("/users/edit/{id}/roles")
    public String updateUserRoles(@PathVariable String id,
            @Valid RolesForm rolesForm, BindingResult result,
            @RequestParam int selectedAppIndex,
            Authentication authentication,
            Model model, HttpSession session) {
        Model modelFromSession = (Model) session.getAttribute("userEditRolesModel");
        if (modelFromSession == null) {
            return "redirect:/admin/users/edit/" + id + "/roles";
        }
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while setting user roles: {}", result.getAllErrors());
            // If there are validation errors, return to the roles page with errors
            // and role unseleected if it is not in the list
            List<AppRoleViewModel> roles = (List<AppRoleViewModel>) modelFromSession.getAttribute("roles");
            if (roles != null) {
                // Add null check for rolesForm.getRoles()
                List<String> selectedRoleIds = rolesForm.getRoles() != null ? rolesForm.getRoles() : new ArrayList<>();
                roles.forEach(role -> {
                    if (!selectedRoleIds.contains(role.getId())) {
                        role.setSelected(false);
                    }
                });
            }
            model.addAttribute("roles", roles);
            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("editUserRolesSelectedAppIndex",
                    modelFromSession.getAttribute("editUserRolesSelectedAppIndex"));
            model.addAttribute("editUserRolesCurrentApp", modelFromSession.getAttribute("editUserRolesCurrentApp"));

            return "edit-user-roles";
        }

        EntraUserDto user = userService.getEntraUserById(id).orElse(null);
        List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class)
                .orElseGet(ArrayList::new);
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("editUserAllSelectedRoles");
        if (allSelectedRolesByPage == null) {
            allSelectedRolesByPage = new HashMap<>();
        }
        // Add the roles for the currently selected app to a map for lookup.
        allSelectedRolesByPage.put(selectedAppIndex, rolesForm.getRoles());
        if (selectedAppIndex >= selectedApps.size() - 1) {
            // Clear the userEditRolesModel and page roles from session to avoid stale data
            session.removeAttribute("userEditRolesModel");
            session.removeAttribute("editUserAllSelectedRoles");
            // Flatten the map to a single list of all selected roles across all pages.
            List<String> allSelectedRoles = allSelectedRolesByPage.values().stream()
                    .flatMap(List::stream)
                    .toList();
            userService.updateUserRoles(id, allSelectedRoles);
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            UpdateRoleAuditEvent updateRoleAuditEvent = new UpdateRoleAuditEvent(currentUserDto, user, allSelectedRoles);
            eventService.logEvent(updateRoleAuditEvent);
            return "redirect:/admin/users/manage/" + id;
        } else {
            modelFromSession.addAttribute("editUserRolesSelectedAppIndex", selectedAppIndex + 1);
            session.setAttribute("editUserAllSelectedRoles", allSelectedRolesByPage);
            session.setAttribute("userEditRolesModel", modelFromSession);
            // Ensure passed in ID is a valid UUID to avoid open redirects.
            UUID uuid = UUID.fromString(id);
            return "redirect:/admin/users/edit/" + uuid + "/roles?selectedAppIndex=" + (selectedAppIndex + 1);
        }
    }

    /**
     * Get user offices for editing
     * 
     * @param id    User ID
     * @param model Model to hold user and office data
     * @return View name for editing user offices
     * @throws IllegalArgumentException If the user ID is invalid or not found
     */
    @GetMapping("/users/edit/{id}/offices")
    public String editUserOffices(@PathVariable String id, Model model, HttpSession session) {
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();

        // Get user's current offices
        List<Office> userOffices = userService.getUserOfficesByUserId(id);
        Set<String> userOfficeIds = userOffices.stream()
                .map(office -> office.getId().toString())
                .collect(Collectors.toSet());

        // Get user's available offices by firm
        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(id);
        List<UUID> firmIds = userFirms.stream().map(FirmDto::getId).collect(Collectors.toList());
        List<Office> allOffices = officeService.getOfficesByFirms(firmIds);

        // Check if user has access to all offices
        boolean hasAllOffices = userOffices.size() == allOffices.size()
                && allOffices.stream()
                        .allMatch(office -> userOfficeIds.contains(office.getId().toString()));

        final List<OfficeModel> officeData = allOffices.stream()
                .map(office -> new OfficeModel(
                        office.getName(),
                        office.getAddress(),
                        office.getId().toString(),
                        userOfficeIds.contains(office.getId().toString())))
                .collect(Collectors.toList());

        // Create form object
        OfficesForm officesForm = new OfficesForm();
        List<String> selectedOffices = new ArrayList<>();

        if (hasAllOffices) {
            selectedOffices.add("ALL");
        } else {
            selectedOffices.addAll(userOfficeIds);
        }

        officesForm.setOffices(selectedOffices);

        model.addAttribute("user", user);
        model.addAttribute("officesForm", officesForm);
        model.addAttribute("officeData", officeData);
        model.addAttribute("hasAllOffices", hasAllOffices);

        // Store the model in session to handle validation errors later
        session.setAttribute("editUserOfficesModel", model);
        return "edit-user-offices";
    }

    /**
     * Update user offices
     * 
     * @param id          User ID
     * @param officesForm Offices form with selected office IDs
     * @param result      Binding result for validation errors
     * @param model       Model for the view
     * @param session     HttpSession to store office data
     * @return Redirect to user management page
     * @throws IOException If an error occurs during user office update
     */
    @PostMapping("/users/edit/{id}/offices")
    public String updateUserOffices(@PathVariable String id,
            @Valid OfficesForm officesForm, BindingResult result,
            Model model, HttpSession session) throws IOException {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while updating user offices: {}", result.getAllErrors());
            // If there are validation errors, return to the edit user offices page with
            // errors
            Model modelFromSession = (Model) session.getAttribute("editUserOfficesModel");
            if (modelFromSession == null) {
                return "redirect:/admin/users/edit/" + id + "/offices";
            }

            // make sure selected offices are not selected if validation errors occur
            List<OfficeModel> officeData = (List<OfficeModel>) modelFromSession.getAttribute("officeData");
            if (officeData != null) {
                List<String> selectedOfficeIds = officesForm.getOffices() != null ? officesForm.getOffices()
                        : new ArrayList<>();
                officeData.forEach(office -> {
                    if (!selectedOfficeIds.contains(office.getId())) {
                        office.setSelected(false);
                    }
                });
            }

            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("officeData", modelFromSession.getAttribute("officeData"));
            return "edit-user-offices";
        }

        // Update user offices
        List<String> selectedOffices = officesForm.getOffices() != null ? officesForm.getOffices() : new ArrayList<>();

        // Handle "ALL" option
        if (selectedOffices.contains("ALL")) {
            // If "ALL" is selected, get all available offices by firm
            List<FirmDto> userFirms = firmService.getUserFirmsByUserId(id);
            List<UUID> firmIds = userFirms.stream().map(FirmDto::getId).collect(Collectors.toList());
            List<Office> allOffices = officeService.getOfficesByFirms(firmIds);
            selectedOffices = allOffices.stream()
                    .map(office -> office.getId().toString())
                    .collect(Collectors.toList());
        }

        userService.updateUserOffices(id, selectedOffices);

        // Clear the session model
        session.removeAttribute("editUserOfficesModel");

        return "redirect:/admin/users/manage/" + id;
    }

    @GetMapping("/users/edit/{id}/cancel")
    public String cancelUserEdit(@PathVariable String id, HttpSession session) {
        // Clear all edit-related session attributes
        // Edit User Details Form
        session.removeAttribute("user");
        session.removeAttribute("editUserDetailsForm");

        // Edit User Apps/Roles Form
        session.removeAttribute("selectedApps");
        session.removeAttribute("editUserAllSelectedRoles");
        session.removeAttribute("userEditRolesModel");
        session.removeAttribute("editUserRoles");
        session.removeAttribute("availableRoles");
        session.removeAttribute("userAssignedRoles");
        session.removeAttribute("selectedAppIndex");
        session.removeAttribute("editUserRolesCurrentApp");
        session.removeAttribute("editUserRolesSelectedAppIndex");

        // Edit User Apps Form
        session.removeAttribute("userAssignedApps");
        session.removeAttribute("availableApps");

        // Edit User Offices Form
        session.removeAttribute("editUserOfficesModel");

        // Clear any success messages
        session.removeAttribute("successMessage");

        return "redirect:/admin/users/manage/" + id;
    }

    @ExceptionHandler(Exception.class)
    public RedirectView handleException(Exception ex) {
        log.error("Error while user management", ex);
        return new RedirectView("/error");
    }
}
