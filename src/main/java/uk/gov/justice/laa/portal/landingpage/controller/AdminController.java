package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppDetailsAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppDisplayOrderAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppRoleDetailsAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppRoleDisplayOrderAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.AppDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppRoleDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppRolesOrderForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
import uk.gov.justice.laa.portal.landingpage.forms.DeleteAppRoleReasonForm;
import uk.gov.justice.laa.portal.landingpage.service.AdminService;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.AppService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;

/**
 * Controller for SiLAS Administration section
 * Requires "Global Admin" role to access
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
@PreAuthorize("@accessControlService.userHasAuthzRole(authentication, T(uk.gov.justice.laa.portal.landingpage.entity.AuthzRole).SILAS_ADMINISTRATION.roleName)")
public class AdminController {

    public static final String SILAS_ADMINISTRATION_TITLE = "SiLAS Administration";
    private static final Set<String> VALID_TABS = Set.of("admin-apps", "apps", "roles");

    private final LoginService loginService;
    private final EventService eventService;
    private final AdminService adminService;
    private final AppService appService;
    private final AppRoleService appRoleService;

    /**
     * Display SiLAS Administration landing page with Admin Services tab by default
     */
    @GetMapping("/silas-administration")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_LAA_APP_METADATA,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String showAdministration(
            @RequestParam(defaultValue = "admin-apps") String tab,
            @RequestParam(required = false) String appFilter,
            Model model, HttpSession session) {

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);
        model.addAttribute("activeTab", tab);

        // Clear any session details from older operations (do not store Model in session)
        clearSessionAttributes(session);

        // Load all admin apps data for admin-apps tab
        model.addAttribute("adminApps", adminService.getAllAdminApps());

        // Load all apps data for apps tab
        List<AppDto> apps = appService.getAllLaaApps();
        model.addAttribute("apps", apps);

        appFilter = Optional.ofNullable(appFilter)
                .or(() -> getObjectFromHttpSession(session, "appFilter", String.class))
                .orElse(null);

        List<AppRoleAdminDto> roles = StringUtils.hasText(appFilter)
                ? appRoleService.getLaaAppRolesByAppName(appFilter)
                : appRoleService.getAllLaaAppRoles();

        model.addAttribute("roles", roles);
        model.addAttribute("appFilter", appFilter);
        session.setAttribute("appFilter", appFilter);

        // Get distinct app names for filter dropdown
        model.addAttribute("appNames", apps.stream()
                .map(AppDto::getName)
                .distinct()
                .sorted()
                .collect(Collectors.toList()));

        return "silas-administration/administration";
    }

    @GetMapping("/silas-administration/app/{appId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppDetailsGet(@PathVariable String appId, Model model, HttpSession session) {
        AppDto appDto = findAppDtoOrThrow(appId);
        model.addAttribute("app", appDto);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);
        AppDetailsForm appDetailsForm = (AppDetailsForm) session.getAttribute("appDetailsForm");

        if (appDetailsForm == null) {
            appDetailsForm = AppDetailsForm.builder()
                    .appId(appDto.getId())
                    .enabled(appDto.isEnabled())
                    .description(appDto.getDescription())
                    .build();
        }

        model.addAttribute("appDetailsForm", appDetailsForm);
        session.setAttribute("appDetailsForm", appDetailsForm);
        return "silas-administration/edit-app-details";
    }

    @PostMapping("/silas-administration/app/{appId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppDetailsPost(@PathVariable String appId, @Valid AppDetailsForm appDetailsForm,
                                     BindingResult result, Model model, HttpSession session) {
        if (result.hasErrors()) {
            model.addAttribute("app", findAppDtoOrThrow(appId));
            model.addAttribute("errorMessage", buildErrorMessages(result));
            return "silas-administration/edit-app-details";
        }

        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", appDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return String.format("redirect:/admin/silas-administration/app/%s/check-answers", appId);
    }

    @GetMapping("/silas-administration/app/{appId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppDetailsGet(@PathVariable String appId,
                                       Model model,
                                       HttpSession session) {
        String appIdFromSession = getObjectFromHttpSession(session, "appId", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App ID not found in session"));
        AppDetailsForm appDetailsForm = getObjectFromHttpSession(session, "appDetailsForm", AppDetailsForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App details not found in session"));

        if (!appId.equals(appIdFromSession)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request for app details change");
        }

        AppDto appDto = findAppDtoOrThrow(appId);

        model.addAttribute("app", appDto);
        model.addAttribute("appDetailsForm", appDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return "silas-administration/edit-app-details-check-answers";
    }

    @PostMapping("/silas-administration/app/{appId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppDetailsPost(@PathVariable String appId,
                                        Authentication authentication,
                                        Model model,
                                        HttpSession session) {
        String appIdFromSession = getObjectFromHttpSession(session, "appId", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App ID not found in session"));
        AppDetailsForm appDetailsForm = getObjectFromHttpSession(session, "appDetailsForm", AppDetailsForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App details not found in session"));

        if (!appId.equals(appIdFromSession)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request for app details change");
        }

        AppDto appDto = findAppDtoOrThrow(appId);
        String appName = appDto.getName();

        appDto.setEnabled(appDetailsForm.isEnabled());
        appDto.setDescription(appDetailsForm.getDescription());

        App updatedApp = appService.save(appDto);

        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        UpdateAppDetailsAuditEvent updateAppDetailsAuditEvent = new UpdateAppDetailsAuditEvent(currentUserDto,
                currentUserProfile.getId(), appName, updatedApp.isEnabled(), appDto.isEnabled(), updatedApp.getDescription(), appDto.getDescription());
        eventService.logEvent(updateAppDetailsAuditEvent);

        model.addAttribute("app", appDto);
        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        session.removeAttribute("appDetailsForm");
        session.removeAttribute("appDetailsFormModel");
        session.removeAttribute("appId");

        return "silas-administration/edit-app-details-confirmation";
    }

    @GetMapping("/silas-administration/apps/reorder")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppOrderGet(Model model,
                                  HttpSession session) {
        AppsOrderForm appsOrderForm = getObjectFromHttpSession(session, "appsOrderForm", AppsOrderForm.class).orElse(null);
        if (appsOrderForm == null) {
            List<AppDto> allLaaApps = appService.getAllLaaApps();
            List<AppsOrderForm.AppOrderDetailsForm> appOrderDetailsForm = allLaaApps.stream()
                    .map(AppsOrderForm.AppOrderDetailsForm::new)
                    .toList();
            appsOrderForm = AppsOrderForm.builder().apps(appOrderDetailsForm).build();
        }
        model.addAttribute("appsOrderForm", appsOrderForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return "silas-administration/edit-apps-order";
    }

    @PostMapping("/silas-administration/apps/reorder")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppOrderPost(@Valid AppsOrderForm appsOrderForm,
                                   BindingResult result,
                                   Model model,
                                   HttpSession session) {
        if (result.hasErrors()) {
            model.addAttribute("errorMessage", buildErrorMessages(result));
            return "silas-administration/edit-apps-order";
        }

        appsOrderForm.getApps().sort(Comparator.comparingInt(AppsOrderForm.AppOrderDetailsForm::getOrdinal));
        session.setAttribute("appsOrderForm", appsOrderForm);
        model.addAttribute("appsOrderList", appsOrderForm.getApps());

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return "silas-administration/edit-apps-order-check-answers";
    }

    @PostMapping("/silas-administration/apps/reorder/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmEditAppOrderPost(Authentication authentication, Model model,
                                          HttpSession session) {
        AppsOrderForm appsOrderForm = getObjectFromHttpSession(session, "appsOrderForm", AppsOrderForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App order details not found in session"));
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

        appService.updateAppsOrder(appsOrderForm.getApps());

        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        UpdateAppDisplayOrderAuditEvent updateAppOrdinalAuditEvent = new UpdateAppDisplayOrderAuditEvent(currentUserDto,
                currentUserProfile.getId());
        eventService.logEvent(updateAppOrdinalAuditEvent);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);
        session.removeAttribute("appsOrderForm");

        return "silas-administration/edit-apps-order-confirmation";
    }

    @GetMapping("/silas-administration/role/{roleId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppRoleDetailsGet(@PathVariable String roleId,
                                        Model model,
                                        HttpSession session) {
        AppRoleDto appRoleDto = findAppRoleDtoOrThrow(roleId);
        model.addAttribute("appRole", appRoleDto);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);
        AppRoleDetailsForm appRoleDetailsForm = (AppRoleDetailsForm) session.getAttribute("appRoleDetailsForm");

        if (appRoleDetailsForm == null) {
            appRoleDetailsForm = AppRoleDetailsForm.builder()
                    .appRoleId(appRoleDto.getId())
                    .name(appRoleDto.getName())
                    .description(appRoleDto.getDescription())
                    .build();
        }

        model.addAttribute("appRoleDetailsForm", appRoleDetailsForm);
        session.setAttribute("appRoleDetailsForm", appRoleDetailsForm);

        return "silas-administration/edit-role-details";
    }

    @PostMapping("/silas-administration/role/{roleId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppRoleDetailsPost(@PathVariable String roleId,
                                         @Valid AppRoleDetailsForm appRoleDetailsForm,
                                         BindingResult result,
                                         Model model,
                                         HttpSession session) {
        if (result.hasErrors()) {
            model.addAttribute("appRole", findAppRoleDtoOrThrow(roleId));
            model.addAttribute("errorMessage", buildErrorMessages(result));
            return "silas-administration/edit-role-details";
        }

        session.setAttribute("roleId", roleId);
        session.setAttribute("appRoleDetailsForm", appRoleDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return String.format("redirect:/admin/silas-administration/role/%s/check-answers", roleId);
    }

    @GetMapping("/silas-administration/role/{roleId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppRoleDetailsGet(@PathVariable String roleId,
                                           Model model,
                                           HttpSession session) {
        String roleIdFromSession = getObjectFromHttpSession(session, "roleId", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        AppRoleDetailsForm roleDetailsForm = getObjectFromHttpSession(session, "appRoleDetailsForm", AppRoleDetailsForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App role details not found in session"));

        if (!roleId.equals(roleIdFromSession)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request for app details change");
        }

        AppRoleDto roleDto = findAppRoleDtoOrThrow(roleId);

        model.addAttribute("appRole", roleDto);
        model.addAttribute("appRoleDetailsForm", roleDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return "silas-administration/edit-role-details-check-answers";
    }

    @PostMapping("/silas-administration/role/{roleId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppRoleDetailsPost(@PathVariable String roleId,
                                            Authentication authentication,
                                            Model model,
                                            HttpSession session) {
        String roleIdFromSession = getObjectFromHttpSession(session, "roleId", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App Role ID not found in session"));
        AppRoleDetailsForm roleDetailsForm = getObjectFromHttpSession(session, "appRoleDetailsForm", AppRoleDetailsForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App role details not found in session"));

        if (!roleId.equals(roleIdFromSession)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request for app role details change");
        }

        AppRoleDto roleDto = findAppRoleDtoOrThrow(roleId);
        String appRoleName = roleDto.getName();
        String appRoleDescription = roleDto.getDescription();

        roleDto.setName(roleDetailsForm.getName());
        roleDto.setDescription(roleDetailsForm.getDescription());

        AppRole updatedAppRole = appRoleService.save(roleDto);


        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        UpdateAppRoleDetailsAuditEvent updateAppRoleDetailsAuditEvent = new UpdateAppRoleDetailsAuditEvent(currentUserDto,
                currentUserProfile.getId(), updatedAppRole.getName(), appRoleName, updatedAppRole.getDescription(), appRoleDescription);
        eventService.logEvent(updateAppRoleDetailsAuditEvent);

        model.addAttribute("appRole", roleDto);
        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        session.removeAttribute("appRoleDetailsForm");
        session.removeAttribute("appRoleDetailsFormModel");
        session.removeAttribute("roleId");

        return "silas-administration/edit-role-details-confirmation";
    }

    @GetMapping("/silas-administration/roles/reorder")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppRolesOrderGet(Model model,
                                       RedirectAttributes redirectAttributes,
                                       HttpSession session) {
        Optional<String> appName = getObjectFromHttpSession(session, "appFilter", String.class);

        if (appName.isEmpty() || !StringUtils.hasText(appName.get())) {
            redirectAttributes.addFlashAttribute("appRolesErrorMessage", "Please select an application to reorder its roles");
            return "redirect:/admin/silas-administration#roles";
        }

        AppRolesOrderForm appRolesOrderForm = getObjectFromHttpSession(session, "appRolesOrderForm", AppRolesOrderForm.class).orElse(null);
        if (appRolesOrderForm == null) {
            List<AppRoleAdminDto> appRoles = appRoleService.getLaaAppRolesByAppName(appName.get());
            List<AppRolesOrderForm.AppRolesOrderDetailsForm> appRoleOrderDetailsForm = appRoles.stream()
                    .map(AppRolesOrderForm.AppRolesOrderDetailsForm::new)
                    .toList();
            appRolesOrderForm = AppRolesOrderForm.builder().appRoles(appRoleOrderDetailsForm).build();
        }

        model.addAttribute("appName", appName.get());
        model.addAttribute("appRolesOrderForm", appRolesOrderForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return "silas-administration/edit-app-roles-order";
    }

    @PostMapping("/silas-administration/roles/reorder")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppRolesOrderPost(@Valid AppRolesOrderForm appRolesOrderForm,
                                        BindingResult result,
                                        Model model,
                                        HttpSession session) {
        String appName = getObjectFromHttpSession(session, "appFilter", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App not selected for role ordering"));
        model.addAttribute("appName", appName);

        if (result.hasErrors()) {
            model.addAttribute("errorMessage", buildErrorMessages(result));
            return "silas-administration/edit-app-roles-order";
        }

        appRolesOrderForm.getAppRoles().sort(Comparator.comparingInt(AppRolesOrderForm.AppRolesOrderDetailsForm::getOrdinal));
        session.setAttribute("appRolesOrderForm", appRolesOrderForm);
        model.addAttribute("appRolesOrderList", appRolesOrderForm.getAppRoles());

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return "silas-administration/edit-app-roles-order-check-answers";
    }

    @PostMapping("/silas-administration/roles/reorder/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmEditAppRolesOrderPost(Authentication authentication, Model model,
                                               HttpSession session) {
        AppRolesOrderForm appRolesOrderForm = getObjectFromHttpSession(session, "appRolesOrderForm", AppRolesOrderForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App role order details not found in session"));
        String appName = getObjectFromHttpSession(session, "appFilter", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

        appRoleService.updateAppRolesOrder(appRolesOrderForm.getAppRoles());

        UserProfile userProfile = loginService.getCurrentProfile(authentication);
        UpdateAppRoleDisplayOrderAuditEvent updateAppOrdinalAuditEvent = new UpdateAppRoleDisplayOrderAuditEvent(currentUserDto,
                userProfile.getId(), appName, appName);
        eventService.logEvent(updateAppOrdinalAuditEvent);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);
        // remove only relevant role-order/session attributes
        session.removeAttribute("appFilter");
        session.removeAttribute("roleId");
        session.removeAttribute("appRolesOrderForm");

        return "silas-administration/edit-app-roles-order-confirmation";
    }

    @GetMapping("/silas-administration/delete-role")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELETE_LAA_APP_ROLE)")
    public String deleteAppRoleGet(Model model,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
        Optional<String> appNameOptional = getObjectFromHttpSession(session, "appFilter", String.class);

        if (appNameOptional.isEmpty() || !StringUtils.hasText(appNameOptional.get())) {
            redirectAttributes.addFlashAttribute("appRolesErrorMessage", "Please select an application to delete its roles");
            return "redirect:/admin/silas-administration#roles";
        }

        String appName = appNameOptional.get();

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        List<AppRoleAdminDto> roles = appRoleService.getLaaAppRolesByAppName(appName);
        model.addAttribute("appName", appName);
        model.addAttribute("roles", roles);

        session.setAttribute("appFilter", appName);

        return "silas-administration/delete-app-roles";
    }

    @PostMapping("/silas-administration/delete-role/{roleId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELETE_LAA_APP_ROLE)")
    public String deleteAppRolePost(@PathVariable String roleId,
                                    @RequestParam("roleName") String roleName,
                                    @RequestParam("appName") String appName,
                                    Model model,
                                    HttpSession session) {
        String appNameInSession = getObjectFromHttpSession(session, "appFilter", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App not selected for role ordering"));
        model.addAttribute("appName", appNameInSession);

        if (!appName.equals(appNameInSession)) {
            log.error("App name mismatch for role ID {}: expected '{}', got '{}'", roleId, appNameInSession, appName);
            model.addAttribute("errorMessage", "Error while processing app role management");
            return "errors/error-generic";
        }

        session.setAttribute("roleIdForDeletion", roleId);
        session.setAttribute("roleNameForDeletion", roleName);

        model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

        return String.format("redirect:/admin/silas-administration/delete-role/%s/reason", roleId);
    }

    @GetMapping("/silas-administration/delete-role/{roleId}/reason")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELETE_LAA_APP_ROLE)")
    public String showDeleteAppRoleReasonPage(@PathVariable String roleId,
                                              HttpSession session,
                                              Model model) {
        String appName = getObjectFromHttpSession(session, "appFilter", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        String roleIdFromSession = getObjectFromHttpSession(session, "roleIdForDeletion", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        String roleName = getObjectFromHttpSession(session, "roleNameForDeletion", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role name not found in session"));

        try {
            if (!roleIdFromSession.equals(roleId)) {
                log.error("Role ID mismatch for role ID {}: expected '{}', got '{}'", roleId, roleIdFromSession, roleId);
                model.addAttribute("errorMessage", "Error while processing app role management");
                return "errors/error-generic";
            }

            AppRoleDto appRole = appRoleService.findById(roleId).orElseThrow(() -> new RuntimeException("App Role not found"));

            if (!appRole.getName().equals(roleName)) {
                log.warn("Role name mismatch for role ID {}: expected '{}', got '{}'", roleId, appRole.getName(), roleName);
                model.addAttribute("errorMessage", "Role name does not match the expected value for the selected role");
                return "errors/error-generic";
            }

            if (!appName.equals(appRole.getApp().getName())) {
                log.warn("App name mismatch for role ID {}: expected '{}', got '{}'", roleId, appRole.getApp().getName(), appName);
                model.addAttribute("errorMessage", "App name does not match the expected value for the selected role");
                return "errors/error-generic";
            }

            DeleteAppRoleReasonForm reasonForm = getObjectFromHttpSession(session, "deleteAppRoleReasonForm",
                    DeleteAppRoleReasonForm.class).orElse(DeleteAppRoleReasonForm.builder().appRoleId(roleId).appName(appName).build());
            model.addAttribute("deleteAppRoleReasonForm", reasonForm);
            model.addAttribute("roleName", roleName);
            model.addAttribute("appName", appName);
            model.addAttribute("roleId", roleId);

            model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

            return "silas-administration/delete-app-role-reason";

        } catch (Exception e) {
            log.error("Error loading reason page for app role {}: {}", roleId, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the page");
            return "errors/error-generic";
        }
    }

    @PostMapping("/silas-administration/delete-role/{roleId}/reason")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELETE_LAA_APP_ROLE)")
    public String processDeleteAppRoleReasonSubmission(@PathVariable String roleId,
                                                       @Valid DeleteAppRoleReasonForm reasonForm,
                                                       BindingResult bindingResult,
                                                       HttpSession session,
                                                       Model model) {

        if (bindingResult.hasFieldErrors("reason")) {
            return "silas-administration/delete-app-role-reason";
        }

        String appName = getObjectFromHttpSession(session, "appFilter", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        String roleIdFromSession = getObjectFromHttpSession(session, "roleIdForDeletion", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        String roleName = getObjectFromHttpSession(session, "roleNameForDeletion", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role name not found in session"));

        AppRoleDto appRole = appRoleService.findById(roleIdFromSession).orElseThrow(() -> new RuntimeException("App Role not found"));

        if (!roleId.equals(roleIdFromSession)) {
            log.error("Role ID mismatch for role ID {}: expected '{}', got '{}'", roleId, roleIdFromSession, roleId);
            model.addAttribute("errorMessage", "Error while processing app role management");
            return "errors/error-generic";
        }

        if (!appRole.getName().equals(roleName)) {
            log.warn("Role name mismatch for role ID {}: expected '{}', got '{}'", roleIdFromSession, appRole.getName(), roleName);
            model.addAttribute("errorMessage", "Role name does not match the expected value for the selected role");
            return "errors/error-generic";
        }

        if (!appName.equals(appRole.getApp().getName())) {
            log.warn("App name mismatch for role ID {}: expected '{}', got '{}'", roleIdFromSession, appRole.getApp().getName(), appName);
            model.addAttribute("errorMessage", "App name does not match the expected value for the selected role");
            return "errors/error-generic";
        }

        if (!roleIdFromSession.equals(reasonForm.getAppRoleId())) {
            log.warn("Role ID mismatch for role ID {}: expected '{}', got '{}'", roleIdFromSession, appRole.getName(), roleName);
            model.addAttribute("errorMessage", "Role ID does not match the expected value for the selected role");
            return "errors/error-generic";
        }

        session.setAttribute("deleteAppRoleReasonForm", reasonForm);

        return String.format("redirect:/admin/silas-administration/delete-role/%s/check-answers", appRole.getId());
    }

    @GetMapping("/silas-administration/delete-role/{roleId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELETE_LAA_APP_ROLE)")
    public String showDeleteAppRoleCheckAnswersPage(@PathVariable String roleId,
                                                    HttpSession session,
                                                    Model model) {
        String appName = getObjectFromHttpSession(session, "appFilter", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        String roleIdFromSession = getObjectFromHttpSession(session, "roleIdForDeletion", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        String roleName = getObjectFromHttpSession(session, "roleNameForDeletion", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role name not found in session"));
        DeleteAppRoleReasonForm reasonForm = getObjectFromHttpSession(session, "deleteAppRoleReasonForm",
                DeleteAppRoleReasonForm.class).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Delete Role form not found in session"));

        try {
            if (!roleIdFromSession.equals(roleId) || !reasonForm.getAppRoleId().equals(roleId)) {
                log.error("Role ID mismatch for role ID {}: expected '{}', got '{}'", roleId, roleIdFromSession, roleId);
                model.addAttribute("errorMessage", "Error while processing app role management");
                return "errors/error-generic";
            }

            model.addAttribute("reason", reasonForm.getReason());
            model.addAttribute("roleName", roleName);
            model.addAttribute("appName", appName);
            model.addAttribute("roleId", roleId);
            model.addAttribute("noOfUserProfilesAffected", appRoleService.countNoOfRoleAssignments(roleId));
            model.addAttribute("noOfFirmsAffected", appRoleService.countNoOfFirmsWithRoleAssignments(roleId));

            model.addAttribute(ModelAttributes.PAGE_TITLE, SILAS_ADMINISTRATION_TITLE);

            return "silas-administration/delete-app-role-check-answers";

        } catch (Exception e) {
            log.error("Error loading role delete check answers page for app role {}: {}", roleId, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the page");
            return "errors/error-generic";
        }
    }

    @PostMapping("/silas-administration/delete-role/{roleId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELETE_LAA_APP_ROLE)")
    public String submitDeleteAppRoleCheckAnswersPage(@PathVariable String roleId,
                                                      HttpSession session,
                                                      Model model,
                                                      Authentication authentication) {
        String appName = getObjectFromHttpSession(session, "appFilter", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        String roleIdFromSession = getObjectFromHttpSession(session, "roleIdForDeletion", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        String roleName = getObjectFromHttpSession(session, "roleNameForDeletion", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role name not found in session"));

        AppRoleDto appRole = appRoleService.findById(roleIdFromSession).orElseThrow(() -> new RuntimeException("App Role not found"));

        if (!appRole.getName().equals(roleName)) {
            log.warn("Role name mismatch for role ID {}: expected '{}', got '{}'", roleIdFromSession, appRole.getName(), roleName);
            model.addAttribute("errorMessage", "Role name does not match the expected value for the selected role");
            return "errors/error-generic";
        }

        if (!appName.equals(appRole.getApp().getName())) {
            log.warn("App name mismatch for role ID {}: expected '{}', got '{}'", roleIdFromSession, appRole.getApp().getName(), appName);
            model.addAttribute("errorMessage", "App name does not match the expected value for the selected role");
            return "errors/error-generic";
        }

        DeleteAppRoleReasonForm reasonForm = getObjectFromHttpSession(session, "deleteAppRoleReasonForm",
                DeleteAppRoleReasonForm.class).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Delete Role form not found in session"));

        if (!roleIdFromSession.equals(roleId) || !reasonForm.getAppRoleId().equals(roleId)) {
            log.error("Role ID mismatch for role ID {}: expected '{}', got '{}'", roleId, roleIdFromSession, roleId);
            model.addAttribute("errorMessage", "Error while processing app role management");
            return "errors/error-generic";
        }

        UUID entraOid = loginService.getCurrentUser(authentication).getUserId();
        UUID userProfileId = loginService.getCurrentProfile(authentication).getId();
        appRoleService.deleteAppRole(userProfileId, entraOid, appName, roleIdFromSession, reasonForm.getReason());

        clearSessionAttributes(session);


        return "silas-administration/delete-app-roles-confirmation";
    }

    @GetMapping("/silas-administration/cancel/{tab}")
    public String cancel(HttpSession session, @PathVariable String tab) {
        clearSessionAttributes(session);

        if (!VALID_TABS.contains(tab)) {
            tab = "admin-apps";
        }

        return "redirect:/admin/silas-administration#" + tab;
    }

    private void clearSessionAttributes(HttpSession session) {
        List.of("appDetailsForm", "appDetailsFormModel", "appId", "appsOrderForm", "deleteAppRoleReasonForm",
                        "appRoleDetailsForm", "appRoleDetailsFormModel", "appFilter", "roleId", "appRolesOrderForm",
                        "roleIdForDeletion", "roleNameForDeletion")
                .forEach(session::removeAttribute);
    }

    private List<String> buildErrorMessages(BindingResult result) {
        return result.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .toList();
    }

    private AppDto findAppDtoOrThrow(String appId) {
        UUID uuid = parseUuid(appId, "appId");
        return appService.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App details not found"));
    }

    private AppRoleDto findAppRoleDtoOrThrow(String roleId) {
        UUID uuid = parseUuid(roleId, "roleId");
        return appRoleService.findById(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App role details not found"));
    }

    private UUID parseUuid(String id, String paramName) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID for {}: {}", paramName, id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, paramName + " is invalid");
        }
    }

    @GetMapping("/silas-administration/roles/create")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_LAA_APP_ROLE)")
    public String showRoleCreationForm(Model model, HttpSession session) {
        RoleCreationDto roleCreationDto = (RoleCreationDto) session.getAttribute("roleCreationDto");
        if (roleCreationDto == null) {
            roleCreationDto = new RoleCreationDto();
        }

        model.addAttribute("roleCreationDto", roleCreationDto);
        model.addAttribute("apps", appService.getAllLaaApps());
        model.addAttribute("userTypes", UserType.values());
        model.addAttribute("firmTypes", FirmType.values());

        return "silas-administration/create-role";
    }

    @PostMapping("/silas-administration/roles/create")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_LAA_APP_ROLE)")
    public String processRoleCreation(@Valid @ModelAttribute RoleCreationDto roleCreationDto,
                                      BindingResult bindingResult,
                                      Model model,
                                      HttpSession session) {

        // Validate role name uniqueness within app
        if (roleCreationDto.getParentAppId() != null && roleCreationDto.getName() != null) {
            if (appRoleService.isRoleNameExistsInApp(roleCreationDto.getName(), roleCreationDto.getParentAppId())) {
                bindingResult.rejectValue("name", "role.name.exists",
                    "A role with this name already exists in the selected application");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("apps", appService.getAllLaaApps());
            model.addAttribute("userTypes", UserType.values());
            model.addAttribute("firmTypes", FirmType.values());
            return "silas-administration/create-role";
        }

        roleCreationDto = appRoleService.enrichRoleCreationDto(roleCreationDto);

        session.setAttribute("roleCreationDto", roleCreationDto);

        return "redirect:/admin/silas-administration/roles/create/check-your-answers";
    }

    @GetMapping("/silas-administration/roles/create/check-your-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_LAA_APP_ROLE)")
    public String showCheckYourAnswers(Model model, HttpSession session) {
        RoleCreationDto roleCreationDto = getObjectFromHttpSession(session, "roleCreationDto", RoleCreationDto.class)
                .orElseThrow(() -> new RuntimeException("App role details not found in session"));

        model.addAttribute("roleCreationDto", roleCreationDto);
        return "silas-administration/create-role-check-answers";
    }

    @PostMapping("/silas-administration/roles/create/check-your-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_LAA_APP_ROLE)")
    public String confirmCheckYourAnswers(HttpSession session, RedirectAttributes redirectAttributes) {
        RoleCreationDto roleCreationDto = getObjectFromHttpSession(session, "roleCreationDto", RoleCreationDto.class)
                .orElseThrow(() -> new RuntimeException("App role details not found in session"));

        try {
            appRoleService.createRole(roleCreationDto);
            session.setAttribute("createdRole", roleCreationDto);
            session.removeAttribute("roleCreationDto");
        } catch (Exception e) {
            log.error("Error creating role: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                "Failed to create role: " + e.getMessage());
            return "redirect:/admin/silas-administration?tab=roles#roles";
        }

        return "redirect:/admin/silas-administration/roles/create/confirmation";
    }

    @GetMapping("/silas-administration/roles/create/confirmation")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_LAA_APP_ROLE)")
    public String showRoleCreated(Model model, HttpSession session) {
        Optional<RoleCreationDto> createdRoleOptional = getObjectFromHttpSession(session, "createdRole", RoleCreationDto.class);

        if (createdRoleOptional.isPresent()) {
            RoleCreationDto createdRole = createdRoleOptional.get();
            model.addAttribute("createdRole", createdRole);
        } else {
            log.error("No createdRole attribute was present in request.");
        }

        session.removeAttribute("createdRole");
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Role created");
        return "silas-administration/create-role-confirmation";
    }
}
