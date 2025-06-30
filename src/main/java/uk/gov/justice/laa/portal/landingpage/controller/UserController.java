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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.microsoft.graph.models.User;

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
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
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
            @RequestParam(required = false) String search,
            @RequestParam(required = false) boolean showFirmAdmins,
            Model model, HttpSession session) {

        PaginatedUsers paginatedUsers;
        if (search != null && !search.isEmpty()) {
            paginatedUsers = userService.getPageOfUsersByNameOrEmail(page, size, search, showFirmAdmins);
        } else {
            search = null;
            paginatedUsers = userService.getPageOfUsers(page, size, showFirmAdmins);
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

        return "users";
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
     * Retrieves a list of users from Microsoft Graph API.
     */
    @GetMapping("/userlist")
    public String displaySavedUsers(Model model) {
        List<EntraUserDto> users = userService.getSavedUsers();
        model.addAttribute("users", users);
        return "users";
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
        List<Office> offices = officeService.getOffices();
        optionalUser.ifPresent(user -> model.addAttribute("user", user));
        model.addAttribute("userAppRoles", userAppRoles);
        model.addAttribute("offices", offices);
        return "manage-user";
    }

    @GetMapping("/user/create/details")
    public String createUser(UserDetailsForm userDetailsForm, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
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
        model.addAttribute("user", user);

        // Store the model in session to handle validation errors later
        session.setAttribute("createUserDetailsModel", model);
        return "add-user-details";
    }

    @PostMapping("/user/create/details")
    public String postUser(
            @Valid UserDetailsForm userDetailsForm, BindingResult result,
            @RequestParam(required = false) String isFirmAdmin,
            HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new User();
        }

        if (userService.userExistsByEmail(userDetailsForm.getEmail())) {
            result.rejectValue("email", "error.email", "Email address already exists");
        }
        // Set user details from the form
        user.setGivenName(userDetailsForm.getFirstName());
        user.setSurname(userDetailsForm.getLastName());
        user.setDisplayName(userDetailsForm.getFirstName() + " " + userDetailsForm.getLastName());
        user.setMail(userDetailsForm.getEmail());
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
        List<AppViewModel> apps = userService.getApps().stream()
                .map(appDto -> {
                    AppViewModel appViewModel = mapper.map(appDto, AppViewModel.class);
                    appViewModel.setSelected(selectedApps.contains(appDto.getId()));
                    return appViewModel;
                }).toList();
        model.addAttribute("apps", apps);
        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
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
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class).orElseGet(ArrayList::new);
        Model modelFromSession = (Model) session.getAttribute("userCreateRolesModel");
        Integer selectedAppIndex;
        if (modelFromSession != null && modelFromSession.getAttribute("createUserRolesSelectedAppIndex") != null) {
            selectedAppIndex = (Integer) modelFromSession.getAttribute("createUserRolesSelectedAppIndex");
        } else {
            selectedAppIndex = 0;
        }
        AppDto currentApp = userService.getAppByAppId(selectedApps.get(selectedAppIndex)).orElseThrow();
        List<AppRoleDto> roles = userService.getAppRolesByAppId(selectedApps.get(selectedAppIndex));
        List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
        List<AppRoleViewModel> appRoleViewModels = roles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).toList();
        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
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
        ;
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
        // if user has firms, use officeService.getOfficesByFirms();
        List<Office> offices = officeService.getOffices();
        List<OfficeModel> officeData = offices.stream()
                .map(office -> new OfficeModel(office.getName(), office.getAddress(),
                        office.getId().toString(), Objects.nonNull(selectedOfficeData.getSelectedOffices())
                                && selectedOfficeData.getSelectedOffices().contains(office.getId().toString())))
                .collect(Collectors.toList());
        model.addAttribute("officeData", officeData);
        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
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
        // if user has firms, use officeService.getOfficesByFirms();
        List<Office> offices = officeService.getOffices();
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

        User user = getObjectFromHttpSession(session, "user", User.class).orElseGet(User::new);
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
        Optional<User> userOptional = getObjectFromHttpSession(session, "user", User.class);
        Optional<List<String>> selectedRolesOptional = getListFromHttpSession(session, "roles", String.class);
        Optional<FirmDto> firmOptional = Optional.ofNullable((FirmDto) session.getAttribute("firm"));
        Optional<Boolean> isFirmAdminOptional = Optional.ofNullable((Boolean) session.getAttribute("isFirmAdmin"));
        Optional<OfficeData> optionalSelectedOfficeData = getObjectFromHttpSession(session, "officeData",
                OfficeData.class);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
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
            eventService.auditUserCreate(currentUserDto, entraUser, displayRoles, selectedOfficesDisplay,
                    selectedFirm.getName());

            String successMessage = "" + user.getGivenName() + " "
                    + user.getSurname()
                    + " has been added to the system" + (isFirmAdmin ? " as a Firm Admin" : "")
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
        Optional<User> userOptional = getObjectFromHttpSession(session, "user", User.class);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            model.addAttribute("user", user);
        } else {
            log.error("No user attribute was present in request. User not added to model.");
        }
        session.removeAttribute("user");
        return "add-user-created";
    }

    /**
     * Retrieves available user roles for user
     */
    @GetMapping("/users/edit/{id}/roles")
    public String editUserRoles(@PathVariable String id,
            @RequestParam(defaultValue = "0") int selectedAppIndex,
            Model model, HttpSession session) {
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class)
                .orElseGet(ArrayList::new);
        List<AppRoleDto> userRoles = userService.getUserAppRolesByUserId(id);
        AppDto currentApp = userService.getAppByAppId(selectedApps.get(selectedAppIndex)).orElseThrow();
        List<AppRoleDto> availableRoles = userService.getAppRolesByAppId(selectedApps.get(selectedAppIndex));

        Set<String> userAssignedRoleIds = userRoles.stream()
                .filter(availableRoles::contains)
                .map(AppRoleDto::getId)
                .collect(Collectors.toSet());

        model.addAttribute("user", user);
        model.addAttribute("availableRoles", availableRoles);
        model.addAttribute("userAssignedRoles", userAssignedRoleIds);
        model.addAttribute("selectedAppIndex", selectedAppIndex);
        model.addAttribute("editUserRolesCurrentApp", currentApp);

        return "edit-user-roles";
    }

    /**
     * Update user roles via graph SDK
     */
    @PostMapping("/users/edit/{id}/roles")
    public RedirectView updateUserRoles(@PathVariable String id,
            @RequestParam(required = false) List<String> selectedRoles,
            @RequestParam int selectedAppIndex,
            Authentication authentication,
            HttpSession session) {
        EntraUserDto user = userService.getEntraUserById(id).orElse(null);
        List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class)
                .orElseGet(ArrayList::new);
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("editUserAllSelectedRoles");
        if (allSelectedRolesByPage == null) {
            allSelectedRolesByPage = new HashMap<>();
        }
        // Add the roles for the currently selected app to a map for lookup.
        allSelectedRolesByPage.put(selectedAppIndex, selectedRoles);
        if (selectedAppIndex >= selectedApps.size() - 1) {
            session.setAttribute("createUserAllSelectedRoles", null);
            // Flatten the map to a single list of all selected roles across all pages.
            List<String> allSelectedRoles = allSelectedRolesByPage.values().stream()
                    .flatMap(List::stream)
                    .toList();
            userService.updateUserRoles(id, allSelectedRoles);
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            eventService.auditUpdateRole(currentUserDto, user, selectedRoles);
            return new RedirectView("/admin/users");
        } else {
            // Ensure passed in ID is a valid UUID to avoid open redirects.
            UUID uuid = UUID.fromString(id);
            session.setAttribute("editUserAllSelectedRoles", allSelectedRolesByPage);
            return new RedirectView(
                    String.format("/admin/users/edit/%s/roles?selectedAppIndex=%d", uuid, selectedAppIndex + 1));
        }
    }

    /**
     * Retrieves available apps for user and their currently assigned apps.
     */
    @GetMapping("/users/edit/{id}/apps")
    public String editUserApps(@PathVariable String id, Model model) {
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(id);
        List<AppDto> availableApps = userService.getApps();

        model.addAttribute("user", user);
        model.addAttribute("userAssignedApps", userAssignedApps);
        model.addAttribute("availableApps", availableApps);

        return "edit-user-apps";
    }

    @PostMapping("/users/edit/{id}/apps")
    public RedirectView setSelectedAppsEdit(@PathVariable String id, @RequestParam("selectedApps") List<String> apps,
            HttpSession session) {
        session.setAttribute("selectedApps", apps);
        // Ensure passed in ID is a valid UUID to avoid open redirects.
        UUID uuid = UUID.fromString(id);
        return new RedirectView(String.format("/admin/users/edit/%s/roles", uuid));
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
}
