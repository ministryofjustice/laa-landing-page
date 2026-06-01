package uk.gov.justice.laa.portal.landingpage.controller;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import uk.gov.justice.laa.portal.landingpage.auth.AuthenticatedUser;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditTableSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteUserAttemptAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteUserSuccessAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers;
import uk.gov.justice.laa.portal.landingpage.entity.DeleteUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.forms.UserTypeForm;
import uk.gov.justice.laa.portal.landingpage.model.DeletedUser;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserReasonRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.AuditExportService;
import uk.gov.justice.laa.portal.landingpage.service.AuditExportService.AuditCsvExport;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesUser;
import uk.gov.justice.laa.portal.landingpage.viewmodel.DeleteUserReasonViewModel;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AuditController {

    private final UserService userService;
    private final LoginService loginService;
    private final EventService eventService;
    private final AccessControlService accessControlService;
    private final AuditExportService auditExportService;
    private final FirmRepository firmRepository;
    private final FirmService firmService;
    private final AuthenticatedUser authenticatedUser;
    private final TechServicesClient techServicesClient;
    private final DisableUserReasonRepository disableUserReasonRepository;

    @Value("${feature.flag.disable.user}")
    private boolean disableUserFeatureEnabled;

    /**
     * Display the User Access Audit Table
     * Shows all registered users including those without firm profiles
     */
    @GetMapping("/users/audit")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_AUDIT_TABLE)")
    public String displayAuditTable(
            @ModelAttribute AuditTableSearchCriteria criteria,
            Model model, Authentication authentication) {

        log.debug("AuditController.displayAuditTable - {}", criteria);

        // Apply user type filtering based on authenticated user's permissions (same logic as UserController)
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        boolean canSeeExternalUsers = accessControlService.authenticatedUserHasPermission(Permission.VIEW_EXTERNAL_USER);
        boolean canSeeInternalUsers = accessControlService.authenticatedUserHasPermission(Permission.VIEW_INTERNAL_USER);
        boolean canSeeAllUsers = canSeeExternalUsers && canSeeInternalUsers;

        UserTypeForm filteredUserType = criteria.getSelectedUserType();
        UUID filteredFirmId = criteria.getSelectedFirmId();
        String selectedUserType = criteria.getSelectedUserType() != null ? criteria.getSelectedUserType().name() : "";

        if (!canSeeAllUsers) {
            if (canSeeInternalUsers) {
                filteredUserType = UserTypeForm.INTERNAL;
                selectedUserType = selectedUserType.equalsIgnoreCase(UserTypeForm.INTERNAL.name()) ? UserTypeForm.INTERNAL.name() : UserTypeForm.ALL.name();
            } else {
                if (filteredUserType == null || filteredUserType == UserTypeForm.ALL || filteredUserType == UserTypeForm.INTERNAL) {
                    filteredUserType = UserTypeForm.ALL_EXTERNAL;
                    selectedUserType = UserTypeForm.ALL.name();
                }
                Optional<FirmDto> optionalFirm = firmService.getUserFirm(entraUser);
                if (optionalFirm.isPresent()) {
                    filteredFirmId = optionalFirm.get().getId();
                }
            }
        }

        // Get audit users with security-filtered user type and firm restriction
        PaginatedAuditUsers paginatedUsers = userService.getAuditUsers(
                criteria.getSearch(), filteredFirmId,
                criteria.getSilasRole(), criteria.getSelectedAppId(), filteredUserType,
                criteria.getPage(), criteria.getSize(), criteria.getSort(), criteria.getDirection(), false,
                criteria.getNeverActivated());
        // Build firm search form using the effective (access-control-applied) firm ID so that the
        // export button correctly reflects the auto-applied firm for external single-firm users.
        FirmSearchForm firmSearchForm = new FirmSearchForm(criteria.getFirmSearch(), filteredFirmId);
        // Add attributes to model
        buildDisplayAuditTableModel(criteria, model, paginatedUsers, firmSearchForm);
        model.addAttribute("canSeeExternalUsers", canSeeExternalUsers);
        model.addAttribute("canSeeInternalUsers", canSeeInternalUsers);
        model.addAttribute("selectedUserType", selectedUserType);

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
        model.addAttribute("selectedFirmName", criteria.getSelectedFirmName());
        model.addAttribute("inactiveSinceDate", criteria.getInactiveSinceDate());
        model.addAttribute("neverActivated", criteria.getNeverActivated() != null ? criteria.getNeverActivated() : false);
        model.addAttribute("sort", criteria.getSort());
        model.addAttribute("direction", criteria.getDirection());
        model.addAttribute("exportCsv",
                accessControlService.authenticatedUserHasPermission(Permission.EXPORT_AUDIT_DATA));
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
        boolean canDisableUser;
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

        TechServicesApiResponse<GetUserResponse> entraUserResponse = techServicesClient.getUser(userDetail.getEntraOid());
        if (entraUserResponse.isSuccess()) {
            TechServicesUser user = entraUserResponse.getData().getUser();
            String disableUserReason = formatDisableUserReason(user);

            OffsetDateTime lastLoginTime = user.getLastSignIn() != null
                    ? OffsetDateTime.parse(user.getLastSignIn())
                    : null;

            model.addAttribute("lastLogin", lastLoginTime);
            model.addAttribute("entraUser", entraUserResponse.getData().getUser());
            model.addAttribute("entraVerificationStatus", getEntraVerificationStatus(entraUserResponse));
            model.addAttribute("entraUserDisableReason", disableUserReason);
        }
        canDisableUser = accessControlService.canDisableUser(userDetail.getUserId());
        boolean showResendVerificationLink = accessControlService.canResendActivationForAuditUser(userDetail.getUserId());
        model.addAttribute("showResendVerificationLink", showResendVerificationLink);
        AccessControlService.EnablementFlags enablementFlags = disableUserFeatureEnabled
                ? accessControlService.getEnablementFlags(userDetail.getUserId())
                : new AccessControlService.EnablementFlags(false, false);
        boolean canEnableUser = enablementFlags.canEnable();
        boolean cannotEnableUser = enablementFlags.blockedByHierarchy();

        // Add attributes to model
        model.addAttribute("user", userDetail);
        model.addAttribute("silasStatus", userService.determineStatusBadgeForAuditUser(userDetail).name());
        model.addAttribute("profileId", userId); // Add profile ID for pagination links
        model.addAttribute("profilePage", profilePage);
        model.addAttribute("profileSize", profileSize);
        model.addAttribute("canDisableUser", disableUserFeatureEnabled && canDisableUser);
        model.addAttribute("canEnableUser", canEnableUser);
        model.addAttribute("cannotEnableUser", cannotEnableUser);
        model.addAttribute("userIsEnabled", userDetail.isEnabled());

        return "user-audit/details";
    }

    private String getEntraVerificationStatus(TechServicesApiResponse<GetUserResponse> entraUserResponse) {
        if (entraUserResponse.getData().getUser() != null
                && entraUserResponse.getData().getUser().getCustomSecurityAttributes() != null
                && entraUserResponse.getData().getUser().getCustomSecurityAttributes().getGuestUserStatus() != null
                && entraUserResponse.getData().getUser().getCustomSecurityAttributes().getGuestUserStatus().getInvitationProgress() != null) {
            return entraUserResponse.getData().getUser().getCustomSecurityAttributes().getGuestUserStatus().getInvitationProgress().name();
        }
        return "";
    }

    /**
     * Display complete detailed audit information for a specific user
     */
    @GetMapping("/users/audit/{id}/full")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_AUDIT_TABLE)")
    public String displayFullUserAuditDetail(@PathVariable("id") UUID userId,
                                         @RequestParam(name = "profilePage", defaultValue = "1") int profilePage,
                                         @RequestParam(name = "profileSize", defaultValue = "3") int profileSize,
                                         @RequestParam(name = "isEntraId", defaultValue = "false") boolean isEntraId,
                                         Model model) {

        log.debug(
                "AuditController.displayUserAuditDetail - userId: '{}', isEntraId: {}, profilePage: {}, profileSize: {}",
                userId, isEntraId, profilePage, profileSize);

        AuditUserDetailDto userDetail;
        boolean canDisableUser;

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
        TechServicesApiResponse<GetUserResponse> entraUserResponse = techServicesClient.getUser(userDetail.getEntraOid());
        if (entraUserResponse.isSuccess()) {
            TechServicesUser user = entraUserResponse.getData().getUser();
            String disableUserReason = formatDisableUserReason(user);
            model.addAttribute("entraUser", entraUserResponse.getData().getUser());
            model.addAttribute("entraUserDisableReason", disableUserReason);
        }
        canDisableUser = accessControlService.canDisableUser(userDetail.getUserId());

        // Add attributes to model
        model.addAttribute("user", userDetail);
        model.addAttribute("silasStatus", userService.determineStatusBadgeForAuditUser(userDetail));
        model.addAttribute("profileId", userId); // Add profile ID for pagination links
        model.addAttribute("profilePage", profilePage);
        model.addAttribute("profileSize", profileSize);
        model.addAttribute("canDisableUser", disableUserFeatureEnabled && canDisableUser);
        model.addAttribute("userIsEnabled", userDetail.isEnabled());

        return "user-audit/full-details";
    }

    private String formatDisableUserReason(TechServicesUser user) {
        return Optional.ofNullable(user)
                .map(TechServicesUser::getCustomSecurityAttributes)
                .map(TechServicesUser.CustomSecurityAttributes::getGuestUserStatus)
                .map(TechServicesUser.GuestUserStatus::getDisabledReason)
                .flatMap(disableUserReasonRepository::findDisableUserReasonByEntraDescription)
                .map(DisableUserReason::getName)
                .orElse("Unknown");
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
        populateDeleteReasonsModel(model);
        return "user-audit/delete-user-without-profile-reason";
    }

    /**
     * Delete a user without a profile Notifies Entra and removes from database
     */
    @PostMapping("/users/audit/entra/{id}/delete")
    @PreAuthorize("@accessControlService.canDeleteUserWithoutProfile(#id)")
    public String deleteUserWithoutProfile(@PathVariable String id,
            @RequestParam("reasonId") String reasonId, Authentication authentication,
            HttpSession session, Model model) {

        log.debug("AuditController.deleteUserWithoutProfile - entraUserId: '{}', reasonId: '{}'", id,
                reasonId);

        AuditUserDetailDto userDetail = userService.getAuditUserDetailByEntraId(UUID.fromString(id));

        UUID deleteReasonId = null;
        if (reasonId == null || reasonId.isBlank()) {
            model.addAttribute("user", userDetail);
            model.addAttribute("fieldErrorMessage", "Please select a reason.");
            model.addAttribute(ModelAttributes.PAGE_TITLE,
                    "Remove access - " + userDetail.getFullName());
            populateDeleteReasonsModel(model);
            return "user-audit/delete-user-without-profile-reason";
        }
        try {
            deleteReasonId = UUID.fromString(reasonId);
        } catch (IllegalArgumentException e) {
            model.addAttribute("user", userDetail);
            model.addAttribute("fieldErrorMessage", "Please select a valid reason.");
            model.addAttribute(ModelAttributes.PAGE_TITLE,
                    "Remove access - " + userDetail.getFullName());
            populateDeleteReasonsModel(model);
            return "user-audit/delete-user-without-profile-reason";
        }

        final UUID resolvedReasonId = deleteReasonId;
        Optional<DeleteUserReason> matchedReason = userService.getDeleteUserReasons(true).stream()
                .filter(r -> r.getId().equals(resolvedReasonId))
                .findFirst();
        if (matchedReason.isEmpty()) {
            model.addAttribute("user", userDetail);
            model.addAttribute("fieldErrorMessage", "Please select a valid reason.");
            model.addAttribute(ModelAttributes.PAGE_TITLE,
                    "Remove access - " + userDetail.getFullName());
            populateDeleteReasonsModel(model);
            return "user-audit/delete-user-without-profile-reason";
        }

        EntraUser current = loginService.getCurrentEntraUser(authentication);
        UUID currentEntraOidUuid = UUID.fromString(current.getEntraOid());
        String deleteReasonLabel = matchedReason.get().getLabel();
        try {
            DeletedUser deletedUser = userService.deleteEntraUserWithoutProfile(id, deleteReasonId, current.getId());
            DeleteUserSuccessAuditEvent deleteUserAuditEvent = new DeleteUserSuccessAuditEvent(
                    deletedUser.getDeleteReasonLabel(), currentEntraOidUuid, deletedUser);
            eventService.logEvent(deleteUserAuditEvent);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("user", userDetail);
            model.addAttribute("fieldErrorMessage", "Please select a valid reason.");
            model.addAttribute(ModelAttributes.PAGE_TITLE,
                    "Remove access - " + userDetail.getFullName());
            populateDeleteReasonsModel(model);
            return "user-audit/delete-user-without-profile-reason";
        } catch (RuntimeException ex) {
            log.error("Failed to delete user without profile {}: {}", id, ex.getMessage(), ex);
            DeleteUserAttemptAuditEvent deleteUserAttemptAuditEvent = new DeleteUserAttemptAuditEvent(id, deleteReasonLabel,
                    currentEntraOidUuid,
                    ex.getMessage());
            eventService.logEvent(deleteUserAttemptAuditEvent);
            model.addAttribute("user", userDetail);
            model.addAttribute("globalErrorMessage", "User delete failed, please try again later");
            model.addAttribute(ModelAttributes.PAGE_TITLE,
                    "Remove access - " + userDetail.getFullName());
            populateDeleteReasonsModel(model);
            return "user-audit/delete-user-without-profile-reason";
        }

        model.addAttribute("deletedUserFullName", userDetail.getFullName());
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User deleted");

        return "user-audit/delete-user-success";
    }

    private void populateDeleteReasonsModel(Model model) {
        List<DeleteUserReasonViewModel> deleteReasons = userService.getDeleteUserReasons(true)
                .stream()
                .map(r -> {
                    DeleteUserReasonViewModel vm = new DeleteUserReasonViewModel();
                    vm.setId(r.getId());
                    vm.setCode(r.getCode());
                    vm.setLabel(r.getLabel());
                    return vm;
                })
                .toList();
        model.addAttribute("deleteReasons", deleteReasons);
    }

    @GetMapping(value = "/users/audit/download", produces = "text/csv")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EXPORT_AUDIT_DATA)")
    public ResponseEntity<byte[]> downloadAuditCsv(@ModelAttribute AuditTableSearchCriteria criteria,
            Authentication authentication) {

        // Apply the same access-control firm/type restrictions as displayAuditTable so that
        // external single-firm users (whose firm is auto-applied server-side) can export CSV.
        boolean canSeeExternalUsers = accessControlService.authenticatedUserHasPermission(Permission.VIEW_EXTERNAL_USER);
        boolean canSeeInternalUsers = accessControlService.authenticatedUserHasPermission(Permission.VIEW_INTERNAL_USER);
        boolean canSeeAllUsers = canSeeExternalUsers && canSeeInternalUsers;

        UUID effectiveFirmId = criteria.getSelectedFirmId();
        UserTypeForm effectiveUserType = criteria.getSelectedUserType();

        if (!canSeeAllUsers) {
            if (canSeeInternalUsers) {
                effectiveUserType = UserTypeForm.INTERNAL;
            } else {
                if (effectiveUserType == null || effectiveUserType == UserTypeForm.ALL || effectiveUserType == UserTypeForm.INTERNAL) {
                    effectiveUserType = UserTypeForm.ALL_EXTERNAL;
                }
                EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
                Optional<FirmDto> optionalFirm = firmService.getUserFirm(entraUser);
                if (optionalFirm.isPresent()) {
                    effectiveFirmId = optionalFirm.get().getId();
                }
            }
        }

        if (effectiveFirmId == null && effectiveUserType != UserTypeForm.INTERNAL) {
            log.warn("Invalid criteria provided for CSV export - firm ID must always be provided when external user type is selected. effectiveUserType: {}",
                    effectiveUserType);
            throw new RuntimeException("Invalid Search criteria provided");
        }

        if (effectiveFirmId != null && effectiveUserType == UserTypeForm.INTERNAL) {
            log.warn("Invalid criteria provided for CSV export - firm ID should not be provided when internal user type is selected. effectiveFirmId: {}",
                    effectiveFirmId);
            throw new RuntimeException("Invalid Search criteria provided");
        }

        final int pageSize = 500;
        int page = 1;

        List<String> filterSummary = Stream.of(
                        criteria.getSilasRole(),
                        effectiveUserType == null ? "" : String.valueOf(effectiveUserType),
                        criteria.getSelectedAppId() == null ? "" : String.valueOf(criteria.getSelectedAppId())
                )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String firmCode = "";
        String firmName = criteria.getSelectedFirmName();
        if (effectiveFirmId != null) {
            Optional<Firm> firm = firmRepository.findById(effectiveFirmId);
            firmCode = firm.map(Firm::getCode).orElse("");
            if (firmName == null || firmName.isBlank()) {
                firmName = firm.map(Firm::getName).orElse("");
            }
        }
        List<AuditUserDto> firmData = new ArrayList<>(pageSize);

        PaginatedAuditUsers result;
        do {
            result = userService.getAuditUsers(
                    criteria.getSearch(),
                    effectiveFirmId,
                    criteria.getSilasRole(),
                    criteria.getSelectedAppId(),
                    effectiveUserType,
                    page,
                    pageSize,
                    criteria.getSort(),
                    criteria.getDirection(),
                    true,
                    criteria.getNeverActivated()
            );

            firmData.addAll(result.getUsers());
            page++;

        } while (!isLastPage(result, pageSize));

        if (result.getUsers().isEmpty()) {
            log.info("No audit users found for search criteria: {}", Arrays.toString(filterSummary.toArray()));
        }

        AuditCsvExport export = auditExportService.downloadAuditCsv(firmData, firmCode, firmName);
        String userId = authenticatedUser.getCurrentUser()
                .map(CurrentUserDto::getUserId)
                .map(Object::toString)
                .orElse("unknown");
        log.info("CSV Audit Export complete - actor= {}, timestamp= {}, Firm Code= {}, Filter Summary (Silas Role, "
                + "UserType, App Id)= {}, "
                + "row count= {}", userId, LocalDateTime.now(), firmCode, filterSummary, result.getUsers().size());


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(export.filename()).build());

        return ResponseEntity.ok().headers(headers).body(export.bytes());
    }

    private boolean isLastPage(PaginatedAuditUsers page, int size) {
        return page.getUsers().size() < size;
    }

    /**
     * Display the Deleted Users page
     * Shows all deleted users with search and pagination
     */
    @GetMapping("/users/audit/deleted")
    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_AUDIT_TABLE)")
    public String displayDeletedUsers(
            @RequestParam(name = "search", required = false) String searchTerm,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "statusChangedDate") String sort,
            @RequestParam(name = "direction", defaultValue = "desc") String direction,
            Model model) {

        log.debug("AuditController.displayDeletedUsers - search: '{}', page: {}, size: {}, sort: {}, direction: {}",
                searchTerm, page, size, sort, direction);

        uk.gov.justice.laa.portal.landingpage.dto.PaginatedDeletedUsers paginatedDeletedUsers =
            userService.getDeletedUsers(searchTerm, page, size, sort, direction);

        model.addAttribute("deletedUsers", paginatedDeletedUsers.getDeletedUsers());
        model.addAttribute("search", searchTerm);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        model.addAttribute("totalDeletedUsers", paginatedDeletedUsers.getTotalDeletedUsers());
        model.addAttribute("totalPages", paginatedDeletedUsers.getTotalPages());
        model.addAttribute("currentPage", page);

        return "user-audit/deleted-users";
    }
}
