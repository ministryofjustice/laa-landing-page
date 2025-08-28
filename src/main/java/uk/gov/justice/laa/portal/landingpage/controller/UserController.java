package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CreateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
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
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.CcmsRoleGroupsUtil;
import uk.gov.justice.laa.portal.landingpage.utils.UserUtils;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getListFromHttpSession;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

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
    private final AccessControlService accessControlService;
    private final RoleAssignmentService roleAssignmentService;

    @GetMapping("/user/firms/search")
    @ResponseBody
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_EXTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_INTERNAL_USER)")
    public List<FirmDto> getFirms(Authentication authentication, @RequestParam(value = "q", defaultValue = "") String query) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        return firmService.getUserAccessibleFirms(entraUser, query);
    }

    @GetMapping("/users")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_EXTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_INTERNAL_USER)")
    public String displayAllUsers(
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "usertype", required = false) String usertype,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "showFirmAdmins", required = false) boolean showFirmAdmins,
            @RequestParam(name = "backButton", required = false) boolean backButton,
            FirmSearchForm firmSearchForm,
            Model model, HttpSession session, Authentication authentication) {

        // Process request parameters and handle session filters
        Map<String, Object> processedFilters = processRequestFilters(size, page, sort, direction, usertype, search,
                showFirmAdmins, backButton, session, firmSearchForm);
        size = (Integer) processedFilters.get("size");
        page = (Integer) processedFilters.get("page");
        sort = (String) processedFilters.get("sort");
        direction = (String) processedFilters.get("direction");
        usertype = (String) processedFilters.get("usertype");
        search = (String) processedFilters.get("search");
        firmSearchForm = (FirmSearchForm) processedFilters.get("firmSearchForm");
        showFirmAdmins = (Boolean) processedFilters.get("showFirmAdmins");

        PaginatedUsers paginatedUsers;
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        boolean internal = userService.isInternal(entraUser.getId());
        boolean canSeeAllUsers = accessControlService.authenticatedUserHasPermission(Permission.VIEW_INTERNAL_USER)
                && accessControlService.authenticatedUserHasPermission(Permission.VIEW_EXTERNAL_USER);

        // Debug logging
        log.debug("UserController.displayAllUsers - search: '{}', firmSearch: '{}', showFirmAdmins: {}",
                search, firmSearchForm, showFirmAdmins);

        if (canSeeAllUsers) {
            UserSearchCriteria searchCriteria = new UserSearchCriteria(search, firmSearchForm, null, showFirmAdmins);
            paginatedUsers = userService.getPageOfUsersBySearch(searchCriteria, page, size, sort, direction);
        } else if (accessControlService.authenticatedUserHasPermission(Permission.VIEW_INTERNAL_USER)) {
            UserSearchCriteria searchCriteria = new UserSearchCriteria(search, firmSearchForm, List.of(UserType.INTERNAL), showFirmAdmins);
            paginatedUsers = userService.getPageOfUsersBySearch(searchCriteria, page, size, sort, direction);
        } else if (accessControlService.authenticatedUserHasPermission(Permission.VIEW_EXTERNAL_USER) && internal) {
            UserSearchCriteria searchCriteria = new UserSearchCriteria(search, firmSearchForm, UserType.EXTERNAL_TYPES, showFirmAdmins);
            paginatedUsers = userService.getPageOfUsersBySearch(searchCriteria, page, size, sort, direction);
        } else {
            // External user - restrict to their firm only
            Optional<FirmDto> optionalFirm = firmService.getUserFirm(entraUser);
            if (optionalFirm.isPresent()) {
                // Check if user is trying to access a specific firm via firmSearchForm
                UUID selectedFirmId = firmSearchForm != null ? firmSearchForm.getSelectedFirmId() : null;
                if (selectedFirmId != null && !optionalFirm.get().getId().equals(selectedFirmId)) {
                    throw new RuntimeException("Firm access denied");
                }
                FirmSearchForm searchForm = Optional.ofNullable(firmSearchForm).orElse(FirmSearchForm.builder().build());
                searchForm.setSelectedFirmId(optionalFirm.get().getId());
                UserSearchCriteria searchCriteria = new UserSearchCriteria(search, searchForm, UserType.EXTERNAL_TYPES, showFirmAdmins);
                paginatedUsers = userService.getPageOfUsersBySearch(searchCriteria, page, size, sort, direction);
            } else {
                // Shouldn't happen, but return nothing if external user has no firm
                paginatedUsers = new PaginatedUsers();
            }
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
        model.addAttribute("firmSearch", firmSearchForm);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("usertype", usertype);
        model.addAttribute("internal", internal);
        model.addAttribute("showFirmAdmins", showFirmAdmins);
        boolean allowCreateUser = accessControlService.authenticatedUserHasPermission(Permission.CREATE_EXTERNAL_USER);
        model.addAttribute("allowCreateUser", allowCreateUser);

        // If firmSearchForm is already populated from session (e.g., validation
        // errors), keep it
        FirmSearchForm existingForm = (FirmSearchForm) session.getAttribute("firmSearchForm");
        if (existingForm != null) {
            firmSearchForm = existingForm;
            session.removeAttribute("firmSearchForm");
        }

        model.addAttribute("firmSearchForm", firmSearchForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Manage your users");

        return "users";
    }

    /**
     * Helper method to check if filters contain any active (non-default) values
     */
    private boolean hasActiveFilters(Map<String, Object> filters) {
        if (filters == null) {
            return false;
        }

        // Check if any filter has a non-default value
        String search = (String) filters.get("search");
        String usertype = (String) filters.get("usertype");
        String sort = (String) filters.get("sort");
        String direction = (String) filters.get("direction");
        Boolean showFirmAdmins = (Boolean) filters.get("showFirmAdmins");
        Integer size = (Integer) filters.get("size");
        Integer page = (Integer) filters.get("page");
        FirmSearchForm firmSearchForm = (FirmSearchForm) filters.get("firmSearchForm");

        return (search != null && !search.isEmpty())
                || (usertype != null && !usertype.isEmpty())
                || (sort != null && !sort.isEmpty())
                || (direction != null && !direction.isEmpty())
                || (showFirmAdmins != null && showFirmAdmins)
                || (size != null && size != 10)
                || (page != null && page != 1)
                || (firmSearchForm != null
                && firmSearchForm.getSelectedFirmId() != null
                && firmSearchForm.getFirmSearch() != null
                && !firmSearchForm.getFirmSearch().isEmpty());
    }

    @GetMapping("/users/edit/{id}")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String editUser(@PathVariable String id, Model model) {
        Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);
        if (optionalUser.isPresent()) {
            UserProfileDto user = optionalUser.get();
            List<AppRoleDto> roles = userService.getUserAppRolesByUserId(user.getId().toString());
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Edit user - " + user.getFullName());
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
    @PreAuthorize("@accessControlService.canAccessUser(#id)")
    public String manageUser(@PathVariable String id, Model model, HttpSession session) {
        Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);

        List<AppRoleDto> userAppRoles = optionalUser.get().getAppRoles().stream()
                .map(appRoleDto -> mapper.map(appRoleDto, AppRoleDto.class))
                .collect(Collectors.toList());
        List<OfficeDto> userOffices = optionalUser.get().getOffices();
        final Boolean isAccessGranted = userService.isAccessGranted(optionalUser.get().getId().toString());
        optionalUser.ifPresent(user -> model.addAttribute("user", user));
        model.addAttribute("userAppRoles", userAppRoles);
        model.addAttribute("userOffices", userOffices);
        model.addAttribute("isAccessGranted", isAccessGranted);
        boolean externalUser = UserType.EXTERNAL_TYPES.contains(optionalUser.get().getUserType());
        model.addAttribute("externalUser", externalUser);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Manage user - " + optionalUser.get().getFullName());

        // Add filter state to model for "Back to Filter" button
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) session.getAttribute("userListFilters");
        boolean hasFilters = filters != null && hasActiveFilters(filters);
        model.addAttribute("hasFilters", hasFilters);

        return "manage-user";
    }

    @GetMapping("/user/create/details")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String createUser(UserDetailsForm userDetailsForm, HttpSession session, Model model) {
        EntraUserDto user = (EntraUserDto) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new EntraUserDto();
        }
        UserType selectedUserType = (UserType) session.getAttribute("selectedUserType");
        model.addAttribute("userTypes", List.of(UserType.EXTERNAL_SINGLE_FIRM_ADMIN, UserType.EXTERNAL_SINGLE_FIRM));
        model.addAttribute("selectedUserType", selectedUserType);
        // If user is already in session, populate the form with existing user details
        userDetailsForm = UserUtils.populateUserDetailsFormWithSession(userDetailsForm, user, session);
        model.addAttribute("userDetailsForm", userDetailsForm);
        model.addAttribute("user", user);

        // Store the model in session to handle validation errors later
        session.setAttribute("createUserDetailsModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add user details");
        return "add-user-details";
    }

    @PostMapping("/user/create/details")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String postUser(
            @Valid UserDetailsForm userDetailsForm, BindingResult result,
            HttpSession session, Model model) {

        EntraUserDto user = (EntraUserDto) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new EntraUserDto();
        }

        if (userService.userExistsByEmail(userDetailsForm.getEmail())) {
            result.rejectValue("email", "error.email", "Email address already exists");
        }

        // Validate selected User Type
        UserType selectedUserType = userDetailsForm.getUserType();
        // Add a check to stop users injecting internal user types into create external
        // user post request.
        if (selectedUserType != null && !UserType.EXTERNAL_TYPES.contains(selectedUserType)) {
            result.rejectValue("userType", "error.userType", "User type given must be Provider User or Provider Admin");
        }

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while creating user: {}", result.getAllErrors());

            Model modelFromSession = (Model) session.getAttribute("createUserDetailsModel");
            if (modelFromSession == null) {
                return "redirect:/admin/user/create/details";
            }

            model.addAttribute("userTypes", modelFromSession.getAttribute("userTypes"));
            model.addAttribute("selectedUserType", userDetailsForm.getUserType());
            model.addAttribute("user", modelFromSession.getAttribute("user"));
            return "add-user-details";
        }

        // Set user details from the form
        user.setFirstName(userDetailsForm.getFirstName());
        user.setLastName(userDetailsForm.getLastName());
        user.setFullName(userDetailsForm.getFirstName() + " " + userDetailsForm.getLastName());
        user.setEmail(userDetailsForm.getEmail());
        session.setAttribute("user", user);
        session.setAttribute("selectedUserType", userDetailsForm.getUserType());

        // Clear the createUserDetailsModel from session to avoid stale data
        session.removeAttribute("createUserDetailsModel");
        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        if (Objects.isNull(selectedFirm)) {
            return "redirect:/admin/user/create/firm";
        } else {
            return "redirect:/admin/user/create/check-answers";
        }
    }

    @GetMapping("/user/create/firm")
    public String createUserFirm(FirmSearchForm firmSearchForm, HttpSession session, Model model) {

        // If firmSearchForm is already populated from session (e.g., validation
        // errors), keep it
        FirmSearchForm existingForm = (FirmSearchForm) session.getAttribute("firmSearchForm");
        if (existingForm != null) {
            firmSearchForm = existingForm;
            session.removeAttribute("firmSearchForm");
        }

        model.addAttribute("firmSearchForm", firmSearchForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Select firm");
        return "add-user-firm";
    }

    @GetMapping("/user/create/firm/search")
    @ResponseBody
    public List<Map<String, String>> searchFirms(@RequestParam(value = "q", defaultValue = "") String query) {
        List<FirmDto> firms = firmService.searchFirms(query);

        List<Map<String, String>> result = firms.stream()
                .limit(10) // Limit results to prevent overwhelming the UI
                .map(firm -> {
                    Map<String, String> firmData = new HashMap<>();
                    firmData.put("id", firm.getId().toString());
                    firmData.put("name", firm.getName());
                    firmData.put("code", firm.getCode());
                    return firmData;
                })
                .collect(Collectors.toList());
        return result;
    }

    @PostMapping("/user/create/firm")
    public String postUserFirm(@Valid FirmSearchForm firmSearchForm, BindingResult result,
            HttpSession session, Model model) {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while searching for firm: {}", result.getAllErrors());
            // Store the form in session to preserve input on redirect
            session.setAttribute("firmSearchForm", firmSearchForm);
            return "add-user-firm";
        }
        FirmDto savedFirm = (FirmDto) session.getAttribute("firm");
        if (!Objects.isNull(savedFirm) && !savedFirm.getName().equals(firmSearchForm.getFirmSearch())) {
            firmSearchForm.setSelectedFirmId(null);
        }
        // Check if a specific firm was selected
        if (firmSearchForm.getSelectedFirmId() != null) {
            try {
                FirmDto selectedFirm = firmService.getFirm(firmSearchForm.getSelectedFirmId());
                session.setAttribute("firm", selectedFirm);
            } catch (Exception e) {
                log.error("Error retrieving selected firm: {}", e.getMessage());
                result.rejectValue("firmSearch", "error.firm", "Invalid firm selection. Please try again.");
                return "add-user-firm";
            }
        } else {
            // Fallback: search by name if no specific firm was selected
            List<FirmDto> firms = firmService.getAllFirmsFromCache();
            FirmDto selectedFirm = firms.stream()
                    .filter(firm -> firm.getName().toLowerCase().contains(firmSearchForm.getFirmSearch().toLowerCase()))
                    .findFirst()
                    .orElse(null);

            if (selectedFirm == null) {
                result.rejectValue("firmSearch", "error.firm",
                        "No firm found with that name. Please select from the dropdown.");
                return "add-user-firm";
            }
            firmSearchForm.setFirmSearch(selectedFirm.getName());
            firmSearchForm.setSelectedFirmId(selectedFirm.getId());
            session.setAttribute("firm", selectedFirm);
        }
        session.setAttribute("firmSearchForm", firmSearchForm);
        return "redirect:/admin/user/create/check-answers";
    }

    @GetMapping("/user/create/services")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
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
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Select services");
        return "add-user-apps";
    }

    @PostMapping("/user/create/services")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
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
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String getSelectedRoles(RolesForm rolesForm, Authentication authentication, Model model,
            HttpSession session) {
        List<String> selectedApps = getListFromHttpSession(session, "apps", String.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        Model modelFromSession = (Model) session.getAttribute("userCreateRolesModel");
        Integer selectedAppIndex;
        if (modelFromSession != null && modelFromSession.getAttribute("createUserRolesSelectedAppIndex") != null) {
            selectedAppIndex = (Integer) modelFromSession.getAttribute("createUserRolesSelectedAppIndex");
        } else {
            selectedAppIndex = 0;
        }
        // TODO: Make this use the selected user type rather than a hard-coded type. Our
        // user creation flow is only for external users right now.
        List<AppRoleDto> roles = userService.getAppRolesByAppIdAndUserType(selectedApps.get(selectedAppIndex),
                UserType.EXTERNAL_SINGLE_FIRM);
        UserProfile editorProfile = loginService.getCurrentProfile(authentication);
        roles = roleAssignmentService.filterRoles(editorProfile.getAppRoles(), roles);
        List<String> selectedRoles = getListFromHttpSession(session, "roles", String.class).orElseGet(ArrayList::new);
        AppDto currentApp = userService.getAppByAppId(selectedApps.get(selectedAppIndex)).orElseThrow();
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
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Select roles");
        return "add-user-roles";
    }

    @PostMapping("/user/create/roles")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
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
        @SuppressWarnings("unchecked")
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
            List<String> allSelectedRoles = allSelectedRolesByPage.values().stream().filter(Objects::nonNull)
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
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String offices(OfficesForm officesForm, HttpSession session, Model model) {
        OfficeData selectedOfficeData = getObjectFromHttpSession(session, "officeData", OfficeData.class)
                .orElseGet(OfficeData::new);
        FirmDto selectedFirm = getObjectFromHttpSession(session, "firm", FirmDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        List<Office> offices = officeService.getOfficesByFirms(List.of(selectedFirm.getId()));
        List<OfficeModel> officeData = offices.stream()
                .map(office -> new OfficeModel(office.getCode(),
                        new OfficeModel.Address(office.getAddress().getAddressLine1(),
                                office.getAddress().getAddressLine2(), office.getAddress().getCity(),
                                office.getAddress().getPostcode()),
                        office.getId().toString(), Objects.nonNull(selectedOfficeData.getSelectedOffices())
                                && selectedOfficeData.getSelectedOffices().contains(office.getId().toString())))
                .collect(Collectors.toList());
        model.addAttribute("officeData", officeData);
        EntraUserDto user = getObjectFromHttpSession(session, "user", EntraUserDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        model.addAttribute("user", user);

        // Store the model in session to handle validation errors later
        session.setAttribute("createUserOfficesModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Select offices");
        return "add-user-offices";
    }

    @PostMapping("/user/create/offices")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
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
                selectedDisplayNames.add(office.getCode());
            }
        }
        officeData.setSelectedOfficesDisplay(selectedDisplayNames);
        session.setAttribute("officeData", officeData);

        // Clear the createUserOfficesModel from session to avoid stale data
        session.removeAttribute("createUserOfficesModel");
        return "redirect:/admin/user/create/check-answers";
    }

    @GetMapping("/user/create/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String getUserCheckAnswers(Model model, HttpSession session) {
        EntraUserDto user = getObjectFromHttpSession(session, "user", EntraUserDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        model.addAttribute("user", user);

        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        model.addAttribute("firm", selectedFirm);

        UserType userType = (UserType) session.getAttribute("selectedUserType");
        model.addAttribute("userType", userType);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Check your answers");
        return "add-user-check-answers";
    }

    @PostMapping("/user/create/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String addUserCheckAnswers(HttpSession session, Authentication authentication) {
        Optional<EntraUserDto> userOptional = getObjectFromHttpSession(session, "user", EntraUserDto.class);
        Optional<FirmDto> firmOptional = Optional.ofNullable((FirmDto) session.getAttribute("firm"));
        UserType userType = getObjectFromHttpSession(session, "selectedUserType", UserType.class).orElseThrow();

        if (userOptional.isPresent()) {
            EntraUserDto user = userOptional.get();
            FirmDto selectedFirm = firmOptional.orElseGet(FirmDto::new);
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            EntraUser entraUser = userService.createUser(user, selectedFirm,
                    userType, currentUserDto.getName());
            session.setAttribute("userProfile",
                    mapper.map(entraUser.getUserProfiles().stream().findFirst(), UserProfileDto.class));
            CreateUserAuditEvent createUserAuditEvent = new CreateUserAuditEvent(currentUserDto, entraUser,
                    selectedFirm.getName(), userType);
            eventService.logEvent(createUserAuditEvent);
        } else {
            log.error("No user attribute was present in request. User not created.");
        }

        session.removeAttribute("firm");
        session.removeAttribute("selectedUserType");

        return "redirect:/admin/user/create/confirmation";
    }

    @GetMapping("/user/create/confirmation")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String addUserCreated(Model model, HttpSession session) {
        Optional<EntraUserDto> userOptional = getObjectFromHttpSession(session, "user", EntraUserDto.class);
        Optional<UserProfileDto> userProfileOptional = getObjectFromHttpSession(session, "userProfile",
                UserProfileDto.class);
        if (userOptional.isPresent() && userProfileOptional.isPresent()) {
            EntraUserDto user = userOptional.get();
            model.addAttribute("user", user);
            model.addAttribute("userProfile", userProfileOptional.get());
        } else {
            log.error("No user attribute was present in request. User not added to model.");
        }
        session.removeAttribute("user");
        session.removeAttribute("userProfile");
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User created");
        return "add-user-created";
    }

    @GetMapping("/user/create/cancel")
    public String cancelUserCreation(HttpSession session) {
        session.removeAttribute("user");
        session.removeAttribute("firm");
        session.removeAttribute("selectedUserType");
        session.removeAttribute("isFirmAdmin");
        session.removeAttribute("apps");
        session.removeAttribute("roles");
        session.removeAttribute("officeData");
        session.removeAttribute("firmSearchForm");
        session.removeAttribute("firmSearchTerm");
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
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_DETAILS) && @accessControlService.canEditUser(#id)")
    public String editUserDetails(@PathVariable String id, Model model) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        EditUserDetailsForm editUserDetailsForm = new EditUserDetailsForm();
        editUserDetailsForm.setFirstName(user.getEntraUser().getFirstName());
        editUserDetailsForm.setLastName(user.getEntraUser().getLastName());
        editUserDetailsForm.setEmail(user.getEntraUser().getEmail());
        model.addAttribute("editUserDetailsForm", editUserDetailsForm);
        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Edit user details - " + user.getFullName());
        return "edit-user-details";
    }

    /**
     * Update user details
     * 
     * @param id                  User ID
     * @param editUserDetailsForm User details form
     * @param result              Binding result for validation errors
     * @param session             HttpSession to store user details
     * @return Redirect to user management page
     * @throws IOException              If an error occurs during user update
     * @throws IllegalArgumentException If the user ID is invalid or not found
     */
    @PostMapping("/users/edit/{id}/details")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_DETAILS) && @accessControlService.canEditUser(#id)")
    public String updateUserDetails(@PathVariable String id,
            @Valid EditUserDetailsForm editUserDetailsForm, BindingResult result,
            HttpSession session) throws IOException {
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while updating user details: {}", result.getAllErrors());
            // If there are validation errors, return to the edit user details page with
            // errors
            UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
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
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String editUserApps(@PathVariable String id, Model model) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        UserType userType = user.getUserType();
        Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(id);
        List<AppDto> availableApps = userService.getAppsByUserType(userType);

        // Add selected attribute to available apps based on user assigned apps
        availableApps.forEach(app -> {
            app.setSelected(userAssignedApps.stream()
                    .anyMatch(userApp -> userApp.getId().equals(app.getId())));
        });

        model.addAttribute("user", user);
        model.addAttribute("apps", availableApps);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Edit user services - " + user.getFullName());

        return "edit-user-apps";
    }

    @PostMapping("/users/edit/{id}/apps")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public RedirectView setSelectedAppsEdit(@PathVariable String id,
            @RequestParam(value = "apps", required = false) List<String> apps,
            Authentication authentication,
            HttpSession session) {
        // Handle case where no apps are selected (apps will be null)
        List<String> selectedApps = apps != null ? apps : new ArrayList<>();
        session.setAttribute("selectedApps", selectedApps);

        if (selectedApps.isEmpty()) {
            // Update user to have no roles (empty list)
            String changed = userService.updateUserRoles(id, new ArrayList<>());
            UserProfileDto userProfileDto = userService.getUserProfileById(id).orElse(null);
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(currentUserDto,
                    userProfileDto != null ? userProfileDto.getEntraUser() : null,
                    changed, "apps");
            eventService.logEvent(updateUserAuditEvent);
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
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String editUserRoles(@PathVariable String id,
            @RequestParam(defaultValue = "0") Integer selectedAppIndex,
            RolesForm rolesForm,
            Authentication authentication,
            Model model, HttpSession session) {

        final UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
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

        List<AppRoleDto> roles = userService.getAppRolesByAppIdAndUserType(selectedApps.get(currentSelectedAppIndex),
                user.getUserType());
        UserProfile editorProfile = loginService.getCurrentProfile(authentication);
        roles = roleAssignmentService.filterRoles(editorProfile.getAppRoles(), roles);
        List<AppRoleDto> userRoles = userService.getUserAppRolesByUserId(id);
        // Get currently selected roles from session or use user's existing roles
        List<String> selectedRoles = getListFromHttpSession(session, "editUserRoles", String.class)
                .orElseGet(() -> userRoles.stream().map(AppRoleDto::getId).collect(Collectors.toList()));
        AppDto currentApp = userService.getAppByAppId(selectedApps.get(currentSelectedAppIndex)).orElseThrow();

        List<AppRoleViewModel> appRoleViewModels = roles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).toList();

        // Check if this is the CCMS app and organize roles by section
        boolean isCcmsApp = (currentApp.getName().contains("CCMS")
                && !currentApp.getName().contains("Requests to transfer CCMS cases"))
                || roles.stream().anyMatch(role -> CcmsRoleGroupsUtil.isCcmsRole(role.getCcmsCode()));

        if (isCcmsApp) {
            // Filter to only CCMS roles for organization
            List<AppRoleDto> ccmsRoles = roles.stream()
                    .filter(role -> CcmsRoleGroupsUtil.isCcmsRole(role.getCcmsCode()))
                    .collect(Collectors.toList());

            Map<String, List<AppRoleDto>> organizedRoles = new HashMap<>();
            if (!ccmsRoles.isEmpty()) {
                // Organize CCMS roles by section dynamically
                organizedRoles.putAll(CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles));
            }
            model.addAttribute("ccmsRolesBySection", organizedRoles);
            model.addAttribute("isCcmsApp", true);
        } else {
            model.addAttribute("isCcmsApp", false);
        }

        model.addAttribute("user", user);
        model.addAttribute("roles", appRoleViewModels);
        model.addAttribute("editUserRolesSelectedAppIndex", currentSelectedAppIndex);
        model.addAttribute("editUserRolesCurrentApp", currentApp);

        // Store the model in session to handle validation errors later and track
        // currently selected app.
        session.setAttribute("userEditRolesModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Edit user roles - " + user.getFullName());
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
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String updateUserRoles(@PathVariable String id,
            @Valid RolesForm rolesForm, BindingResult result,
            @RequestParam int selectedAppIndex,
            Authentication authentication,
            Model model, HttpSession session) {
        Model modelFromSession = (Model) session.getAttribute("userEditRolesModel");

        // Ensure passed in ID is a valid UUID to avoid open redirects.
        UUID uuid = UUID.fromString(id);

        if (modelFromSession == null) {
            return "redirect:/admin/users/edit/" + uuid + "/roles";
        }
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while setting user roles: {}", result.getAllErrors());
            // If there are validation errors, return to the roles page with errors
            // and role unseleected if it is not in the list
            @SuppressWarnings("unchecked")
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
            if (roles != null) {
                model.addAttribute("roles", roles);
            }

            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("editUserRolesSelectedAppIndex",
                    modelFromSession.getAttribute("editUserRolesSelectedAppIndex"));
            model.addAttribute("editUserRolesCurrentApp", modelFromSession.getAttribute("editUserRolesCurrentApp"));

            return "edit-user-roles";
        }

        UserProfileDto user = userService.getUserProfileById(id).orElse(null);
        List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class)
                .orElseGet(ArrayList::new);
        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("editUserAllSelectedRoles");
        if (allSelectedRolesByPage == null) {
            allSelectedRolesByPage = new HashMap<>();
        }
        // Add the roles for the currently selected app to a map for lookup.
        if (rolesForm.getRoles() != null) {
            allSelectedRolesByPage.put(selectedAppIndex, rolesForm.getRoles());
        }
        if (selectedAppIndex >= selectedApps.size() - 1) {
            // Clear the userEditRolesModel and page roles from session to avoid stale data
            session.removeAttribute("userEditRolesModel");
            session.removeAttribute("editUserAllSelectedRoles");
            // Flatten the map to a single list of all selected roles across all pages.
            List<String> allSelectedRoles = allSelectedRolesByPage.values().stream().filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .toList();
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            UserProfile editorProfile = loginService.getCurrentProfile(authentication);
            if (roleAssignmentService.canAssignRole(editorProfile.getAppRoles(), allSelectedRoles)) {
                String changed = userService.updateUserRoles(id, allSelectedRoles);
                UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(currentUserDto,
                        user != null ? user.getEntraUser() : null, changed,
                        "role");
                eventService.logEvent(updateUserAuditEvent);
            }
            return "redirect:/admin/users/manage/" + uuid;
        } else {
            modelFromSession.addAttribute("editUserRolesSelectedAppIndex", selectedAppIndex + 1);
            session.setAttribute("editUserAllSelectedRoles", allSelectedRolesByPage);
            session.setAttribute("userEditRolesModel", modelFromSession);

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
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_OFFICE) && @accessControlService.canEditUser(#id)")
    public String editUserOffices(@PathVariable String id, Model model, HttpSession session) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();

        // Get user's current offices
        List<OfficeDto> userOffices = userService.getUserOfficesByUserId(id);
        Set<String> userOfficeIds = userOffices.stream()
                .map(office -> office.getId().toString())
                .collect(Collectors.toSet());

        // Get user's available offices by firm
        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(id);
        List<UUID> firmIds = userFirms.stream().map(FirmDto::getId).collect(Collectors.toList());
        List<Office> allOffices = officeService.getOfficesByFirms(firmIds);

        // Check if user has access to all offices
        boolean hasAllOffices = userOffices.isEmpty();

        final List<OfficeModel> officeData = allOffices.stream()
                .map(office -> new OfficeModel(
                        office.getCode(),
                        new OfficeModel.Address(office.getAddress().getAddressLine1(),
                                office.getAddress().getAddressLine2(),
                                office.getAddress().getCity(), office.getAddress().getPostcode()),
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
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Edit user offices - " + user.getFullName());
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
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_OFFICE) && @accessControlService.canEditUser(#id)")
    public String updateUserOffices(@PathVariable String id,
            @Valid OfficesForm officesForm, BindingResult result,
            Authentication authentication,
            Model model, HttpSession session) throws IOException {
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while updating user offices: {}", result.getAllErrors());
            // If there are validation errors, return to the edit user offices page with
            // errors
            Model modelFromSession = (Model) session.getAttribute("editUserOfficesModel");
            if (modelFromSession == null) {
                return "redirect:/admin/users/edit/" + id + "/offices";
            }
            @SuppressWarnings("unchecked")
            List<OfficeModel> officeData = (List<OfficeModel>) modelFromSession.getAttribute("officeData");

            // make sure selected offices are not selected if validation errors occur
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
        List<String> selectOfficesDisplay = new ArrayList<>();
        // Handle "ALL" option
        if (!selectedOffices.contains("ALL")) {
            // If "ALL" is selected, get all available offices by firm
            Model modelFromSession = (Model) session.getAttribute("editUserOfficesModel");
            if (modelFromSession != null) {
                @SuppressWarnings("unchecked")
                List<OfficeModel> officeData = (List<OfficeModel>) modelFromSession.getAttribute("officeData");
                if (officeData != null) {
                    List<String> selectedOfficeIds = officesForm.getOffices() != null ? officesForm.getOffices()
                            : new ArrayList<>();
                    for (OfficeModel office : officeData) {
                        if (selectedOfficeIds.contains(office.getId())) {
                            selectOfficesDisplay.add(office.getCode());
                        }
                    }
                }
            }
        }

        userService.updateUserOffices(id, selectedOffices);
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UserProfileDto userProfileDto = userService.getUserProfileById(id).orElse(null);
        UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(currentUserDto,
                userProfileDto != null ? userProfileDto.getEntraUser() : null,
                selectOfficesDisplay, "office");
        eventService.logEvent(updateUserAuditEvent);
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

    /**
     * Grant access to a user by updating their profile status to COMPLETE
     */
    @PostMapping("/users/manage/{id}/grant-access")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantUserAccess(@PathVariable String id) {
        return "redirect:/admin/users/grant-access/" + id + "/apps";
    }

    /**
     * Grant Access Flow - Retrieves available apps for user and their currently
     * assigned apps.
     */
    @GetMapping("/users/grant-access/{id}/apps")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantAccessEditUserApps(@PathVariable String id, ApplicationsForm applicationsForm, Model model,
            HttpSession session) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        UserType userType = user.getUserType();
        Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(id);
        List<AppDto> availableApps = userService.getAppsByUserType(userType);

        // Add selected attribute to available apps based on user assigned apps
        availableApps.forEach(app -> {
            app.setSelected(userAssignedApps.stream()
                    .anyMatch(userApp -> userApp.getId().equals(app.getId())));
        });

        model.addAttribute("user", user);
        model.addAttribute("apps", availableApps);

        // Store the model in session to handle validation errors later
        session.setAttribute("grantAccessUserAppsModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Grant access - Select services - " + user.getFullName());
        return "grant-access-user-apps";
    }

    @PostMapping("/users/grant-access/{id}/apps")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantAccessSetSelectedApps(@PathVariable String id,
            @Valid ApplicationsForm applicationsForm, BindingResult result,
            Authentication authentication,
            Model model, HttpSession session) {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while selecting apps: {}", result.getAllErrors());
            // If there are validation errors, return to the apps page with errors
            Model modelFromSession = (Model) session.getAttribute("grantAccessUserAppsModel");
            if (modelFromSession == null) {
                // If no model in session, redirect to apps page to repopulate
                return "redirect:/admin/users/grant-access/" + id + "/apps";
            }

            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("apps", modelFromSession.getAttribute("apps"));
            return "grant-access-user-apps";
        }

        // Handle case where no apps are selected (apps will be null)
        List<String> selectedApps = applicationsForm.getApps() != null ? applicationsForm.getApps() : new ArrayList<>();
        session.setAttribute("grantAccessSelectedApps", selectedApps);

        // Clear the grantAccessUserAppsModel from session to avoid stale data
        session.removeAttribute("grantAccessUserAppsModel");

        // If no apps are selected, persist empty roles to database and redirect to
        // manage user page
        if (selectedApps.isEmpty()) {
            // Update user to have no roles (empty list)
            String changed = userService.updateUserRoles(id, new ArrayList<>());
            UserProfileDto userProfileDto = userService.getUserProfileById(id).orElse(null);
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(currentUserDto,
                    userProfileDto != null ? userProfileDto.getEntraUser() : null,
                    changed, "roles");
            eventService.logEvent(updateUserAuditEvent);
            // Ensure passed in ID is a valid UUID to avoid open redirects.
            UUID uuid = UUID.fromString(id);
            return "redirect:/admin/users/manage/" + uuid;
        }

        // Ensure passed in ID is a valid UUID to avoid open redirects.
        UUID uuid = UUID.fromString(id);
        return "redirect:/admin/users/grant-access/" + uuid + "/roles";
    }

    /**
     * Grant Access Flow - Retrieves available roles for user and their currently
     * assigned roles.
     */
    @GetMapping("/users/grant-access/{id}/roles")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantAccessEditUserRoles(@PathVariable String id,
            @RequestParam(defaultValue = "0") Integer selectedAppIndex,
            RolesForm rolesForm,
            Authentication authentication,
            Model model, HttpSession session) {

        final UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        List<String> selectedApps = getListFromHttpSession(session, "grantAccessSelectedApps", String.class)
                .orElseGet(() -> {
                    // If no selectedApps in session, get user's current apps
                    Set<AppDto> userApps = userService.getUserAppsByUserId(id);
                    List<String> userAppIds = userApps.stream()
                            .map(AppDto::getId)
                            .collect(Collectors.toList());
                    session.setAttribute("grantAccessSelectedApps", userAppIds);
                    return userAppIds;
                });

        // Ensure the selectedAppIndex is within bounds
        if (selectedApps.isEmpty()) {
            // No apps assigned to user, redirect back to manage page
            return "redirect:/admin/users/manage/" + id;
        }

        Model modelFromSession = (Model) session.getAttribute("grantAccessUserRolesModel");
        Integer currentSelectedAppIndex;
        if (modelFromSession != null && modelFromSession.getAttribute("grantAccessSelectedAppIndex") != null) {
            currentSelectedAppIndex = (Integer) modelFromSession.getAttribute("grantAccessSelectedAppIndex");
        } else {
            currentSelectedAppIndex = selectedAppIndex != null ? selectedAppIndex : 0;
        }

        // Ensure the index is within bounds
        if (currentSelectedAppIndex >= selectedApps.size()) {
            currentSelectedAppIndex = 0;
        }

        List<AppRoleDto> roles = userService.getAppRolesByAppIdAndUserType(selectedApps.get(currentSelectedAppIndex),
                user.getUserType());
        UserProfile editorProfile = loginService.getCurrentProfile(authentication);
        roles = roleAssignmentService.filterRoles(editorProfile.getAppRoles(), roles);
        List<AppRoleDto> userRoles = userService.getUserAppRolesByUserId(id);

        AppDto currentApp = userService.getAppByAppId(selectedApps.get(currentSelectedAppIndex)).orElseThrow();
        // Get currently selected roles from session or use user's existing roles
        List<String> selectedRoles = getListFromHttpSession(session, "grantAccessUserRoles", String.class)
                .orElseGet(() -> userRoles.stream().map(AppRoleDto::getId).collect(Collectors.toList()));

        List<AppRoleViewModel> appRoleViewModels = roles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).toList();

        // Check if this is the CCMS app and organize roles by section
        boolean isCcmsApp = (currentApp.getName().contains("CCMS")
                && !currentApp.getName().contains("Requests to transfer CCMS cases"))
                || roles.stream().anyMatch(role -> CcmsRoleGroupsUtil.isCcmsRole(role.getCcmsCode()));

        if (isCcmsApp) {
            // Filter to only CCMS roles for organization
            List<AppRoleDto> ccmsRoles = roles.stream()
                    .filter(role -> CcmsRoleGroupsUtil.isCcmsRole(role.getCcmsCode()))
                    .collect(Collectors.toList());

            if (!ccmsRoles.isEmpty()) {
                // Organize CCMS roles by section dynamically
                Map<String, List<AppRoleDto>> organizedRoles = CcmsRoleGroupsUtil.organizeCcmsRolesBySection(ccmsRoles);
                model.addAttribute("ccmsRolesBySection", organizedRoles);
            }
            model.addAttribute("isCcmsApp", true);
        } else {
            model.addAttribute("isCcmsApp", false);
        }

        model.addAttribute("user", user);
        model.addAttribute("roles", appRoleViewModels);
        model.addAttribute("grantAccessSelectedAppIndex", currentSelectedAppIndex);
        model.addAttribute("grantAccessCurrentApp", currentApp);

        // Store the model in session to handle validation errors later and track
        // currently selected app.
        session.setAttribute("grantAccessUserRolesModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Grant access - Select roles - " + user.getFullName());
        return "grant-access-user-roles";
    }

    /**
     * Grant Access Flow - Update user roles for a specific app.
     */
    @PostMapping("/users/grant-access/{id}/roles")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantAccessUpdateUserRoles(@PathVariable String id,
            @Valid RolesForm rolesForm, BindingResult result,
            @RequestParam int selectedAppIndex,
            Authentication authentication,
            Model model, HttpSession session) {
        Model modelFromSession = (Model) session.getAttribute("grantAccessUserRolesModel");
        if (modelFromSession == null) {
            return "redirect:/admin/users/grant-access/" + id + "/roles";
        }
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while setting user roles: {}", result.getAllErrors());
            // If there are validation errors, return to the roles page with errors
            @SuppressWarnings("unchecked")
            List<AppRoleViewModel> roles = (List<AppRoleViewModel>) modelFromSession.getAttribute("roles");
            if (roles != null) {
                List<String> selectedRoleIds = rolesForm.getRoles() != null ? rolesForm.getRoles() : new ArrayList<>();
                roles.forEach(role -> {
                    if (!selectedRoleIds.contains(role.getId())) {
                        role.setSelected(false);
                    }
                });
            }
            model.addAttribute("roles", roles);
            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("grantAccessSelectedAppIndex",
                    modelFromSession.getAttribute("grantAccessSelectedAppIndex"));
            model.addAttribute("grantAccessCurrentApp", modelFromSession.getAttribute("grantAccessCurrentApp"));

            return "grant-access-user-roles";
        }

        UserProfileDto user = userService.getUserProfileById(id).orElse(null);
        List<String> selectedApps = getListFromHttpSession(session, "grantAccessSelectedApps", String.class)
                .orElseGet(ArrayList::new);
        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("grantAccessAllSelectedRoles");
        if (allSelectedRolesByPage == null) {
            allSelectedRolesByPage = new HashMap<>();
        }
        // Add the roles for the currently selected app to a map for lookup.
        allSelectedRolesByPage.put(selectedAppIndex, rolesForm.getRoles());
        if (selectedAppIndex >= selectedApps.size() - 1) {
            // Clear the grantAccessUserRolesModel and page roles from session to avoid
            // stale data
            session.removeAttribute("grantAccessUserRolesModel");
            session.removeAttribute("grantAccessAllSelectedRoles");
            // Flatten the map to a single list of all selected roles across all pages.
            List<String> allSelectedRoles = allSelectedRolesByPage.values().stream().filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .toList();
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            UserProfile editorProfile = loginService.getCurrentProfile(authentication);
            if (roleAssignmentService.canAssignRole(editorProfile.getAppRoles(), allSelectedRoles)) {
                String changed = userService.updateUserRoles(id, allSelectedRoles);
                UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(currentUserDto,
                        user != null ? user.getEntraUser() : null, changed,
                        "role");
                eventService.logEvent(updateUserAuditEvent);
            }
            return "redirect:/admin/users/grant-access/" + id + "/offices";
        } else {
            modelFromSession.addAttribute("grantAccessSelectedAppIndex", selectedAppIndex + 1);
            session.setAttribute("grantAccessAllSelectedRoles", allSelectedRolesByPage);
            session.setAttribute("grantAccessUserRolesModel", modelFromSession);
            // Ensure passed in ID is a valid UUID to avoid open redirects.
            UUID uuid = UUID.fromString(id);
            return "redirect:/admin/users/grant-access/" + uuid + "/roles?selectedAppIndex=" + (selectedAppIndex + 1);
        }
    }

    /**
     * Grant Access Flow - Get user offices for editing
     */
    @GetMapping("/users/grant-access/{id}/offices")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_OFFICE) && @accessControlService.canEditUser(#id)")
    public String grantAccessEditUserOffices(@PathVariable String id, Model model, HttpSession session) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();

        // Get user's current offices
        List<OfficeDto> userOffices = userService.getUserOfficesByUserId(id);
        Set<String> userOfficeIds = userOffices.stream()
                .map(office -> office.getId().toString())
                .collect(Collectors.toSet());

        // Get user's available offices by firm
        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(id);
        List<UUID> firmIds = userFirms.stream().map(FirmDto::getId).collect(Collectors.toList());
        List<Office> allOffices = officeService.getOfficesByFirms(firmIds);

        // Check if user has access to all offices
        boolean hasAllOffices = userOffices.isEmpty();

        final List<OfficeModel> officeData = allOffices.stream()
                .map(office -> new OfficeModel(
                        office.getCode(),
                        OfficeModel.Address.builder().addressLine1(office.getAddress().getAddressLine1())
                                .addressLine2(office.getAddress().getAddressLine2()).city(office.getAddress().getCity())
                                .postcode(office.getAddress().getPostcode()).build(),
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
        session.setAttribute("grantAccessUserOfficesModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Grant access - Select offices - " + user.getFullName());
        return "grant-access-user-offices";
    }

    /**
     * Grant Access Flow - Update user offices
     */
    @PostMapping("/users/grant-access/{id}/offices")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_OFFICE) && @accessControlService.canEditUser(#id)")
    public String grantAccessUpdateUserOffices(@PathVariable String id,
            @Valid OfficesForm officesForm, BindingResult result,
            Authentication authentication,
            Model model, HttpSession session) throws IOException {
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while updating user offices: {}", result.getAllErrors());
            // If there are validation errors, return to the edit user offices page with
            // errors
            Model modelFromSession = (Model) session.getAttribute("grantAccessUserOfficesModel");
            if (modelFromSession == null) {
                return "redirect:/admin/users/grant-access/" + id + "/offices";
            }
            @SuppressWarnings("unchecked")
            List<OfficeModel> officeData = (List<OfficeModel>) modelFromSession.getAttribute("officeData");

            // make sure selected offices are not selected if validation errors occur
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
            return "grant-access-user-offices";
        }

        // Update user offices
        List<String> selectedOffices = officesForm.getOffices() != null ? officesForm.getOffices() : new ArrayList<>();
        List<String> selectOfficesDisplay = new ArrayList<>();
        // Handle "ALL" option
        if (!selectedOffices.contains("ALL")) {
            Model modelFromSession = (Model) session.getAttribute("grantAccessUserOfficesModel");
            if (modelFromSession != null) {
                @SuppressWarnings("unchecked")
                List<OfficeModel> officeData = (List<OfficeModel>) modelFromSession.getAttribute("officeData");
                if (officeData != null) {
                    List<String> selectedOfficeIds = officesForm.getOffices() != null ? officesForm.getOffices()
                            : new ArrayList<>();
                    for (OfficeModel office : officeData) {
                        if (selectedOfficeIds.contains(office.getId())) {
                            selectOfficesDisplay.add(office.getCode());
                        }
                    }
                }
            }
        }

        userService.updateUserOffices(id, selectedOffices);
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UserProfileDto userProfileDto = userService.getUserProfileById(id).orElse(null);
        UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(currentUserDto,
                userProfileDto != null ? userProfileDto.getEntraUser() : null,
                selectOfficesDisplay, "office");
        eventService.logEvent(updateUserAuditEvent);

        // Clear grant access session data
        session.removeAttribute("grantAccessUserOfficesModel");
        session.removeAttribute("grantAccessSelectedApps");
        session.removeAttribute("grantAccessUserRoles");
        session.removeAttribute("grantAccessUserRolesModel");
        session.removeAttribute("grantAccessAllSelectedRoles");

        return "redirect:/admin/users/grant-access/" + id + "/check-answers";
    }

    /**
     * Grant Access Flow - Check answers page
     */
    @GetMapping("/users/grant-access/{id}/check-answers")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantAccessCheckAnswers(@PathVariable String id, Model model) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();

        // Get user's current app roles
        List<AppRoleDto> userAppRoles = userService.getUserAppRolesByUserId(id);

        // Group roles by app name and sort by app name
        Map<String, List<AppRoleDto>> groupedAppRoles = userAppRoles.stream()
                .collect(Collectors.groupingBy(
                        appRole -> appRole.getApp().getName(),
                        LinkedHashMap::new, // Preserve insertion order
                        Collectors.toList()));

        // Sort the map by app name
        Map<String, List<AppRoleDto>> sortedGroupedAppRoles = groupedAppRoles.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));

        // Get user's current offices
        List<OfficeDto> userOffices = userService.getUserOfficesByUserId(id);

        model.addAttribute("user", user);
        model.addAttribute("userAppRoles", userAppRoles);
        model.addAttribute("groupedAppRoles", sortedGroupedAppRoles);
        model.addAttribute("userOffices", userOffices);
        model.addAttribute("externalUser", UserType.EXTERNAL_TYPES.contains(user.getUserType()));
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Grant access - Check your answers - " + user.getFullName());

        return "grant-access-check-answers";
    }

    /**
     * Grant Access Flow - Remove an app role from user
     */
    @GetMapping("/users/grant-access/{userId}/remove-app-role/{appId}/{roleName}")
    @PreAuthorize("@accessControlService.canEditUser(#userId)")
    public String removeAppRole(@PathVariable String userId, @PathVariable String appId, @PathVariable String roleName,
            Authentication authentication) {
        try {
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

            // Remove the app role from the user
            userService.removeUserAppRole(userId, appId, roleName);

            // Create audit event for the app role removal
            UserProfileDto userProfileDto = userService.getUserProfileById(userId).orElseThrow();
            UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
                    currentUserDto,
                    userProfileDto.getEntraUser(),
                    List.of("Removed app role: " + roleName + " for app: " + appId),
                    "app_role_removed");
            eventService.logEvent(updateUserAuditEvent);

        } catch (Exception e) {
            log.error("Error removing app role for user: " + userId, e);
        }

        UUID uuid = UUID.fromString(userId);

        return "redirect:/admin/users/grant-access/" + uuid + "/check-answers";
    }

    /**
     * Grant Access Flow - Process check answers and complete grant
     */
    @PostMapping("/users/grant-access/{id}/check-answers")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantAccessProcessCheckAnswers(@PathVariable String id, Authentication authentication,
            HttpSession session) {
        try {
            UserProfileDto userProfileDto = userService.getUserProfileById(id).orElseThrow();
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

            // Update user profile status to COMPLETE to finalize access grant
            userService.grantAccess(id, currentUserDto.getName());

            // Create audit event for the final access grant
            UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
                    currentUserDto,
                    userProfileDto.getEntraUser(),
                    List.of("Access granted"),
                    "access_grant_complete");
            eventService.logEvent(updateUserAuditEvent);

        } catch (Exception e) {
            log.error("Error completing grant access for user: " + id, e);
            // Could add error handling here if needed
        }

        // Clear grant access session data
        session.removeAttribute("grantAccessUserOfficesModel");
        session.removeAttribute("grantAccessSelectedApps");
        session.removeAttribute("grantAccessUserRoles");
        session.removeAttribute("grantAccessUserRolesModel");
        session.removeAttribute("grantAccessAllSelectedRoles");

        return "redirect:/admin/users/grant-access/" + id + "/confirmation";
    }

    /**
     * Grant Access Flow - Show confirmation page
     */
    @GetMapping("/users/grant-access/{id}/confirmation")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantAccessConfirmation(@PathVariable String id, Model model) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Access granted - " + user.getFullName());
        return "grant-access-confirmation";
    }

    /**
     * Cancel the grant access flow and clean up session data
     *
     * @param id      User ID
     * @param session HttpSession to clear grant access data
     * @return Redirect to user management page
     */
    @GetMapping("/users/grant-access/{id}/cancel")
    public String cancelGrantAccess(@PathVariable String id, HttpSession session) {
        // Clear all grant access related session attributes
        session.removeAttribute("grantAccessSelectedApps");
        session.removeAttribute("grantAccessUserRoles");
        session.removeAttribute("grantAccessUserRolesModel");
        session.removeAttribute("grantAccessAllSelectedRoles");
        session.removeAttribute("grantAccessUserOfficesModel");
        session.removeAttribute("grantAccessUserAppsModel");

        // Clear any success messages
        session.removeAttribute("successMessage");

        return "redirect:/admin/users/manage/" + id;
    }

    /**
     * Handle authorization exceptions when user lacks permissions to access
     * specific users
     */
    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public RedirectView handleAuthorizationException(Exception ex, HttpSession session) {
        log.warn("Authorization denied while accessing user: {}", ex.getMessage());
        return new RedirectView("/not-authorised");
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public RedirectView handleException(Exception ex) {
        log.error("Error while user management", ex);
        return new RedirectView("/error");
    }

    private Map<String, Object> processRequestFilters(int size, int page, String sort, String direction,
            String usertype, String search, boolean showFirmAdmins,
                                                      boolean backButton, HttpSession session, FirmSearchForm firmSearchForm) {

        if (backButton) {
            // Use session filters when back button is used
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionFilters = (Map<String, Object>) session.getAttribute("userListFilters");

            if (sessionFilters != null) {
                size = sessionFilters.containsKey("size") ? (Integer) sessionFilters.get("size") : size;
                page = sessionFilters.containsKey("page") ? (Integer) sessionFilters.get("page") : page;
                sort = sessionFilters.containsKey("sort") ? (String) sessionFilters.get("sort") : sort;
                direction = sessionFilters.containsKey("direction") ? (String) sessionFilters.get("direction")
                        : direction;
                usertype = sessionFilters.containsKey("usertype") ? (String) sessionFilters.get("usertype") : usertype;
                search = sessionFilters.containsKey("search") ? (String) sessionFilters.get("search") : search;
                showFirmAdmins = sessionFilters.containsKey("showFirmAdmins")
                        ? (Boolean) sessionFilters.get("showFirmAdmins")
                        : showFirmAdmins;
                firmSearchForm = sessionFilters.containsKey("firmSearchForm")
                        ? (FirmSearchForm) sessionFilters.get("firmSearchForm")
                        : firmSearchForm;

                // Handle empty strings for optional parameters
                if (sort != null && sort.isEmpty()) {
                    sort = null;
                }
                if (direction != null && direction.isEmpty()) {
                    direction = null;
                }
                if (usertype != null && usertype.isEmpty()) {
                    usertype = null;
                }
            }
        } else {
            // Clear session filters when not using back button (new filter request)
            session.removeAttribute("userListFilters");
        }

        Map<String, Object> result = Map.of(
                "size", size,
                "page", page,
                "sort", sort != null ? sort : "",
                "direction", direction != null ? direction : "",
                "search", search != null ? search : "",
                "showFirmAdmins", showFirmAdmins,
                "usertype", usertype != null ? usertype : "",
                "firmSearchForm", firmSearchForm != null ? firmSearchForm : FirmSearchForm.builder().build());

        // Store current filter state in session for future back navigation
        session.setAttribute("userListFilters", result);


        return result;
    }
}
