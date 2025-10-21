package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AddUserProfileAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
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
import uk.gov.justice.laa.portal.landingpage.model.UserRole;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

    private final AppRoleService appRoleService;

    private final RoleAssignmentService roleAssignmentService;

    private final OfficeService officeService;

    private final EventService eventService;

    private final ModelMapper mapper;

    @GetMapping("/user/add/profile/before-start")
    public String addUserProfileStart(HttpSession session) {
        clearSessionAttributes(session);

        return "multi-firm-user/add-profile-start";
    }

    @GetMapping("/user/add/profile")
    public String addUserProfile(Model model, HttpSession session) {
        MultiFirmUserForm multiFirmUserForm =
                getObjectFromHttpSession(session, "multiFirmUserForm", MultiFirmUserForm.class).orElse(new MultiFirmUserForm());
        model.addAttribute("multiFirmUserForm", multiFirmUserForm);
        model.addAttribute("email", multiFirmUserForm.getEmail());
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile");
        return "multi-firm-user/select-user";
    }

    @PostMapping("/user/add/profile")
    public String addUserProfilePost(@Valid MultiFirmUserForm multiFirmUserForm, BindingResult result,
                                     Model model, HttpSession session, Authentication authentication) {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while searching for user: {}", result.getAllErrors());
            session.setAttribute("multiFirmUserForm", multiFirmUserForm);
            model.addAttribute("multiFirmUserForm", multiFirmUserForm);
            return "multi-firm-user/select-user";
        }

        session.setAttribute("multiFirmUserForm", multiFirmUserForm);

        Optional<EntraUser> entraUserOptional = userService.findEntraUserByEmail(multiFirmUserForm.getEmail());

        if (entraUserOptional.isPresent()) {

            EntraUser entraUser = entraUserOptional.get();

            if (!entraUser.isMultiFirmUser()) {
                log.debug("The user is not a multi firm user: {}.", multiFirmUserForm.getEmail());
                result.rejectValue("email", "error.email", "This user cannot be linked to another firm. Ask LAA to enable multi-firm for this user.");
                return "multi-firm-user/select-user";
            }

            if (entraUser.getUserProfiles() != null && !entraUser.getUserProfiles().isEmpty()) {
                UserProfile authenticatedUserProfile = loginService.getCurrentProfile(authentication);

                Optional<UserProfile> sameFirmProfile = entraUser.getUserProfiles().stream().filter(up -> up.getFirm().equals(authenticatedUserProfile.getFirm())).findFirst();

                if (sameFirmProfile.isPresent()) {
                    log.debug("This user already has access for your firm. Manage them from the Manage Your Users screen.");
                    result.rejectValue("email", "error.email", "This user already has access for your firm. Manage them from the Manage Your Users screen.");
                    model.addAttribute("userProfileExistsOnFirm", true);
                    model.addAttribute("existingUserProfileId", sameFirmProfile.get().getId());
                    return "multi-firm-user/select-user";
                }
            }

            EntraUserDto entraUserDto = mapper.map(entraUser, EntraUserDto.class);

            model.addAttribute("entraUser", entraUserDto);
            session.setAttribute("entraUser", entraUserDto);

            model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - " + entraUserDto.getFullName());
            return "redirect:/admin/multi-firm/user/add/profile/select/apps";

        } else {
            log.debug("User not found for the given user email: {}", multiFirmUserForm.getEmail());
            result.rejectValue("email", "error.email", "We could not find this user. Ask LAA to create the account.");
            return "multi-firm-user/select-user";
        }
    }

    @GetMapping("/user/add/profile/select/apps")
    public String selectUserApps(Model model, HttpSession session, Authentication authentication) {

        ApplicationsForm applicationsForm =
                getObjectFromHttpSession(session, "applicationsForm", ApplicationsForm.class).orElse(new ApplicationsForm());
        model.addAttribute("applicationsForm", applicationsForm);


        List<AppDto> availableApps = userService.getAppsByUserType(UserType.EXTERNAL);

        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        List<AppDto> assignableApps = availableApps.stream()
                .filter(app -> roleAssignmentService.canUserAssignRolesForApp(currentUserProfile, app))
                .toList();
        List<String> selectedApps = applicationsForm.getApps() == null ? List.of() : applicationsForm.getApps();

        assignableApps.forEach(app -> app.setSelected(selectedApps.stream()
                .anyMatch(userApp -> userApp.equals(app.getId()))));

        EntraUserDto entraUserDto = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();
        model.addAttribute("entraUser", entraUserDto);
        model.addAttribute("apps", assignableApps);

        session.setAttribute("addProfileUserAppsModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - Select services - " + entraUserDto.getFullName());
        return "multi-firm-user/select-user-apps";
    }

    @PostMapping("/user/add/profile/select/apps")
    public String selectUserAppsPost(@Valid ApplicationsForm applicationsForm, BindingResult result,
                                     Model model, HttpSession session) {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while selecting apps: {}", result.getAllErrors());

            Model modelFromSession = (Model) session.getAttribute("addProfileUserAppsModel");
            if (modelFromSession == null) {
                return "multi-firm-user/select-user-apps";
            }

            model.addAttribute("entraUser", modelFromSession.getAttribute("entraUser"));
            model.addAttribute("apps", modelFromSession.getAttribute("apps"));
            return "multi-firm-user/select-user-apps";
        }

        session.setAttribute("applicationsForm", applicationsForm);

        List<String> selectedAppIds = applicationsForm.getApps() != null ? applicationsForm.getApps() : new ArrayList<>();
        session.setAttribute("addProfileSelectedApps", selectedAppIds);

        session.removeAttribute("addProfileUserAppsModel");

        return "redirect:/admin/multi-firm/user/add/profile/select/roles";
    }

    @GetMapping("/user/add/profile/select/roles")
    public String selectUserAppRoles(@RequestParam(defaultValue = "0") Integer selectedAppIndex,
                                     RolesForm rolesForm,
                                     Authentication authentication,
                                     Model model, HttpSession session) {

        List<String> selectedAppIds = getListFromHttpSession(session, "addProfileSelectedApps", String.class).orElse(List.of());

        if (selectedAppIds.isEmpty()) {
            return "redirect:/admin/multi-firm/user/add/profile/select/apps";
        }

        Integer currentSelectedAppIndex = selectedAppIndex != null ? selectedAppIndex : 0;

        if (currentSelectedAppIndex >= selectedAppIds.size()) {
            currentSelectedAppIndex = 0;
        }

        List<AppRoleDto> availableRoles = userService.getAppRolesByAppIdAndUserType(selectedAppIds.get(currentSelectedAppIndex), UserType.EXTERNAL);
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        List<AppRoleDto> assignableRoles = roleAssignmentService.filterRoles(currentUserProfile.getAppRoles(), availableRoles.stream().map(role -> UUID.fromString(role.getId())).toList());

        final AppDto currentApp = userService.getAppByAppId(selectedAppIds.get(currentSelectedAppIndex)).orElseThrow();

        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> editUserAllSelectedRoles = (Map<Integer, List<String>>) session.getAttribute("addUserProfileAllSelectedRoles");
        if (Objects.isNull(editUserAllSelectedRoles)) {
            editUserAllSelectedRoles = new HashMap<>();
        }

        List<String> selectedRoles;
        if (editUserAllSelectedRoles.get(currentSelectedAppIndex) != null) {
            selectedRoles = editUserAllSelectedRoles.get(currentSelectedAppIndex);
        } else {
            selectedRoles = new ArrayList<>();
        }

        List<AppRoleViewModel> appRoleViewModels = assignableRoles.stream()
                .map(appRoleDto -> {
                    AppRoleViewModel viewModel = mapper.map(appRoleDto, AppRoleViewModel.class);
                    viewModel.setSelected(selectedRoles.contains(appRoleDto.getId()));
                    return viewModel;
                }).sorted().toList();

        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();

        model.addAttribute("entraUser", user);
        model.addAttribute("roles", appRoleViewModels);
        model.addAttribute("addProfileSelectedAppIndex", currentSelectedAppIndex);
        model.addAttribute("addProfileCurrentApp", currentApp);

        String rolesBackUrl = currentSelectedAppIndex == 0
                ? "/admin/multi-firm/user/add/profile/select/apps"
                : "/admin/multi-firm/user/add/profile/select/roles?selectedAppIndex=" + (currentSelectedAppIndex - 1);
        model.addAttribute("backUrl", rolesBackUrl);

        session.setAttribute("addProfileUserRolesModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - Select roles - " + user.getFullName());
        return "multi-firm-user/select-user-app-roles";
    }

    @PostMapping("/user/add/profile/select/roles")
    public String selectUserAppRolesPost(@Valid RolesForm rolesForm, BindingResult result,
                                         @RequestParam int selectedAppIndex,
                                         Authentication authentication,
                                         Model model, HttpSession session) {
        Model modelFromSession = (Model) session.getAttribute("addProfileUserRolesModel");
        if (modelFromSession == null) {
            return "redirect:/admin/multi-firm/user/add/profile/select/roles";
        }
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while setting user roles: {}", result.getAllErrors());
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
            model.addAttribute("entraUser", modelFromSession.getAttribute("entraUser"));
            model.addAttribute("addProfileSelectedAppIndex",
                    modelFromSession.getAttribute("addProfileSelectedAppIndex"));
            model.addAttribute("addProfileCurrentApp", modelFromSession.getAttribute("addProfileCurrentApp"));

            return "multi-firm-user/select-user-app-roles";
        }

        List<String> selectedApps = getListFromHttpSession(session, "addProfileSelectedApps", String.class)
                .orElseGet(ArrayList::new);
        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("addUserProfileAllSelectedRoles");
        if (allSelectedRolesByPage == null) {
            allSelectedRolesByPage = new HashMap<>();
        }
        // Add the roles for the currently selected app to a map for lookup.
        allSelectedRolesByPage.put(selectedAppIndex, rolesForm.getRoles());
        if (selectedAppIndex >= selectedApps.size() - 1) {
            List<String> allSelectedRoles = allSelectedRolesByPage.values().stream().filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .toList();
            session.setAttribute("addUserProfileAllSelectedRoles", allSelectedRolesByPage);

            UserProfile editorProfile = loginService.getCurrentProfile(authentication);
            if (!roleAssignmentService.canAssignRole(editorProfile.getAppRoles(), allSelectedRoles)) {
                return "multi-firm-user/select-user-app-roles";
            }
            session.removeAttribute("addProfileUserRolesModel");
            return "redirect:/admin/multi-firm/user/add/profile/select/offices";
        } else {
            modelFromSession.addAttribute("addProfileSelectedAppIndex", selectedAppIndex + 1);
            session.setAttribute("addUserProfileAllSelectedRoles", allSelectedRolesByPage);
            session.setAttribute("addProfileUserRolesModel", modelFromSession);
            return "redirect:/admin/multi-firm/user/add/profile/select/roles?selectedAppIndex=" + (selectedAppIndex + 1);
        }
    }

    @GetMapping("/user/add/profile/select/offices")
    public String addProfileSelectOffices(Model model, HttpSession session, Authentication authentication) {
        final EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();

        OfficesForm userOfficesForm = getObjectFromHttpSession(session, "officesForm", OfficesForm.class)
                .orElse(OfficesForm.builder().offices(List.of()).build());
        Set<String> userOfficeIds = new HashSet<>(userOfficesForm.getOffices());

        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);

        Firm currentUserFirm = currentUserProfile.getFirm();
        Set<Office> currentUserOffices = currentUserFirm.getOffices();

        final List<OfficeModel> officeData = currentUserOffices.stream()
                .map(office -> new OfficeModel(
                        office.getCode(),
                        OfficeModel.Address.builder().addressLine1(office.getAddress().getAddressLine1())
                                .addressLine2(office.getAddress().getAddressLine2()).city(office.getAddress().getCity())
                                .postcode(office.getAddress().getPostcode()).build(),
                        office.getId().toString(),
                        userOfficeIds.contains(office.getId().toString())))
                .collect(Collectors.toList());

        boolean hasAllOffices = userOfficeIds.contains("ALL");

        model.addAttribute("entraUser", user);
        model.addAttribute("officesForm", userOfficesForm);
        model.addAttribute("officeData", officeData);
        model.addAttribute("hasAllOffices", hasAllOffices);

        // Store the model in session to handle validation errors later
        session.setAttribute("addProfileUserOfficesModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - Select offices - " + user.getFullName());
        return "multi-firm-user/select-user-offices";
    }

    @PostMapping("/user/add/profile/select/offices")
    public String addProfileSelectOfficesPost(@Valid OfficesForm officesForm, BindingResult result,
                                              Model model, HttpSession session) {
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while selecting user offices: {}", result.getAllErrors());
            // If there are validation errors, return to the edit user offices page with
            // errors
            Model modelFromSession = (Model) session.getAttribute("addProfileUserOfficesModel");
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

            model.addAttribute("entraUser", modelFromSession.getAttribute("entraUser"));
            model.addAttribute("officeData", modelFromSession.getAttribute("officeData"));
            return "multi-firm-user/select-user-offices";
        }

        List<String> selectedOffices = officesForm.getOffices() != null ? officesForm.getOffices() : new ArrayList<>();
        session.setAttribute("userOffices", selectedOffices);
        session.setAttribute("officesForm", officesForm);

        return "redirect:/admin/multi-firm/user/add/profile/check-answers";
    }

    @GetMapping("/user/add/profile/check-answers")
    public String checkAnswerAndAddProfile(Model model, Authentication authentication, HttpSession session) {
        Map<Integer, List<String>> appRolesByPage = (Map<Integer, List<String>>) session.getAttribute("addUserProfileAllSelectedRoles");
        if (appRolesByPage == null) {
            appRolesByPage = new HashMap<>();
        }
        List<String> userOfficeIds = getListFromHttpSession(session, "userOffices", String.class).orElse(List.of());

        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        UserProfileDto currentUserProfileDto = mapper.map(currentUserProfile, UserProfileDto.class);
        List<OfficeDto> userOfficeDtos = userOfficeIds.contains("ALL") ? List.of() : officeService.getOfficesByIds(userOfficeIds);
        FirmDto firmDto = currentUserProfileDto.getFirm();

        session.setAttribute("userProfile", currentUserProfileDto);
        model.addAttribute("userOffices", userOfficeDtos);
        model.addAttribute("firm", firmDto);

        List<AppRoleDto> appRoleDtoList = appRoleService.getByIds(appRolesByPage.values().stream()
                .filter(Objects::nonNull).flatMap(List::stream).toList())
                .stream().sorted(Comparator.comparingInt(AppRoleDto::getOrdinal)).toList();
        List<UserRole> selectedAppRole = appRoleDtoList.stream()
                .map(appRoleDto -> UserRole.builder().appName(appRoleDto.getName())
                        .roleName(appRoleDto.getName()).url("/admin/multi-firm/user/add/profile/select/roles").build())
                .toList();
        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();

        model.addAttribute("user", user);
        model.addAttribute("selectedAppRole", selectedAppRole);
        model.addAttribute("externalUser", true);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Add profile - Check your answers - " + user.getFullName());

        return "multi-firm-user/add-profile-check-answers";
    }

    @PostMapping("/user/add/profile/check-answers")
    public String checkAnswerAndAddProfilePost(Authentication authentication, HttpSession session) {
        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class).orElseThrow();

        Map<Integer, List<String>> appRolesByPage = (Map<Integer, List<String>>) session.getAttribute("addUserProfileAllSelectedRoles");
        if (appRolesByPage == null) {
            appRolesByPage = new HashMap<>();
        }

        List<AppRoleDto> appRoleDtoList = appRoleService.getByIds(appRolesByPage.values().stream().filter(Objects::nonNull).flatMap(List::stream).toList());

        // Validate if app role assignment is fully permitted
        UserProfile userProfile = loginService.getCurrentProfile(authentication);
        if (!roleAssignmentService.canAssignRole(userProfile.getAppRoles(), appRoleDtoList.stream().map(AppRoleDto::getId).toList())) {
            log.error("User does not have sufficient permissions to assign the selected roles: userId={}, attemptedRoleIds={}",
                    userProfile.getId(),
                    appRoleDtoList.stream().map(AppRoleDto::getId).toList());
            throw new RuntimeException("User does not have sufficient permissions to assign the selected roles");
        }

        // Validate if the office assignment is fully permitted
        List<OfficeDto> userOfficeDtos = getListFromHttpSession(session, "userOffices", OfficeDto.class).orElse(List.of());
        if (!userOfficeDtos.isEmpty()) {
            if (userProfile.getFirm() == null || userProfile.getFirm().getOffices() == null
                    || !userOfficeDtos.stream().map(OfficeDto::getCode).allMatch(code ->
                    userProfile.getFirm().getOffices().stream().map(Office::getCode).anyMatch(code::equals))) {
                log.error("User does not have sufficient permissions to assign the selected offices: userId={}, attemptedOfficeIds={}",
                        userProfile.getId(),
                        userOfficeDtos.stream().map(OfficeDto::getId).toList());
                throw new RuntimeException("Office assignment is not permitted");
            }
        }

        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UserProfileDto currentUserProfileDto = mapper.map(userProfile, UserProfileDto.class);
        FirmDto firmDto = currentUserProfileDto.getFirm();

        try {
            UserProfile newUserProfile = userService.addMultiFirmUserProfile(user, firmDto, userOfficeDtos,
                    appRoleDtoList, currentUserDto.getName());

            String rolesAdded = appRoleDtoList.stream().map(AppRoleDto::getName)
                    .collect(Collectors.joining(",", "(", ")"));
            AddUserProfileAuditEvent addUserProfileAuditEvent = new AddUserProfileAuditEvent(
                    currentUserDto,
                    newUserProfile.getId(),
                    user,
                    firmDto.getId(),
                    "roles",
                    rolesAdded);
            eventService.logEvent(addUserProfileAuditEvent);
        } catch (Exception e) {
            log.error("Error creating new profile for user: {}", user.getFullName(), e);
        }

        // Clear grant access session data
        session.removeAttribute("userAppRoles");
        session.removeAttribute("userOffices");

        return "redirect:/admin/multi-firm/user/add/profile/confirmation";
    }

    @GetMapping("/user/add/profile/confirmation")
    public String addProfileConfirmation(Model model, HttpSession session) {
        EntraUserDto user = getObjectFromHttpSession(session, "entraUser", EntraUserDto.class)
                .orElse(EntraUserDto.builder().firstName("Unknown").lastName("Unknown").build());
        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User profile created - " + user.getFullName());
        return "multi-firm-user/add-profile-confirmation";
    }

    @GetMapping("/user/cancel")
    public String cancelUserProfileCreation(HttpSession session) {
        clearSessionAttributes(session);

        return "redirect:/admin/users";
    }

    private void clearSessionAttributes(HttpSession session) {
        session.removeAttribute("addUserProfileAllSelectedRoles");
        session.removeAttribute("addProfileSelectedApps");
        session.removeAttribute("addProfileUserAppsModel");
        session.removeAttribute("addProfileUserOfficesModel");
        session.removeAttribute("addProfileUserRolesModel");
        session.removeAttribute("entraUser");
        session.removeAttribute("multiFirmUserForm");
        session.removeAttribute("applicationsForm");
        session.removeAttribute("userOffices");
        session.removeAttribute("userProfile");
        session.removeAttribute("officesForm");
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
