package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AdminAppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleAdminDto;
import uk.gov.justice.laa.portal.landingpage.dto.RoleCreationDto;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppDetailsAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateAppDisplayOrderAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.forms.AppDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.AppsOrderForm;
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

        // Clear any session details from older operations
        session.removeAttribute("appDetailsForm");
        session.removeAttribute("appDetailsFormModel");
        session.removeAttribute("appId");
        session.removeAttribute("appsOrderForm");

        // Load all admin apps data for admin-apps tab
        List<AdminAppDto> adminApps = adminService.getAllAdminApps();
        model.addAttribute("adminApps", adminApps);

        // Load all apps data for apps tab
        List<AppDto> apps = appService.getAllLaaApps();
        model.addAttribute("apps", apps);

        List<AppRoleAdminDto> roles;
        if (appFilter != null && !appFilter.isEmpty()) {
            roles = adminService.getAppRolesByApp(appFilter);
        } else {
            roles = adminService.getAllAppRoles();
        }
        model.addAttribute("roles", roles);
        model.addAttribute("appFilter", appFilter);

        // Get distinct app names for filter dropdown
        List<String> appNames = adminService.getAllApps().stream()
                .map(AppAdminDto::getName)
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
        AppDto appDto = appService.findById(UUID.fromString(appId))
                .orElseThrow(() -> new RuntimeException("App details not found"));
        model.addAttribute("app", appDto);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        model.addAttribute(ModelAttributes.PAGE_SUMMARY, "Legal Aid Services");
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
        session.setAttribute("appDetailsFormModel", model);
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
            AppDto appDto = appService.findById(UUID.fromString(appId))
                    .orElseThrow(() -> new RuntimeException("App details not found"));
            model.addAttribute("app", appDto);
            model.addAttribute("errorMessage", errorMessage);
            return "silas-administration/edit-app-details";
        }

        session.setAttribute("appId", appId);
        session.setAttribute("appDetailsForm", appDetailsForm);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return String.format("redirect:/admin/silas-administration/app/%s/check-answers", appId);
    }

    @GetMapping("/silas-administration/app/{appId}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmAppDetailsGet(@PathVariable String appId,
                                       Model model,
                                       HttpSession session) {
        String appIdFromSession = getObjectFromHttpSession(session, "appId", String.class)
                .orElseThrow(() -> new RuntimeException("App ID not found in session"));
        AppDetailsForm appDetailsForm = getObjectFromHttpSession(session, "appDetailsForm", AppDetailsForm.class)
                .orElseThrow(() -> new RuntimeException("App details not found in session"));

        if (!appId.equals(appIdFromSession)) {
            throw new RuntimeException("Invalid request for app details change");
        }

        AppDto appDto = appService.findById(UUID.fromString(appId))
                .orElseThrow(() -> new RuntimeException("App details not found"));

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
        String appIdFromSession = getObjectFromHttpSession(session, "appId", String.class)
                .orElseThrow(() -> new RuntimeException("App ID not found in session"));
        AppDetailsForm appDetailsForm = getObjectFromHttpSession(session, "appDetailsForm", AppDetailsForm.class)
                .orElseThrow(() -> new RuntimeException("App details not found in session"));

        if (!appId.equals(appIdFromSession)) {
            throw new RuntimeException("Invalid request for app details change");
        }

        AppDto appDto = appService.findById(UUID.fromString(appId))
                .orElseThrow(() -> new RuntimeException("App details not found"));
        String appName = appDto.getName();

        appDto.setEnabled(appDetailsForm.isEnabled());
        appDto.setDescription(appDetailsForm.getDescription());

        App updatedApp = appService.save(appDto);

        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UpdateAppDetailsAuditEvent updateAppDetailsAuditEvent = new UpdateAppDetailsAuditEvent(currentUserDto,
                appName, updatedApp.isEnabled(), appDto.isEnabled(), updatedApp.getDescription(), appDto.getDescription());
        eventService.logEvent(updateAppDetailsAuditEvent);

        model.addAttribute("app", appDto);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

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
        session.setAttribute("appsOrderForm", appsOrderForm);
        model.addAttribute("appsOrderList", appsOrderForm.getApps());

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");

        return "silas-administration/edit-apps-order-check-answers";
    }

    @PostMapping("/silas-administration/apps/reorder/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_LAA_APP_METADATA)")
    public String confirmEditAppOrderPost(Authentication authentication, Model model,
                                          HttpSession session) {
        AppsOrderForm appsOrderForm = getObjectFromHttpSession(session, "appsOrderForm", AppsOrderForm.class)
                .orElseThrow(() -> new RuntimeException("App order details not found in session"));
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);

        appService.updateAppsOrder(appsOrderForm.getApps());

        UpdateAppDisplayOrderAuditEvent updateAppOrdinalAuditEvent = new UpdateAppDisplayOrderAuditEvent(currentUserDto);
        eventService.logEvent(updateAppOrdinalAuditEvent);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "SiLAS Administration");
        session.removeAttribute("appsOrderForm");

        return "silas-administration/edit-apps-order-confirmation";
    }

    private static String buildErrorString(BindingResult result) {
        StringBuilder errorMessage = new StringBuilder();
        List<ObjectError> errors = result.getAllErrors();
        for (int i = 0; i < errors.size(); i++) {
            ObjectError error = errors.get(i);
            errorMessage.append(error.getDefaultMessage());
            if (i < errors.size() - 1) {
                errorMessage.append("\n");
            }
        }
        return errorMessage.toString();
    }

    @GetMapping("/silas-administration/roles/create")
    public String showRoleCreationForm(Model model, HttpSession session) {
        RoleCreationDto roleCreationDto = (RoleCreationDto) session.getAttribute("roleCreationDto");
        if (roleCreationDto == null) {
            roleCreationDto = new RoleCreationDto();
        }

        model.addAttribute("roleCreationDto", roleCreationDto);
        model.addAttribute("apps", adminService.getAllApps());
        model.addAttribute("userTypes", UserType.values());
        model.addAttribute("firmTypes", FirmType.values());

        return "silas-administration/create-role";
    }

    @PostMapping("/silas-administration/roles/create")
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
            model.addAttribute("apps", adminService.getAllApps());
            model.addAttribute("userTypes", UserType.values());
            model.addAttribute("firmTypes", FirmType.values());
            return "silas-administration/create-role";
        }

        roleCreationDto = appRoleService.enrichRoleCreationDto(roleCreationDto);

        session.setAttribute("roleCreationDto", roleCreationDto);

        return "redirect:/admin/silas-administration/roles/create/check-your-answers";
    }

    @GetMapping("/silas-administration/roles/create/check-your-answers")
    public String showCheckYourAnswers(Model model, HttpSession session) {
        RoleCreationDto roleCreationDto = (RoleCreationDto) session.getAttribute("roleCreationDto");
        if (roleCreationDto == null) {
            return "redirect:/admin/silas-administration/roles/create";
        }

        model.addAttribute("roleCreationDto", roleCreationDto);
        return "silas-administration/create-role-check-answers";
    }

    @PostMapping("/silas-administration/roles/create/confirm")
    public String confirmRoleCreation(HttpSession session, RedirectAttributes redirectAttributes) {
        RoleCreationDto roleCreationDto = (RoleCreationDto) session.getAttribute("roleCreationDto");
        if (roleCreationDto == null) {
            return "redirect:/admin/silas-administration/roles/create";
        }

        try {
            appRoleService.createRole(roleCreationDto);
            session.removeAttribute("roleCreationDto");
            redirectAttributes.addFlashAttribute("successMessage",
                "Role '" + roleCreationDto.getName() + "' has been created successfully.");
        } catch (Exception e) {
            log.error("Error creating role: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                "Failed to create role: " + e.getMessage());
        }

        return "redirect:/admin/silas-administration?tab=roles#roles";
    }
}
