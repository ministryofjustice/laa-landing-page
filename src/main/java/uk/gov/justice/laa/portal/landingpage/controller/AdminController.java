package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppDetailsAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppDisplayOrderAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppRoleDetailsAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppRoleDisplayOrderAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
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

    private static final String S_APP_DETAILS_FORM = "appDetailsForm";
    private static final String S_APP_DETAILS_FORM_MODEL = "appDetailsFormModel";
    private static final String S_APP_ID = "appId";
    private static final String S_APPS_ORDER_FORM = "appsOrderForm";
    private static final String S_APP_ROLE_DETAILS_FORM = "appRoleDetailsForm";
    private static final String S_APP_ROLE_DETAILS_FORM_MODEL = "appRoleDetailsFormModel";
    private static final String S_APP_FILTER = "appFilter";
    private static final String S_ROLE_ID = "roleId";
    private static final String S_APP_ROLES_ORDER_FORM = "appRolesOrderForm";

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

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        model.addAttribute("activeTab", tab);

        // Clear any session details from older operations (do not store Model in session)
        session.removeAttribute(S_APP_DETAILS_FORM);
        session.removeAttribute(S_APP_DETAILS_FORM_MODEL);
        session.removeAttribute(S_APP_ID);
        session.removeAttribute(S_APPS_ORDER_FORM);
        session.removeAttribute(S_APP_ROLE_DETAILS_FORM);
        session.removeAttribute(S_APP_ROLE_DETAILS_FORM_MODEL);

        // Load all admin apps data for admin-apps tab
        List<AdminAppDto> adminApps = adminService.getAllAdminApps();
        model.addAttribute("adminApps", adminApps);

        // Load all apps data for apps tab
        List<AppDto> apps = appService.getAllLaaApps();
        model.addAttribute("apps", apps);

        List<AppRoleAdminDto> roles;

        if (appFilter == null) {
            appFilter = getObjectFromHttpSession(session, S_APP_FILTER, String.class).orElse(null);
        }

        if (appFilter != null && !appFilter.isEmpty()) {
            roles = appRoleService.getLaaAppRolesByAppName(appFilter);
        } else {
            roles = appRoleService.getAllLaaAppRoles();
        }

        model.addAttribute("roles", roles);
        model.addAttribute("appFilter", appFilter);
        session.setAttribute("appFilter", appFilter);

        // Get distinct app names for filter dropdown
        List<String> appNames = apps.stream()
                .map(AppDto::getName)
                .distinct()
                .sorted()
                .toList();
        model.addAttribute("appNames", appNames);

        return "silas-administration/administration";
    }

    @GetMapping("/silas-administration/app/{appId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppDetailsGet(@PathVariable String appId,
                                    Model model,
                                    HttpSession session) {
        AppDto appDto = findAppDtoOrThrow(appId);
        model.addAttribute("app", appDto);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        model.addAttribute(ModelAttributes.PAGE_SUMMARY, "Legal Aid Services");
        AppDetailsForm appDetailsForm = (AppDetailsForm) session.getAttribute(S_APP_DETAILS_FORM);

        if (appDetailsForm == null) {
            appDetailsForm = AppDetailsForm.builder()
                    .appId(appDto.getId())
                    .enabled(appDto.isEnabled())
                    .description(appDto.getDescription())
                    .build();
        }

        model.addAttribute("appDetailsForm", appDetailsForm);
        session.setAttribute(S_APP_DETAILS_FORM, appDetailsForm);
        return "silas-administration/edit-app-details";
    }

    @PostMapping("/silas-administration/app/{appId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppDetailsPost(@PathVariable String appId,
                                     @Valid AppDetailsForm appDetailsForm,
                                     BindingResult result,
                                     Model model,
                                     HttpSession session) {
        if (result.hasErrors()) {
            String errorMessage = buildErrorString(result);
            AppDto appDto = findAppDtoOrThrow(appId);
            model.addAttribute("app", appDto);
            model.addAttribute("errorMessage", errorMessage);
            return "silas-administration/edit-app-details";
        }

        session.setAttribute(S_APP_ID, appId);
        session.setAttribute(S_APP_DETAILS_FORM, appDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return String.format("redirect:/admin/silas-administration/app/%s/check-answers", appId);
    }

    @GetMapping("/silas-administration/app/{appId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppDetailsGet(@PathVariable String appId,
                                       Model model,
                                       HttpSession session) {
        String appIdFromSession = getObjectFromHttpSession(session, S_APP_ID, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App ID not found in session"));
        AppDetailsForm appDetailsForm = getObjectFromHttpSession(session, S_APP_DETAILS_FORM, AppDetailsForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App details not found in session"));

        if (!appId.equals(appIdFromSession)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request for app details change");
        }

        AppDto appDto = findAppDtoOrThrow(appId);

        model.addAttribute("app", appDto);
        model.addAttribute("appDetailsForm", appDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return "silas-administration/edit-app-details-check-answers";
    }

    @PostMapping("/silas-administration/app/{appId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppDetailsPost(@PathVariable String appId,
                                        Authentication authentication,
                                        Model model,
                                        HttpSession session) {
        String appIdFromSession = getObjectFromHttpSession(session, S_APP_ID, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App ID not found in session"));
        AppDetailsForm appDetailsForm = getObjectFromHttpSession(session, S_APP_DETAILS_FORM, AppDetailsForm.class)
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
        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        session.removeAttribute(S_APP_DETAILS_FORM);
        session.removeAttribute(S_APP_DETAILS_FORM_MODEL);
        session.removeAttribute(S_APP_ID);

        return "silas-administration/edit-app-details-confirmation";
    }

    @GetMapping("/silas-administration/apps/reorder")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppOrderGet(Model model,
                                  HttpSession session) {
        AppsOrderForm appsOrderForm = getObjectFromHttpSession(session, S_APPS_ORDER_FORM, AppsOrderForm.class).orElse(null);
        if (appsOrderForm == null) {
            List<AppDto> allLaaApps = appService.getAllLaaApps();
            List<AppsOrderForm.AppOrderDetailsForm> appOrderDetailsForm = allLaaApps.stream()
                    .map(AppsOrderForm.AppOrderDetailsForm::new)
                    .toList();
            appsOrderForm = AppsOrderForm.builder().apps(appOrderDetailsForm).build();
        }
        model.addAttribute("appsOrderForm", appsOrderForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return "silas-administration/edit-apps-order";
    }

    @PostMapping("/silas-administration/apps/reorder")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppOrderPost(@Valid AppsOrderForm appsOrderForm,
                                   BindingResult result,
                                   Model model,
                                   HttpSession session) {
        if (result.hasErrors()) {
            String errorMessage = buildErrorString(result);
            model.addAttribute("errorMessage", errorMessage);
            return "silas-administration/edit-apps-order";
        }

        appsOrderForm.getApps().sort(Comparator.comparingInt(AppsOrderForm.AppOrderDetailsForm::getOrdinal));
        session.setAttribute(S_APPS_ORDER_FORM, appsOrderForm);
        model.addAttribute("appsOrderList", appsOrderForm.getApps());

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return "silas-administration/edit-apps-order-check-answers";
    }

    @PostMapping("/silas-administration/apps/reorder/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmEditAppOrderPost(Authentication authentication, Model model,
                                          HttpSession session) {
        AppsOrderForm appsOrderForm = getObjectFromHttpSession(session, S_APPS_ORDER_FORM, AppsOrderForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App order details not found in session"));
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

        appService.updateAppsOrder(appsOrderForm.getApps());

        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        UpdateAppDisplayOrderAuditEvent updateAppOrdinalAuditEvent = new UpdateAppDisplayOrderAuditEvent(currentUserDto,
                currentUserProfile.getId());
        eventService.logEvent(updateAppOrdinalAuditEvent);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        session.removeAttribute(S_APPS_ORDER_FORM);

        return "silas-administration/edit-apps-order-confirmation";
    }

    @GetMapping("/silas-administration/role/{roleId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppRoleDetailsGet(@PathVariable String roleId,
                                    Model model,
                                    HttpSession session) {
        AppRoleDto appRoleDto = findAppRoleDtoOrThrow(roleId);
        model.addAttribute("appRole", appRoleDto);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        model.addAttribute(ModelAttributes.PAGE_SUMMARY, "Legal Aid Services");
        AppRoleDetailsForm appRoleDetailsForm = (AppRoleDetailsForm) session.getAttribute(S_APP_ROLE_DETAILS_FORM);

        if (appRoleDetailsForm == null) {
            appRoleDetailsForm = AppRoleDetailsForm.builder()
                    .appRoleId(appRoleDto.getId())
                    .name(appRoleDto.getName())
                    .description(appRoleDto.getDescription())
                    .build();
        }

        model.addAttribute("appRoleDetailsForm", appRoleDetailsForm);
        session.setAttribute(S_APP_ROLE_DETAILS_FORM, appRoleDetailsForm);

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
            String errorMessage = buildErrorString(result);
            AppRoleDto appRoleDto = findAppRoleDtoOrThrow(roleId);
            model.addAttribute("appRole", appRoleDto);
            model.addAttribute("errorMessage", errorMessage);
            return "silas-administration/edit-role-details";
        }

        session.setAttribute(S_ROLE_ID, roleId);
        session.setAttribute(S_APP_ROLE_DETAILS_FORM, appRoleDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return String.format("redirect:/admin/silas-administration/role/%s/check-answers", roleId);
    }

    @GetMapping("/silas-administration/role/{roleId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppRoleDetailsGet(@PathVariable String roleId,
                                       Model model,
                                       HttpSession session) {
        String roleIdFromSession = getObjectFromHttpSession(session, S_ROLE_ID, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        AppRoleDetailsForm roleDetailsForm = getObjectFromHttpSession(session, S_APP_ROLE_DETAILS_FORM, AppRoleDetailsForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App role details not found in session"));

        if (!roleId.equals(roleIdFromSession)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid request for app details change");
        }

        AppRoleDto roleDto = findAppRoleDtoOrThrow(roleId);

        model.addAttribute("appRole", roleDto);
        model.addAttribute("appRoleDetailsForm", roleDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return "silas-administration/edit-role-details-check-answers";
    }

    @PostMapping("/silas-administration/role/{roleId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppRoleDetailsPost(@PathVariable String roleId,
                                        Authentication authentication,
                                        Model model,
                                        HttpSession session) {
        String roleIdFromSession = getObjectFromHttpSession(session, S_ROLE_ID, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App Role ID not found in session"));
        AppRoleDetailsForm roleDetailsForm = getObjectFromHttpSession(session, S_APP_ROLE_DETAILS_FORM, AppRoleDetailsForm.class)
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
        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        session.removeAttribute(S_APP_ROLE_DETAILS_FORM);
        session.removeAttribute(S_APP_ROLE_DETAILS_FORM_MODEL);
        session.removeAttribute(S_ROLE_ID);

        return "silas-administration/edit-role-details-confirmation";
    }

    @GetMapping("/silas-administration/roles/reorder")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppRolesOrderGet(Model model,
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {
        Optional<String> appName = getObjectFromHttpSession(session, S_APP_FILTER, String.class);

        if (appName.isEmpty() || !StringUtils.hasText(appName.get())) {
            redirectAttributes.addFlashAttribute("appRolesErrorMessage", "Please select an application to reorder its roles");
            return "redirect:/admin/silas-administration#roles";
        }

        AppRolesOrderForm appRolesOrderForm = getObjectFromHttpSession(session, S_APP_ROLES_ORDER_FORM, AppRolesOrderForm.class).orElse(null);
        if (appRolesOrderForm == null) {
            List<AppRoleAdminDto> appRoles = appRoleService.getLaaAppRolesByAppName(appName.get());
            List<AppRolesOrderForm.AppRolesOrderDetailsForm> appRoleOrderDetailsForm = appRoles.stream()
                    .map(AppRolesOrderForm.AppRolesOrderDetailsForm::new)
                    .toList();
            appRolesOrderForm = AppRolesOrderForm.builder().appRoles(appRoleOrderDetailsForm).build();
        }

        model.addAttribute("appName", appName.get());
        model.addAttribute("appRolesOrderForm", appRolesOrderForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return "silas-administration/edit-app-roles-order";
    }

    @PostMapping("/silas-administration/roles/reorder")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String editAppRolesOrderPost(@Valid AppRolesOrderForm appRolesOrderForm,
                                   BindingResult result,
                                   Model model,
                                   HttpSession session) {
        String appName = getObjectFromHttpSession(session, S_APP_FILTER, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App not selected for role ordering"));
        model.addAttribute("appName", appName);

        if (result.hasErrors()) {
            String errorMessage = buildErrorString(result);
            model.addAttribute("errorMessage", errorMessage);
            return "silas-administration/edit-app-roles-order";
        }

        appRolesOrderForm.getAppRoles().sort(Comparator.comparingInt(AppRolesOrderForm.AppRolesOrderDetailsForm::getOrdinal));
        session.setAttribute(S_APP_ROLES_ORDER_FORM, appRolesOrderForm);
        model.addAttribute("appRolesOrderList", appRolesOrderForm.getAppRoles());

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return "silas-administration/edit-app-roles-order-check-answers";
    }

    @PostMapping("/silas-administration/roles/reorder/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmEditAppRolesOrderPost(Authentication authentication, Model model,
                                          HttpSession session) {
        AppRolesOrderForm appRolesOrderForm = getObjectFromHttpSession(session, S_APP_ROLES_ORDER_FORM, AppRolesOrderForm.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App role order details not found in session"));
        String appName = getObjectFromHttpSession(session, S_APP_FILTER, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

        appRoleService.updateAppRolesOrder(appRolesOrderForm.getAppRoles());

        UserProfile userProfile = loginService.getCurrentProfile(authentication);
        UpdateAppRoleDisplayOrderAuditEvent updateAppOrdinalAuditEvent = new UpdateAppRoleDisplayOrderAuditEvent(currentUserDto,
                userProfile.getId(), appName, appName);
        eventService.logEvent(updateAppOrdinalAuditEvent);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        // remove only relevant role-order/session attributes
        session.removeAttribute(S_APP_FILTER);
        session.removeAttribute(S_ROLE_ID);
        session.removeAttribute(S_APP_ROLES_ORDER_FORM);

        return "silas-administration/edit-app-roles-order-confirmation";
    }

    @GetMapping("/silas-administration/delete-role")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELETE_LAA_APP_ROLE)")
    public String deleteAppRoleGet(Model model,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
        Optional<String> appNameOptional = getObjectFromHttpSession(session, S_APP_FILTER, String.class);

        if (appNameOptional.isEmpty() || !StringUtils.hasText(appNameOptional.get())) {
            redirectAttributes.addFlashAttribute("appRolesErrorMessage", "Please select an application to reorder its roles");
            return "redirect:/admin/silas-administration#roles";
        }

        String appName = appNameOptional.get();

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        List<AppRoleAdminDto> roles = appRoleService.getLaaAppRolesByAppName(appName);
        model.addAttribute("appName", appName);
        model.addAttribute("roles", roles);

        session.setAttribute(S_APP_FILTER, appName);

        return "silas-administration/delete-app-roles";
    }

    @PostMapping("/silas-administration/delete-role/{roleId}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).DELETE_LAA_APP_ROLE)")
    public String deleteAppRolePost(@PathVariable String roleId,
                                    @RequestParam("roleName") String roleName,
                                    @RequestParam("appName") String appName,
                                    Model model,
                                    HttpSession session) {
        String appNameInSession = getObjectFromHttpSession(session, S_APP_FILTER, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App not selected for role ordering"));
        model.addAttribute("appName", appNameInSession);

        if(!appName.equals(appNameInSession)) {
            log.error("App name mismatch for role ID {}: expected '{}', got '{}'", roleId, appNameInSession, appName);
            model.addAttribute("errorMessage", "Error while processing app role management");
            return "errors/error-generic";
        }

        session.setAttribute("ROLE_ID_FOR_DELETION", roleId);
        session.setAttribute("ROLE_NAME_FOR_DELETION", roleName);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return String.format("redirect:/admin/silas-administration/delete-role/%s/reason", roleId);
    }

    @GetMapping("/silas-administration/delete-role/{roleId}/reason")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String showDeleteAppRoleReasonPage(@PathVariable String roleId,
                                             HttpSession session,
                                             Model model) {
        String appName = getObjectFromHttpSession(session, S_APP_FILTER, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        String roleIdFromSession = getObjectFromHttpSession(session, "ROLE_ID_FOR_DELETION", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        String roleName = getObjectFromHttpSession(session, "ROLE_NAME_FOR_DELETION", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role name not found in session"));

        try {
            if(!roleIdFromSession.equals(roleId)) {
                log.error("Role ID mismatch for role ID {}: expected '{}', got '{}'", roleId, roleIdFromSession, roleId);
                model.addAttribute("errorMessage", "Error while processing app role management");
                return "errors/error-generic";
            }

            AppRoleDto appRole = appRoleService.findById(roleId). orElseThrow(() -> new RuntimeException("App Role not found"));

            if(!appRole.getName().equals(roleName)) {
                log.warn("Role name mismatch for role ID {}: expected '{}', got '{}'", roleId, appRole.getName(), roleName);
                model.addAttribute("errorMessage", "Role name does not match the expected value for the selected role");
                return "errors/error-generic";
            }

            if(!appName.equals(appRole.getApp().getName())) {
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

            model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

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

        String appName = getObjectFromHttpSession(session, S_APP_FILTER, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        String roleIdFromSession = getObjectFromHttpSession(session, "ROLE_ID_FOR_DELETION", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        String roleName = getObjectFromHttpSession(session, "ROLE_NAME_FOR_DELETION", String.class)
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
        String appName = getObjectFromHttpSession(session, S_APP_FILTER, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        String roleIdFromSession = getObjectFromHttpSession(session, "ROLE_ID_FOR_DELETION", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        String roleName = getObjectFromHttpSession(session, "ROLE_NAME_FOR_DELETION", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role name not found in session"));

        try {
            DeleteAppRoleReasonForm reasonForm = getObjectFromHttpSession(session, "deleteAppRoleReasonForm",
                    DeleteAppRoleReasonForm.class).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Delete Role form not found in session"));

            if(!roleIdFromSession.equals(roleId) || !reasonForm.getAppRoleId().equals(roleId)) {
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

            model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

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
        String appName = getObjectFromHttpSession(session, S_APP_FILTER, String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App name not found in session"));
        String roleIdFromSession = getObjectFromHttpSession(session, "ROLE_ID_FOR_DELETION", String.class)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role ID not found in session"));
        String roleName = getObjectFromHttpSession(session, "ROLE_NAME_FOR_DELETION", String.class)
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

        if(!roleIdFromSession.equals(roleId) || !reasonForm.getAppRoleId().equals(roleId)) {
            log.error("Role ID mismatch for role ID {}: expected '{}', got '{}'", roleId, roleIdFromSession, roleId);
            model.addAttribute("errorMessage", "Error while processing app role management");
            return "errors/error-generic";
        }

        UUID entraOid = loginService.getCurrentUser(authentication).getUserId();
        UUID userProfileId = loginService.getCurrentProfile(authentication).getId();
        appRoleService.deleteAppRole(userProfileId, entraOid, appName, reasonForm.getReason(), roleIdFromSession);


        return "redirect:/admin/silas-administration/delete-role-confirmation";
    }

    @GetMapping("/silas-administration/cancel/{tab}")
    public String cancel(HttpSession session, @PathVariable String tab) {
        session.removeAttribute(S_APP_DETAILS_FORM);
        session.removeAttribute(S_APP_DETAILS_FORM_MODEL);
        session.removeAttribute(S_APP_ID);
        session.removeAttribute(S_APP_ROLE_DETAILS_FORM);
        session.removeAttribute(S_APP_ROLE_DETAILS_FORM_MODEL);
        session.removeAttribute(S_APPS_ORDER_FORM);
        session.removeAttribute(S_APP_FILTER);
        session.removeAttribute(S_ROLE_ID);
        session.removeAttribute(S_APP_ROLES_ORDER_FORM);

        if (!VALID_TABS.contains(tab)) {
            tab = "admin-apps";
        }

        return "redirect:/admin/silas-administration#" + tab;
    }

    private static String buildErrorString(BindingResult result) {
        StringBuilder errorMessage = new StringBuilder();
        List<ObjectError> errors = result.getAllErrors();
        for (int i = 0; i < errors.size(); i++) {
            ObjectError error = errors.get(i);
            errorMessage.append(error.getDefaultMessage());
            if (i < errors.size() - 1) {
                errorMessage.append("<br/>");
            }
        }
        return errorMessage.toString();
    }

    // helper methods
    private UUID parseUuid(String id, String paramName) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid UUID for {}: {}", paramName, id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, paramName + " is invalid");
        }
    }

    private AppDto findAppDtoOrThrow(String appId) {
        UUID uuid = parseUuid(appId, "appId");
        return appService.findById(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App details not found"));
    }

    private AppRoleDto findAppRoleDtoOrThrow(String roleId) {
        UUID uuid = parseUuid(roleId, "roleId");
        return appRoleService.findById(uuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "App role details not found"));
    }

}
