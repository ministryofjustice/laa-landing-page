package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.forms.OfficesForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.CcmsRoleGroupsUtil;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;

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
 * Multi-firm User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/multi-firm")
@PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELEGATE_EXTERNAL_USER_ACCESS)")
public class MultiFirmUserController {

    private final UserService userService;

    private final LoginService loginService;

    private final RoleAssignmentService roleAssignmentService;

    private final EventService eventService;

    private final ModelMapper mapper;

    @Value("${feature.flag.enable.multi.firm.user}")
    private boolean enableMultiFirmUser;

    @GetMapping("/user/add/profile/before-start")
    public String addUserProfileStart() {
        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available."
                    + "Please contact your system administrator for assistance.");
        }

        return "multi-firm-user/add-profile-start";
    }

    @GetMapping("/user/add/profile")
    public String addUserProfile(Model model, HttpSession session) {

        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available."
                    + "Please contact your system administrator for assistance.");
        }

        MultiFirmUserForm multiFirmUserForm =
                getObjectFromHttpSession(session, "multiFirmUserForm", MultiFirmUserForm.class).orElse(new MultiFirmUserForm());
        model.addAttribute("multiFirmUserForm", multiFirmUserForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile");
        return "multi-firm-user/select-user";
    }

    @PostMapping("/user/add/profile")
    public String addUserProfilePost(@Valid MultiFirmUserForm multiFirmUserForm, BindingResult result,
                                     Model model, HttpSession session, Authentication authentication) {

        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available."
                    + "Please contact your system administrator for assistance.");
        }

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while searching for user: {}", result.getAllErrors());
            session.setAttribute("multiFirmUserForm", multiFirmUserForm);
            model.addAttribute("multiFirmUserForm", multiFirmUserForm);
            return "multi-firm-user/select-user";
        }

        Optional<EntraUser> entraUserOptional = userService.findEntraUserByEmail(multiFirmUserForm.getEmail());

        if (entraUserOptional.isEmpty()) {
            log.debug("User not found for the given user email: {}", multiFirmUserForm.getEmail());
            result.rejectValue("email", "error.email", "We could not find this user. Ask LAA to create the account.");
            return "multi-firm-user/select-user";
        } else {

            EntraUser entraUser = entraUserOptional.get();

            if (!entraUser.isMultiFirmUser()) {
                log.debug("The user is not a multi firm user: {}.", multiFirmUserForm.getEmail());
                result.rejectValue("email", "error.email",
                        "This user cannot be linked to another firm. Ask LAA to enable multi-firm for this user.");
                return "multi-firm-user/select-user";
            }

            if (entraUser.getUserProfiles() != null && !entraUser.getUserProfiles().isEmpty()) {
                UserProfile authenticatedUserProfile = loginService.getCurrentProfile(authentication);

                Optional<UserProfile> sameFirmProfile = entraUser.getUserProfiles().stream()
                        .filter(up -> up.getFirm().equals(authenticatedUserProfile.getFirm()))
                        .findFirst();

                if (sameFirmProfile.isPresent()) {
                    log.debug("This user already has access for your firm. Manage them from the Manage Your Users screen.");
                    result.rejectValue("email", "error.email", "This user already has access for your firm. Manage them from the Manage Your Users screen.");
                    model.addAttribute("userProfileExistsOnFirm", true);
                    model.addAttribute("existingUserProfileId", sameFirmProfile.get().getId());
                    return "multi-firm-user/select-user";
                }

                EntraUserDto entraUserDto = mapper.map(entraUser, EntraUserDto.class);

                model.addAttribute("entraUser", entraUserDto);
                session.setAttribute("entraUser", entraUserDto);

                model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - " + entraUserDto.getFullName());
            }

        }

        return "redirect:/admin/multi-firm/user/add/profile/select/apps";
    }

    /**
     * Grant Access Flow - Retrieves available apps for user and their currently
     * assigned apps.
     */
    @GetMapping("/user/add/profile/select/apps")
    public String selectUserApps(Model model, HttpSession session, Authentication authentication) {

        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available."
                    + "Please contact your system administrator for assistance.");
        }

        ApplicationsForm applicationsForm =
                getObjectFromHttpSession(session, "applicationsForm", ApplicationsForm.class).orElse(new ApplicationsForm());
        model.addAttribute("applicationsForm", applicationsForm);

        UserProfile editorUserProfile = loginService.getCurrentProfile(authentication);

        EntraUserDto targetEntraUserDto = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();
        List<AppDto> availableApps = userService.getAppsByUserType(UserType.EXTERNAL);

        List<AppDto> editableApps = availableApps.stream()
                .filter(app -> roleAssignmentService.canUserAssignRolesForApp(editorUserProfile, app))
                .toList();
        List<String> userAssignedApps = applicationsForm.getApps() == null ? List.of() : applicationsForm.getApps();

        // Add selected attribute to available apps based on user assigned apps
        editableApps.forEach(app -> {
            app.setSelected(userAssignedApps.stream()
                    .anyMatch(userApp -> userApp.equals(app.getName())));
        });

        model.addAttribute("user", targetEntraUserDto);
        model.addAttribute("apps", editableApps);

        // Store the model in session to handle validation errors later
        //session.setAttribute("grantAccessUserAppsModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Grant access - Select services - " + targetEntraUserDto.getFullName());
        return "multi-firm-user/select-user-apps";
    }

    @PostMapping("/user/add/profile/select/apps")
    public String selectUserAppsPost(@Valid ApplicationsForm applicationsForm, BindingResult result,
                                             Authentication authentication,
                                             Model model, HttpSession session) {
        if (!enableMultiFirmUser) {
            throw new RuntimeException("The page you are trying to access is not available."
                    + "Please contact your system administrator for assistance.");
        }

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while selecting apps: {}", result.getAllErrors());
            // If there are validation errors, return to the apps page with errors
            Model modelFromSession = (Model) session.getAttribute("grantAccessUserAppsModel");
            if (modelFromSession == null) {
                // If no model in session, redirect to apps page to repopulate
                return "multi-firm-user/select-user-apps";
            }

            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("apps", modelFromSession.getAttribute("apps"));
            return "multi-firm-user/select-user-apps";
        }

        EntraUserDto targetEntraUserDto = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();

        // Handle case where no apps are selected (apps will be null)
        List<String> selectedApps = applicationsForm.getApps() != null ? applicationsForm.getApps() : new ArrayList<>();
        session.setAttribute("grantAccessSelectedApps", selectedApps);

        // Clear the grantAccessUserAppsModel from session to avoid stale data
        session.removeAttribute("grantAccessUserAppsModel");

        // Ensure passed in ID is a valid UUID to avoid open redirects.
        return "redirect:/admin/multi-firm/user/add/profile/select/roles";
    }

    @GetMapping("/user/add/profile/select/roles")
    public String selectUserAppRoles( @RequestParam(defaultValue = "0") Integer selectedAppIndex,
                                           RolesForm rolesForm,
                                           Authentication authentication,
                                           Model model, HttpSession session) {

        UserProfile editorUserProfile = loginService.getCurrentProfile(authentication);
        final EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();

        List<String> selectedApps = getListFromHttpSession(session, "grantAccessSelectedApps", String.class).orElse(List.of());

        // Ensure the selectedAppIndex is within bounds
        if (selectedApps.isEmpty()) {
            // No apps assigned to user, redirect back to manage page
            return "redirect:/admin/multi-firm/user/add/profile/select/apps";
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

        List<AppRoleDto> roles = userService.getAppRolesByAppIdAndUserType(selectedApps.get(currentSelectedAppIndex), UserType.EXTERNAL);
        UserProfile editorProfile = loginService.getCurrentProfile(authentication);
        roles = roleAssignmentService.filterRoles(editorProfile.getAppRoles(), roles.stream().map(role -> UUID.fromString(role.getId())).toList());


        AppDto currentApp = userService.getAppByAppId(selectedApps.get(currentSelectedAppIndex)).orElseThrow();
        // Get currently selected roles from session or use user's existing roles
        List<String> selectedRoles = getListFromHttpSession(session, "grantAccessUserRoles", String.class).orElse(List.of());

        List<AppRoleViewModel> appRoleViewModels = roles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).sorted().toList();

        // Check if this is the CCMS app and organize roles by section
        boolean isCcmsApp = (currentApp.getName().contains("CCMS")
                && !currentApp.getName().contains("CCMS case transfer requests"))
                || roles.stream().anyMatch(role -> CcmsRoleGroupsUtil.isCcmsRole(role.getCcmsCode()));

        if (isCcmsApp) {
            // Filter to only CCMS roles for organization
            List<AppRoleDto> ccmsRoles = roles.stream()
                    .filter(role -> CcmsRoleGroupsUtil.isCcmsRole(role.getCcmsCode()))
                    .sorted().collect(Collectors.toList());

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
        return "multi-firm-user/select-user-app-roles";
    }

    @PostMapping("/user/add/profile/select/roles")
    public String selectUserAppRolesPost(@Valid RolesForm rolesForm, BindingResult result,
                                             @RequestParam int selectedAppIndex,
                                             Authentication authentication,
                                             Model model, HttpSession session) {
        Model modelFromSession = (Model) session.getAttribute("grantAccessUserRolesModel");
        if (modelFromSession == null) {
            return "redirect:/admin/multi-firm/user/add/profile/select/roles";
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

            return "multi-firm-user/select-user-app-roles";
        }

        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();
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
            UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
            // Clear the grantAccessUserRolesModel and page roles from session to avoid
            // stale data
            session.removeAttribute("grantAccessUserRolesModel");
            session.removeAttribute("grantAccessAllSelectedRoles");
            // Flatten the map to a single list of all selected roles across all pages.
            List<String> allSelectedRoles = allSelectedRolesByPage.values().stream().filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .toList();
            List<String> nonEditableRoles = List.of();
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            UserProfile editorProfile = loginService.getCurrentProfile(authentication);
            //if (roleAssignmentService.canAssignRole(editorProfile.getAppRoles(), allSelectedRoles)) {
            //    Map<String, String> updateResult = userService.updateUserRoles(id, allSelectedRoles, nonEditableRoles, currentUserDto.getUserId());
            //    UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
            //            editorProfile.getId(),
            //            currentUserDto,
            //            user != null ? user.getEntraUser() : null, updateResult.get("diff"),
            //            "role");
                //eventService.logEvent(updateUserAuditEvent);
            //}
            return "redirect:/admin/multi-firm/user/add/profile/select/offices";
        } else {
            modelFromSession.addAttribute("grantAccessSelectedAppIndex", selectedAppIndex + 1);
            session.setAttribute("grantAccessAllSelectedRoles", allSelectedRolesByPage);
            session.setAttribute("grantAccessUserRolesModel", modelFromSession);
            // Ensure passed in ID is a valid UUID to avoid open redirects.
            return "redirect:/admin/multi-firm/user/add/profile/select/roles?selectedAppIndex=" + (selectedAppIndex + 1);
        }
    }

    @GetMapping("/user/add/profile/select/offices")
    public String grantAccessEditUserOffices(Model model, HttpSession session, Authentication authentication) {
        final EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);


        List<OfficeDto> userOffices = getListFromHttpSession(session, "grantAccessUserOffices", OfficeDto.class).orElse(List.of());
        Set<String> userOfficeIds = userOffices.stream()
                .map(office -> office.getId().toString())
                .collect(Collectors.toSet());
        // Get user's available offices by firm
        Firm userFirm = currentUserProfile.getFirm();
        UUID firmId = userFirm.getId();
        Set<Office> allOffices = currentUserProfile.getOffices();

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
        return "multi-firm-user/select-user-offices";
    }

    /**
     * Grant Access Flow - Update user offices
     */
    @PostMapping("/user/add/profile/select/offices")
    public String grantAccessUpdateUserOffices(@Valid OfficesForm officesForm, BindingResult result,
                                               Authentication authentication,
                                               Model model, HttpSession session) throws IOException {
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while updating user offices: {}", result.getAllErrors());
            // If there are validation errors, return to the edit user offices page with
            // errors
            Model modelFromSession = (Model) session.getAttribute("grantAccessUserOfficesModel");
            if (modelFromSession == null) {
                return "redirect:/admin/multi-firm/user/add/profile/select/offices";
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
            return "multi-firm-user/select-user-offices";
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

        //String changed = userService.updateUserOffices(id, selectedOffices);
        //CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        //UserProfileDto userProfileDto = userService.getUserProfileById(id).orElse(null);
//        UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
//                userProfileDto != null ? userProfileDto.getId() : null,
//                currentUserDto,
//                userProfileDto != null ? userProfileDto.getEntraUser() : null,
//                changed, "office");
        //eventService.logEvent(updateUserAuditEvent);

        // Clear grant access session data
        session.removeAttribute("grantAccessUserOfficesModel");
        session.removeAttribute("grantAccessSelectedApps");
        session.removeAttribute("grantAccessUserRoles");
        session.removeAttribute("grantAccessUserRolesModel");
        session.removeAttribute("grantAccessAllSelectedRoles");

        return "redirect:/admin/multi-firm/user/add/profile/check-answers";
    }

    @GetMapping("/user/add/profile/check-answers")
    public String checkAnswerAddProfile(Model model, Authentication authentication) {
        UserProfile editorUserProfile = loginService.getCurrentProfile(authentication);
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();

        // Get user's current app roles
        List<AppRoleDto> userAppRoles = userService.getUserAppRolesByUserId(id);
        List<AppRoleDto> editableUserAppRoles = userAppRoles.stream()
                .filter(role -> roleAssignmentService.canUserAssignRolesForApp(editorUserProfile, role.getApp()))
                .toList();

        // Group roles by app name and sort by app name
        Map<String, List<AppRoleDto>> groupedAppRoles = editableUserAppRoles.stream().sorted()
                .collect(Collectors.groupingBy(
                        appRole -> appRole.getApp().getName(),
                        LinkedHashMap::new, // Preserve insertion order
                        Collectors.toList()));

        // Sort the map by app name
        Map<String, List<AppRoleDto>> sortedGroupedAppRoles = groupedAppRoles.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));

        // Get user's current offices
        List<OfficeDto> userOffices = userService.getUserOfficesByUserId(id);

        model.addAttribute("user", user);
        model.addAttribute("userAppRoles", editableUserAppRoles);
        model.addAttribute("groupedAppRoles", sortedGroupedAppRoles);
        model.addAttribute("userOffices", userOffices);
        model.addAttribute("externalUser", user.getUserType() == UserType.EXTERNAL);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Grant access - Check your answers - " + user.getFullName());

        return "multi-firm-user/add-profile-checkanswers";
    }

    @PostMapping("/user/add/profile/check-answers")
    public String checkAnswerAddProfilePost(Authentication authentication, HttpSession session) {
        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();
        List<AppRoleDto> appRoleDtos = getListFromHttpSession(session, "userAppRoles", AppRoleDto.class).orElseThrow();
        List<OfficeDto> userOfficeDtos = getListFromHttpSession(session, "userOffices", OfficeDto.class).orElse(List.of());

        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UserProfileDto currentUserProfileDto = getObjectFromHttpSession(session, "userProfile", UserProfileDto.class).orElseThrow();

        FirmDto firmDto = currentUserProfileDto.getFirm();

        try {

            userService.addMultiFirmUserProfile(user, firmDto, userOfficeDtos, appRoleDtos, currentUserDto.getName());

            UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
                    currentUserProfileDto.getId(),
                    currentUserDto,
                    currentUserProfileDto.getEntraUser(),
                    "Access granted",
                    "access_grant_complete");
            eventService.logEvent(updateUserAuditEvent);

        } catch (Exception e) {
            log.error("Error creating new profile for user: {}", user.getFullName(), e);
        }

        // Clear grant access session data
        session.removeAttribute("grantAccessUserOfficesModel");
        session.removeAttribute("grantAccessSelectedApps");
        session.removeAttribute("grantAccessUserRoles");
        session.removeAttribute("grantAccessUserRolesModel");
        session.removeAttribute("grantAccessAllSelectedRoles");

        return "redirect:/admin/multi-firm/user/add/profile/confirmation";
    }

    @GetMapping("/user/add/profile/confirmation")
    public String addProfileConfirmation(Model model, HttpSession session) {
        UserProfileDto user = getObjectFromHttpSession(session, "userProfile", UserProfileDto.class).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User profile created - " + user.getFullName());
        return "multi-firm-user/add-profile-confirmation";
    }

    @GetMapping("/user/create/cancel")
    public String cancelUserProfileCreation(HttpSession session) {
        session.removeAttribute("entraUser");
        session.removeAttribute("multiFirmUserForm");
        session.removeAttribute("existingUserProfile");
        return "redirect:/admin/users";
    }

    /**
     * Handle authorization exceptions when user lacks permissions to access
     * specific users
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public RedirectView handleAuthorizationException(Exception ex, HttpSession session,
                                                     HttpServletRequest request) {
        Object requestedPath = session != null ? session.getAttribute("SPRING_SECURITY_SAVED_REQUEST") : null;
        String uri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";
        String referer = request != null ? request.getHeader("Referer") : null;
        log.warn("Authorization denied while accessing user: reason='{}', method='{}', uri='{}', referer='{}', savedRequest='{}'",
                ex.getMessage(), method, uri, referer, requestedPath);
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

}
