package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditTableSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteUserAttemptAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteUserSuccessAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.model.DeletedUser;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AuditController {

    private final UserService userService;
    private final LoginService loginService;
    private final EventService eventService;

    /**
     * Display the User Access Audit Table
     * Shows all registered users including those without firm profiles
     */
    @GetMapping("/users/audit")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_AUDIT_TABLE)")
    public String displayAuditTable(
            @ModelAttribute AuditTableSearchCriteria criteria,
            Model model) {

        log.debug("AuditController.displayAuditTable - {}", criteria);
        // Get audit users
        PaginatedAuditUsers paginatedUsers = userService.getAuditUsers(
                criteria.getSearch(), criteria.getSelectedFirmId(), criteria.getSilasRole(),
                criteria.getSelectedAppId(), criteria.getSelectedUserType(), criteria.getMultiFirm(),
                criteria.getPage(), criteria.getSize(), criteria.getSort(), criteria.getDirection());
        // Build firm search form
        FirmSearchForm firmSearchForm = new FirmSearchForm(criteria.getFirmSearch(), criteria.getSelectedFirmId());
        // Add attributes to model
        buildDisplayAuditTableModel(criteria, model, paginatedUsers, firmSearchForm);

        return "user-audit/users";
    }

    private void buildDisplayAuditTableModel(AuditTableSearchCriteria criteria, Model model,
            PaginatedAuditUsers paginatedUsers, FirmSearchForm firmSearchForm) {
        model.addAttribute("users", paginatedUsers.getUsers());
        model.addAttribute("requestedPageSize", criteria.getSize());
        model.addAttribute("actualPageSize", paginatedUsers.getUsers().size());
        model.addAttribute("page", criteria.getPage());
        model.addAttribute("totalUsers", paginatedUsers.getTotalUsers());
        model.addAttribute("totalPages", paginatedUsers.getTotalPages());
        model.addAttribute("search", criteria.getSearch());
        model.addAttribute("firmSearch", firmSearchForm);
        // Get all SiLAS roles for dropdown filter
        List<AppRoleDto> silasRoles = userService.getAllSilasRoles();
        model.addAttribute("silasRoles", silasRoles);
        List<AppDto> apps = userService.getApps();
        model.addAttribute("apps", apps);
        model.addAttribute("selectedSilasRole", criteria.getSilasRole() != null ? criteria.getSilasRole() : "");
        model.addAttribute("selectedAppId",
                criteria.getSelectedAppId() != null ? criteria.getSelectedAppId().toString() : "");
        model.addAttribute("selectedUserType",
                criteria.getSelectedUserType() != null ? criteria.getSelectedUserType().toString() : "");
        model.addAttribute("multiFirm", criteria.getMultiFirm() != null ? criteria.getMultiFirm().toString() : "");
        model.addAttribute("sort", criteria.getSort());
        model.addAttribute("direction", criteria.getDirection());
    }

    /**
     * Display detailed audit information for a specific user
     */
    @GetMapping("/users/audit/{id}")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_AUDIT_TABLE)")
    public String displayUserAuditDetail(@PathVariable("id") UUID userId,
            @RequestParam(name = "profilePage", defaultValue = "1") int profilePage,
            @RequestParam(name = "profileSize", defaultValue = "3") int profileSize,
            @RequestParam(name = "isEntraId", defaultValue = "false") boolean isEntraId,
            Model model) {

        log.debug(
                "AuditController.displayUserAuditDetail - userId: '{}', isEntraId: {}, profilePage: {}, profileSize: {}",
                userId, isEntraId, profilePage, profileSize);

        AuditUserDetailDto userDetail;
        // Determine if this is an EntraUser ID or UserProfile ID
        if (isEntraId) {
            // Load user by EntraUser ID (for users without profiles)
            userDetail = userService.getAuditUserDetailByEntraId(userId);
        } else {
            // Try to load by UserProfile ID first (existing behavior)
            try {
                userDetail = userService.getAuditUserDetail(userId, profilePage, profileSize);

            } catch (IllegalArgumentException e) {
                // If profile not found, try as EntraUser ID
                log.debug("Profile not found with ID {}, attempting to load as EntraUser ID",
                        userId);
                userDetail = userService.getAuditUserDetailByEntraId(userId);
            }
        }

        Map<String, List<String>> appAssignments = getAppsSortedAlphabetically(userDetail);

        // Add attributes to model
        model.addAttribute("user", userDetail);
        model.addAttribute("profileId", userId); // Add profile ID for pagination links
        model.addAttribute("profilePage", profilePage);
        model.addAttribute("profileSize", profileSize);
        model.addAttribute("appAssignments", appAssignments);

        return "user-audit/details";
    }

    private static Map<String, List<String>> getAppsSortedAlphabetically(AuditUserDetailDto userDetail) {
        //map to roles
        List<AppRoleDto> roles = userDetail.getProfiles()
                .stream()
                .flatMap(profile -> profile.getRoles().stream())
                .toList();
        return roles.stream()
                .collect(Collectors.groupingBy(
                        role -> role.getApp().getName(),
                        // Use TreeMap to keep keys ordered.
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                        // Map role -> role name, and sort the list values
                        Collectors.collectingAndThen(
                                Collectors.mapping(AppRoleDto::getName, Collectors.toList()),
                                list -> list.stream()
                                        .filter(Objects::nonNull)
                                        .sorted(String.CASE_INSENSITIVE_ORDER)
                                        .collect(Collectors.toList())
                        )
                ));
    }

    /**
     * Display delete confirmation page for a user without a profile
     */
    @GetMapping("/users/audit/entra/{id}/delete")
    @PreAuthorize("@accessControlService.canDeleteUserWithoutProfile(#id)")
    public String deleteUserWithoutProfileConfirm(@PathVariable String id, Model model) {
        log.debug("AuditController.deleteUserWithoutProfileConfirm - entraUserId: '{}'", id);

        AuditUserDetailDto userDetail = userService.getAuditUserDetailByEntraId(UUID.fromString(id));
        model.addAttribute("user", userDetail);
        model.addAttribute(ModelAttributes.PAGE_TITLE,
                "Remove access - " + userDetail.getFullName());

        return "user-audit/delete-user-without-profile-reason";
    }

    /**
     * Delete a user without a profile Notifies Entra and removes from database
     */
    @PostMapping("/users/audit/entra/{id}/delete")
    @PreAuthorize("@accessControlService.canDeleteUserWithoutProfile(#id)")
    public String deleteUserWithoutProfile(@PathVariable String id,
            @RequestParam("reason") String reason, Authentication authentication,
            HttpSession session, Model model) {

        log.debug("AuditController.deleteUserWithoutProfile - entraUserId: '{}', reason: '{}'", id,
                reason);

        AuditUserDetailDto userDetail = userService.getAuditUserDetailByEntraId(UUID.fromString(id));

        if (reason == null || reason.trim().length() < 10) {
            model.addAttribute("user", userDetail);
            model.addAttribute("fieldErrorMessage",
                    "Please enter a reason (minimum 10 characters).");
            model.addAttribute(ModelAttributes.PAGE_TITLE,
                    "Remove access - " + userDetail.getFullName());
            return "user-audit/delete-user-without-profile-reason";
        }

        EntraUser current = loginService.getCurrentEntraUser(authentication);
        try {
            DeletedUser deletedUser = userService.deleteEntraUserWithoutProfile(id, reason.trim(), current.getId());
            DeleteUserSuccessAuditEvent deleteUserAuditEvent = new DeleteUserSuccessAuditEvent(reason.trim(),
                    current.getId(), deletedUser);
            eventService.logEvent(deleteUserAuditEvent);
        } catch (RuntimeException ex) {
            log.error("Failed to delete user without profile {}: {}", id, ex.getMessage(), ex);
            DeleteUserAttemptAuditEvent deleteUserAttemptAuditEvent = new DeleteUserAttemptAuditEvent(id, reason.trim(),
                    current.getId(),
                    ex.getMessage());
            eventService.logEvent(deleteUserAttemptAuditEvent);
            model.addAttribute("user", userDetail);
            model.addAttribute("globalErrorMessage", "User delete failed, please try again later");
            model.addAttribute(ModelAttributes.PAGE_TITLE,
                    "Remove access - " + userDetail.getFullName());
            return "user-audit/delete-user-without-profile-reason";
        }

        model.addAttribute("deletedUserFullName", userDetail.getFullName());
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User deleted");

        return "user-audit/delete-user-success";
    }
}
