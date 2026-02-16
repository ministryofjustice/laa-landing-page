package uk.gov.justice.laa.portal.landingpage.controller;

import static uk.gov.justice.laa.portal.landingpage.service.FirmComparatorByRelevance.relevance;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getListFromHttpSession;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getObjectFromHttpSession;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.getSetFromHttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.ConvertToMultiFirmAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CreateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteUserAttemptAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.DeleteUserSuccessAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.UpdateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.CreateUserDetailsIncompleteException;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.forms.ApplicationsForm;
import uk.gov.justice.laa.portal.landingpage.forms.ConvertToMultiFirmForm;
import uk.gov.justice.laa.portal.landingpage.forms.DisableUserReasonForm;
import uk.gov.justice.laa.portal.landingpage.forms.EditUserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.forms.FirmReassignmentForm;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmForm;
import uk.gov.justice.laa.portal.landingpage.forms.OfficesForm;
import uk.gov.justice.laa.portal.landingpage.forms.ReassignmentReasonForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.model.DeletedUser;
import uk.gov.justice.laa.portal.landingpage.model.OfficeModel;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.model.UserRole;

import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.utils.CcmsRoleGroupsUtil;
import uk.gov.justice.laa.portal.landingpage.utils.RolesUtils;
import uk.gov.justice.laa.portal.landingpage.utils.UserUtils;
import uk.gov.justice.laa.portal.landingpage.viewmodel.AppRoleViewModel;
import uk.gov.justice.laa.portal.landingpage.viewmodel.DisableUserReasonViewModel;

/**
 * User Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class UserController {

    public static final String NO_OFFICES = "NO_OFFICES";
    public static final String ALL = "ALL";
    private final LoginService loginService;
    private final UserService userService;
    private final OfficeService officeService;
    private final EventService eventService;
    private final FirmService firmService;
    private final ModelMapper mapper;
    private final AccessControlService accessControlService;
    private final RoleAssignmentService roleAssignmentService;
    private final EmailValidationService emailValidationService;
    private final AppRoleService appRoleService;
    private final UserAccountStatusService disableUserService;

    @Value("${feature.flag.disable.user}")
    public boolean disableUserFeatureEnabled;

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
            @RequestParam(name = "showMultiFirmUsers", required = false) boolean showMultiFirmUsers,
            FirmSearchForm firmSearchForm,
            Model model, HttpSession session, Authentication authentication) {

        // Process request parameters and handle session filters
        search = search == null ? "" : search.trim();
        Map<String, Object> processedFilters = processRequestFilters(size, page, sort, direction, usertype, search,
                showFirmAdmins, showMultiFirmUsers, backButton, session, firmSearchForm);
        size = (Integer) processedFilters.get("size");
        page = (Integer) processedFilters.get("page");
        sort = (String) processedFilters.get("sort");
        direction = (String) processedFilters.get("direction");
        usertype = (String) processedFilters.get("usertype");
        search = (String) processedFilters.get("search");
        firmSearchForm = (FirmSearchForm) processedFilters.get("firmSearchForm");
        showFirmAdmins = Boolean.parseBoolean(String.valueOf(processedFilters.get("showFirmAdmins")));
        showMultiFirmUsers = Boolean.parseBoolean(String.valueOf(processedFilters.get("showMultiFirmUsers")));

        PaginatedUsers paginatedUsers;
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        boolean internal = userService.isInternal(entraUser.getId());
        boolean canSeeAllUsers = accessControlService.authenticatedUserHasPermission(Permission.VIEW_INTERNAL_USER)
                && accessControlService.authenticatedUserHasPermission(Permission.VIEW_EXTERNAL_USER);

        // Debug logging
        log.debug(
                "UserController.displayAllUsers - search: '{}', firmSearch: '{}', showFirmAdmins: {}, showMultiFirmUsers: {}",
                search, firmSearchForm, showFirmAdmins, showMultiFirmUsers);

        if (canSeeAllUsers) {
            UserSearchCriteria searchCriteria = new UserSearchCriteria(search, firmSearchForm, null, showFirmAdmins,
                    showMultiFirmUsers);
            paginatedUsers = userService.getPageOfUsersBySearch(searchCriteria, page, size, sort, direction);
        } else if (accessControlService.authenticatedUserHasPermission(Permission.VIEW_INTERNAL_USER)) {
            UserSearchCriteria searchCriteria = new UserSearchCriteria(search, firmSearchForm, UserType.INTERNAL,
                    showFirmAdmins, showMultiFirmUsers);
            paginatedUsers = userService.getPageOfUsersBySearch(searchCriteria, page, size, sort, direction);
        } else if (accessControlService.authenticatedUserHasPermission(Permission.VIEW_EXTERNAL_USER) && internal) {
            UserSearchCriteria searchCriteria = new UserSearchCriteria(search, firmSearchForm, UserType.EXTERNAL,
                    showFirmAdmins, showMultiFirmUsers);
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
                FirmSearchForm searchForm = Optional.ofNullable(firmSearchForm)
                        .orElse(FirmSearchForm.builder().build());
                searchForm.setSelectedFirmId(optionalFirm.get().getId());
                UserSearchCriteria searchCriteria = new UserSearchCriteria(search, searchForm, UserType.EXTERNAL,
                        showFirmAdmins, showMultiFirmUsers);
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

        boolean allowDelegateUserAccess = accessControlService
                .authenticatedUserHasAnyGivenPermissions(Permission.DELEGATE_EXTERNAL_USER_ACCESS,
                        Permission.DELEGATE_EXTERNAL_USER_ACCESS_INTERNAL);

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
        model.addAttribute("allowDelegateUserAccess", allowDelegateUserAccess);
        model.addAttribute("showMultiFirmUsers", showMultiFirmUsers);
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
        boolean showFirmAdmins = Boolean.parseBoolean(String.valueOf(filters.get("showFirmAdmins")));
        boolean showMultiFirmUsers = Boolean.parseBoolean(String.valueOf(filters.get("showMultiFirmUsers")));
        Integer size = (Integer) filters.get("size");
        Integer page = (Integer) filters.get("page");
        FirmSearchForm firmSearchForm = (FirmSearchForm) filters.get("firmSearchForm");

        return (search != null && !search.isEmpty())
                || (usertype != null && !usertype.isEmpty())
                || (sort != null && !sort.isEmpty())
                || (direction != null && !direction.isEmpty())
                || showFirmAdmins
                || showMultiFirmUsers
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
     * Manage user via graph SDK
     */
    @GetMapping("/users/manage/{id}")
    @PreAuthorize("@accessControlService.canAccessUser(#id)")
    public String manageUser(@PathVariable String id,
            @RequestParam(value = "resendVerification", required = false) boolean resendVerification,
            Model model, HttpSession session, Authentication authentication) {

        // Handle verification email resend if requested
        if (Boolean.TRUE.equals(resendVerification)) {
            handleResendVerification(id, model);
        }

        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();

        List<AppRoleDto> userAppRoles = user.getAppRoles() != null
                ? user.getAppRoles().stream()
                        .filter(appRoleDto -> appRoleDto.getApp().isEnabled())
                        .map(appRoleDto -> mapper.map(appRoleDto, AppRoleDto.class))
                        .sorted()
                        .collect(Collectors.toList())
                : Collections.emptyList();
        List<OfficeDto> userOffices = user.getOffices() != null ? user.getOffices() : Collections.emptyList();
        final boolean isAccessGranted = userService.isAccessGranted(user.getId().toString());
        final boolean canEditUser = accessControlService.canEditUser(user.getId().toString());
        model.addAttribute("user", user);
        model.addAttribute("userAppRoles", userAppRoles);
        model.addAttribute("userOffices", userOffices);
        model.addAttribute("isAccessGranted", isAccessGranted);
        boolean externalUser = UserType.EXTERNAL == user.getUserType();
        model.addAttribute("externalUser", externalUser);

        UserProfile editorUserProfile = loginService.getCurrentProfile(authentication);
        boolean editorInternalUser = UserType.INTERNAL == editorUserProfile.getUserType();
        boolean canViewAllProfiles = externalUser && editorInternalUser
                && accessControlService.authenticatedUserHasPermission(Permission.VIEW_EXTERNAL_USER);

        model.addAttribute("canViewAllProfiles", canViewAllProfiles);
        model.addAttribute("editorInternalUser", editorInternalUser);

        boolean hasViewOfficePermission = accessControlService
                .authenticatedUserHasPermission(Permission.VIEW_USER_OFFICE);

        boolean hasEditOfficePermission = accessControlService
                .authenticatedUserHasPermission(Permission.EDIT_USER_OFFICE);

        boolean canManageOffices = hasEditOfficePermission && canEditUser;

        boolean showOfficesTab = hasViewOfficePermission || canManageOffices;

        boolean isInternalUser = userService.isInternal(user.getEntraUser().getId());

        model.addAttribute("showOfficesTab", showOfficesTab);
        model.addAttribute("canManageOffices", canManageOffices);
        model.addAttribute("isInternalUser", isInternalUser);

        model.addAttribute("canEditUser", canEditUser);
        
        // Check if current user can reassign firms (External User Admin permission + target is external user)
        boolean canReassignFirm = externalUser
                && accessControlService.authenticatedUserHasPermission(Permission.EDIT_USER_FIRM);
        model.addAttribute("canReassignFirm", canReassignFirm);
        
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Manage user - " + user.getFullName());
        final boolean canDeleteUser = accessControlService.canDeleteUser(id);
        model.addAttribute("canDeleteUser", canDeleteUser);
        final boolean canDisableUser = disableUserFeatureEnabled && accessControlService.canDisableUser(user.getEntraUser().getId());
        model.addAttribute("canDisableUser", canDisableUser);
        boolean showResendVerificationLink = accessControlService.canSendVerificationEmail(id);
        model.addAttribute("showResendVerificationLink", showResendVerificationLink);


        // Multi-firm user information
        boolean isMultiFirmUser = user.getEntraUser() != null && user.getEntraUser().isMultiFirmUser();
        model.addAttribute("isMultiFirmUser", isMultiFirmUser);
        model.addAttribute("canViewAllFirmsOfMultiFirmUser", accessControlService.canViewAllFirmsOfMultiFirmUser());

        // Check if this profile belongs to the logged-in user
        boolean isOwnProfile = editorUserProfile.getId().equals(user.getId());

        if (isMultiFirmUser) {
            // Check if user can delete the currently viewed profile (not all profiles)
            // but not their own profile
            boolean canDeleteFirmProfile = !isOwnProfile
                    && accessControlService.canDeleteFirmProfile(user.getId().toString());
            model.addAttribute("canDeleteFirmProfile", canDeleteFirmProfile);

            // Get profile count for multi-firm users (using count query instead of fetching
            // all profiles)
            long profileCount = userService.getProfileCountByEntraUserId(UUID.fromString(user.getEntraUser().getId()));
            model.addAttribute("profileCount", profileCount);
        } else {
            // For non-multi-firm users, still add canDeleteFirmProfile if they have the
            // permission but not their own profile
            boolean canDeleteFirmProfile = !isOwnProfile
                    && accessControlService.canDeleteFirmProfile(user.getId().toString());
            model.addAttribute("canDeleteFirmProfile", canDeleteFirmProfile);

            // Non-multi-firm users have exactly 1 profile
            model.addAttribute("profileCount", 1);
        }

        // Add filter state to model for "Back to search results" link
        @SuppressWarnings("unchecked")
        Map<String, Object> filters = (Map<String, Object>) session.getAttribute("userListFilters");
        boolean hasFilters = hasActiveFilters(filters);
        model.addAttribute("hasFilters", hasFilters);

        return "manage-user";
    }

    @PostMapping("/users/manage/{id}/delete")
    @PreAuthorize("@accessControlService.canDeleteUser(#id)")
    public String deleteExternalUser(@PathVariable String id,
            @RequestParam("reason") String reason,
            Authentication authentication,
            HttpSession session,
            Model model) {
        Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found.");
        }

        if (reason == null || reason.trim().length() < 10) {
            model.addAttribute("user", optionalUser.get());
            model.addAttribute("fieldErrorMessage", "Please enter a reason (minimum 10 characters).");
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Remove access - " + optionalUser.get().getFullName());
            return "delete-user-reason";
        }

        EntraUser current = loginService.getCurrentEntraUser(authentication);
        try {
            DeletedUser deletedUser = userService.deleteExternalUser(id, reason.trim(), current.getId());
            DeleteUserSuccessAuditEvent deleteUserAuditEvent = new DeleteUserSuccessAuditEvent(
                    reason.trim(), current.getId(), deletedUser);
            eventService.logEvent(deleteUserAuditEvent);
        } catch (RuntimeException ex) {
            log.error("Failed to delete external user {}: {}", id, ex.getMessage(), ex);
            DeleteUserAttemptAuditEvent deleteUserAttemptAuditEvent = new DeleteUserAttemptAuditEvent(
                    optionalUser.get().getEntraUser().getId(),
                    reason.trim(), current.getId(), ex.getMessage());
            eventService.logEvent(deleteUserAttemptAuditEvent);
            model.addAttribute("user", optionalUser.get());
            model.addAttribute("globalErrorMessage", "User delete failed, please try again later");
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Remove access - " + optionalUser.get().getFullName());
            return "delete-user-reason";
        }

        model.addAttribute("deletedUserFullName", optionalUser.get().getFullName());
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User deleted");
        return "delete-user-success";
    }

    @GetMapping("/users/manage/{id}/delete")
    @PreAuthorize("@accessControlService.canDeleteUser(#id)")
    public String deleteExternalUserConfirm(@PathVariable String id, Model model) {
        Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found.");
        }
        model.addAttribute("user", optionalUser.get());
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Remove access - " + optionalUser.get().getFullName());
        return "delete-user-reason";
    }

    @GetMapping("/users/manage/{id}/disable")
    @PreAuthorize("@accessControlService.canDisableUser(#id)")
    public String disableUserReasonsGet(@PathVariable String id,
                                     DisableUserReasonForm disableUserReasonForm,
                                     Model model,
                                     HttpSession session) {
        if (!disableUserFeatureEnabled) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        List<DisableUserReasonViewModel> reasons = disableUserService.getDisableUserReasons().stream()
                .map(reason -> mapper.map(reason, DisableUserReasonViewModel.class))
                .toList();
        model.addAttribute("user", user);
        model.addAttribute("reasons", reasons);
        model.addAttribute("disableUserReasonsForm", disableUserReasonForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Disable User - " + user.getFullName());
        session.setAttribute("disableUserReasonModel", model);
        return "disable-user-reason";
    }

    @PostMapping("/users/manage/{id}/disable")
    @PreAuthorize("@accessControlService.canDisableUser(#id)")
    public String disableUserReasonsPost(@PathVariable String id,
                                     @Valid DisableUserReasonForm disableUserReasonForm,
                                     BindingResult result,
                                     Authentication authentication,
                                     Model model,
                                     HttpSession session) {
        if (!disableUserFeatureEnabled) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404));
        }
        if (result.hasErrors()) {
            String errorMessage = buildErrorString(result);
            Model modelFromSession = getObjectFromHttpSession(session, "disableUserReasonModel", Model.class).orElseThrow();
            EntraUserDto user = (EntraUserDto) modelFromSession.getAttribute("user");
            model.addAttribute("reasons", modelFromSession.getAttribute("reasons"));
            model.addAttribute("user", user);
            model.addAttribute("disableUserReasonsForm", disableUserReasonForm);
            model.addAttribute("errorMessage", errorMessage);
            return "disable-user-reason";
        }
        EntraUserDto user = userService.getEntraUserById(id).orElseThrow();
        UUID disabledUserId = UUID.fromString(user.getId());
        UUID disabledByUserId = loginService.getCurrentEntraUser(authentication).getId();
        UUID disabledReasonId = UUID.fromString(disableUserReasonForm.getReasonId());
        disableUserService.disableUser(disabledUserId, disabledReasonId, disabledByUserId);

        model.addAttribute("user", user);
        model.addAttribute("disableUserReasonsForm", disableUserReasonForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Disable User Success - " + user.getFullName());
        return "disable-user-completed";
    }

    @NotNull
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

    @GetMapping("/user/create/details")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String createUser(UserDetailsForm userDetailsForm, HttpSession session, Model model) {
        EntraUserDto user = (EntraUserDto) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = new EntraUserDto();
        }
        UserType selectedUserType = (UserType) session.getAttribute("selectedUserType");
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
        if (Objects.nonNull(userDetailsForm.getEmail()) && !userDetailsForm.getEmail().isEmpty()) {
            if (userService.userExistsByEmail(userDetailsForm.getEmail())) {
                // Check if the existing user is a multi-firm user
                if (userService.isMultiFirmUserByEmail(userDetailsForm.getEmail())) {
                    result.rejectValue("email", "error.email",
                            "This email address is already registered as a multi-firm user");
                } else {
                    result.rejectValue("email", "error.email", "Email address already exists");
                }
            }

            if (!emailValidationService.isValidEmailDomain(userDetailsForm.getEmail())) {
                result.rejectValue("email", "email.invalidDomain",
                        "The email address domain is not valid or cannot receive emails.");
            }
        }

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while creating user: {}", result.getAllErrors());

            Model modelFromSession = (Model) session.getAttribute("createUserDetailsModel");
            if (modelFromSession == null) {
                return "redirect:/admin/user/create/details";
            }

            model.addAttribute("userTypes", modelFromSession.getAttribute("userTypes"));
            model.addAttribute("user", modelFromSession.getAttribute("user"));
            return "add-user-details";
        }

        // Set user details from the form
        user.setFirstName(userDetailsForm.getFirstName().trim());
        user.setLastName(userDetailsForm.getLastName().trim());
        user.setFullName(userDetailsForm.getFirstName().trim() + " " + userDetailsForm.getLastName().trim());
        user.setEmail(userDetailsForm.getEmail().trim());
        session.setAttribute("user", user);
        session.setAttribute("isUserManager", userDetailsForm.getUserManager());

        // Clear the createUserDetailsModel from session to avoid stale data
        session.removeAttribute("createUserDetailsModel");

        // Check feature flag to determine next step
        return "redirect:/admin/user/create/multi-firm";
    }

    @GetMapping("/user/create/multi-firm")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String createUserMultiFirm(MultiFirmForm multiFirmForm, HttpSession session, Model model) {
        // Check if multi-firm feature is enabled
        EntraUserDto user = (EntraUserDto) session.getAttribute("user");
        if (Objects.isNull(user)) {
            return "redirect:/admin/user/create/details";
        }

        // Pre-populate form with value from session if it exists
        Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");
        if (isMultiFirmUser != null) {
            multiFirmForm.setMultiFirmUser(isMultiFirmUser);
        }

        model.addAttribute("multiFirmForm", multiFirmForm);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Allow multi-firm access");
        return "add-user-multi-firm";
    }

    @PostMapping("/user/create/multi-firm")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String postUserMultiFirm(@Valid MultiFirmForm multiFirmForm, BindingResult result,
            HttpSession session, Model model) {

        if (multiFirmForm.getMultiFirmUser() == null) {
            result.rejectValue("multiFirmUser", "error.multiFirmUser",
                    "You must select whether this user requires access to multiple firms");
        }

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while setting multi-firm access: {}", result.getAllErrors());
            model.addAttribute("multiFirmForm", multiFirmForm);
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Allow multi-firm access");
            return "add-user-multi-firm";
        }

        // Update user in session with multi-firm flag
        EntraUserDto user = (EntraUserDto) session.getAttribute("user");
        if (Objects.isNull(user)) {
            return "redirect:/admin/user/create/details";
        }

        session.setAttribute("isMultiFirmUser", multiFirmForm.getMultiFirmUser());

        // If user is multi-firm, skip the firm selection page and go directly to
        // check-answers
        if (Boolean.TRUE.equals(multiFirmForm.getMultiFirmUser())) {
            // Clear any previously selected firm from session
            session.removeAttribute("firm");
            session.removeAttribute("firmSearchForm");
            return "redirect:/admin/user/create/check-answers";
        }

        return "redirect:/admin/user/create/firm";
    }

    @GetMapping("/user/create/firm")
    public String createUserFirm(FirmSearchForm firmSearchForm, HttpSession session, Model model,
            @RequestParam(value = "firmSearchResultCount", defaultValue = "10") Integer count) {

        Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");

        // If firmSearchForm is already populated from session (e.g., validation
        // errors), keep it
        FirmSearchForm existingForm = (FirmSearchForm) session.getAttribute("firmSearchForm");
        if (existingForm != null) {
            firmSearchForm = existingForm;
        } else if (session.getAttribute("firm") != null) {
            // Grab firm search details from session firm if coming here from the
            // confirmation screen.
            FirmDto firm = (FirmDto) session.getAttribute("firm");
            firmSearchForm = FirmSearchForm.builder()
                    .selectedFirmId(firm.getId())
                    .firmSearch(firm.getName())
                    .build();
        }
        int validatedCount = Math.max(10, Math.min(count, 100));
        boolean showSkipFirmSelection = Boolean.TRUE.equals(isMultiFirmUser);
        model.addAttribute("firmSearchForm", firmSearchForm);
        model.addAttribute("firmSearchResultCount", validatedCount);
        model.addAttribute("showSkipFirmSelection", showSkipFirmSelection);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Select firm");
        return "add-user-firm";
    }

    @PostMapping("/user/create/firm")
    public String postUserFirm(@Valid FirmSearchForm firmSearchForm, BindingResult result,
            HttpSession session, Model model) {

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while searching for firm: {}", result.getAllErrors());
            Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");
            boolean showSkipFirmSelection = Boolean.TRUE.equals(isMultiFirmUser);
            model.addAttribute("showSkipFirmSelection", showSkipFirmSelection);
            // Store the form in session to preserve input on redirect
            session.setAttribute("firmSearchForm", firmSearchForm);
            return "add-user-firm";
        }

        session.removeAttribute("firm");

        if (firmSearchForm.getSelectedFirmId() != null) {
            try {
                FirmDto selectedFirm = firmService.getFirm(firmSearchForm.getSelectedFirmId());
                selectedFirm.setSkipFirmSelection(firmSearchForm.isSkipFirmSelection());
                session.setAttribute("firm", selectedFirm);
            } catch (Exception e) {
                log.error("Error retrieving selected firm: {}", e.getMessage());
                result.rejectValue("firmSearch", "error.firm", "Invalid firm selection. Please try again.");
                return "add-user-firm";
            }
        } else if (firmSearchForm.getFirmSearch() != null && !firmSearchForm.getFirmSearch().isBlank()) {
            // Fallback: search by name if no specific firm was selected
            List<FirmDto> firms = firmService.getAllFirmsFromCache();
            FirmDto selectedFirm = firms.stream()
                    .filter(firm -> firm.getName().toLowerCase().contains(firmSearchForm.getFirmSearch().toLowerCase()))
                    .sorted((s1, s2) -> Integer.compare(relevance(s2, firmSearchForm.getFirmSearch()),
                            relevance(s1, firmSearchForm.getFirmSearch())))
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

    @GetMapping("/user/create/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String getUserCheckAnswers(Model model, HttpSession session) {
        EntraUserDto user = getObjectFromHttpSession(session, "user", EntraUserDto.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        model.addAttribute("user", user);

        FirmDto selectedFirm = (FirmDto) session.getAttribute("firm");
        model.addAttribute("firm", selectedFirm);

        boolean isUserManager = (boolean) session.getAttribute("isUserManager");
        model.addAttribute("isUserManager", isUserManager);

        Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");
        model.addAttribute("isMultiFirmUser", isMultiFirmUser != null ? isMultiFirmUser : false);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Check your answers");
        return "add-user-check-answers";
    }

    @PostMapping("/user/create/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CREATE_EXTERNAL_USER)")
    public String addUserCheckAnswers(HttpSession session, Authentication authentication, Model model) {
        Optional<EntraUserDto> userOptional = getObjectFromHttpSession(session, "user", EntraUserDto.class);
        Optional<FirmDto> firmOptional = getObjectFromHttpSession(session, "firm", FirmDto.class);
        Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");
        boolean userManager = getObjectFromHttpSession(session, "isUserManager", Boolean.class)
                .orElseThrow(CreateUserDetailsIncompleteException::new);
        if (userOptional.isPresent()) {
            EntraUserDto user = userOptional.get();

            // Handle firm selection - multi-firm users can skip firm selection
            FirmDto selectedFirm = firmOptional.orElseGet(() -> {
                // For multi-firm users who skip firm selection, create a placeholder
                if (Boolean.TRUE.equals(isMultiFirmUser)) {
                    FirmDto firm = new FirmDto();
                    firm.setSkipFirmSelection(true);
                    return firm;
                }
                throw new CreateUserDetailsIncompleteException();
            });

            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            try {
                EntraUser entraUser = userService.createUser(user, selectedFirm,
                        userManager, currentUserDto.getName(), isMultiFirmUser != null ? isMultiFirmUser : false);

                // Multi-firm users only have entra_user entry, no user_profile
                // Non-multi-firm users get a user_profile with firm assignment
                if (Boolean.TRUE.equals(isMultiFirmUser)) {
                    // Multi-firm user - no profile created
                    session.setAttribute("userProfile", null);
                } else {
                    // Regular user - profile created with firm
                    session.setAttribute("userProfile",
                            mapper.map(entraUser.getUserProfiles().stream().findFirst(), UserProfileDto.class));
                }

                String firmDescription = Boolean.TRUE.equals(isMultiFirmUser)
                        ? "(Multi-firm user)"
                        : selectedFirm.getName();
                CreateUserAuditEvent createUserAuditEvent = new CreateUserAuditEvent(currentUserDto, entraUser,
                        firmDescription, userManager);
                eventService.logEvent(createUserAuditEvent);
            } catch (TechServicesClientException techServicesClientException) {
                log.debug("Error creating user: {}", techServicesClientException.getMessage());
                model.addAttribute("errorMessage", techServicesClientException.getMessage());
                model.addAttribute("errors", techServicesClientException.getErrors());

                model.addAttribute("user", user);
                model.addAttribute("firm", selectedFirm);
                boolean isUserManager = (boolean) session.getAttribute("isUserManager");
                model.addAttribute("isUserManager", isUserManager);
                model.addAttribute(ModelAttributes.PAGE_TITLE, "Check your answers");
                return "add-user-check-answers";
            }

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

        // Get multi-firm user flag from session
        Boolean isMultiFirmUser = (Boolean) session.getAttribute("isMultiFirmUser");

        if (userOptional.isPresent()) {
            EntraUserDto user = userOptional.get();
            model.addAttribute("user", user);

            // Multi-firm users don't have a user profile - only entra_user entry
            // Regular users have a user profile with firm assignment
            if (Boolean.TRUE.equals(isMultiFirmUser)) {
                // No user profile for multi-firm users
                model.addAttribute("userProfile", null);
            } else {
                // Regular user should have a profile
                if (userProfileOptional.isPresent()) {
                    model.addAttribute("userProfile", userProfileOptional.get());
                } else {
                    log.error("No userProfile attribute was present in request for non-multi-firm user.");
                }
            }
        } else {
            log.error("No user attribute was present in request. User not added to model.");
        }

        model.addAttribute("isMultiFirmUser", isMultiFirmUser != null ? isMultiFirmUser : false);

        session.removeAttribute("user");
        session.removeAttribute("userProfile");
        session.removeAttribute("isMultiFirmUser");
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User created");
        return "add-user-created";
    }

    @GetMapping("/user/create/cancel/confirmation")
    public String confirmCancelCreateUser() {
        return "cancel-add-user-confirmation";
    }

    @GetMapping("/user/create/cancel")
    public String cancelUserCreation(HttpSession session) {
        session.removeAttribute("user");
        session.removeAttribute("firm");
        session.removeAttribute("selectedUserType");
        session.removeAttribute("isFirmAdmin");
        session.removeAttribute("isMultiFirmUser");
        session.removeAttribute("multiFirmForm");
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
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String editUserDetails(@PathVariable String id, Model model, HttpSession session) {
        UserProfileDto user = (UserProfileDto) session.getAttribute("user");
        if (Objects.isNull(user)) {
            user = userService.getUserProfileById(id).orElseThrow();
        }
        EditUserDetailsForm editUserDetailsForm = (EditUserDetailsForm) session.getAttribute("editUserDetailsForm");
        if (Objects.isNull(editUserDetailsForm)) {
            editUserDetailsForm = new EditUserDetailsForm();
            editUserDetailsForm.setFirstName(user.getEntraUser().getFirstName());
            editUserDetailsForm.setLastName(user.getEntraUser().getLastName());
            editUserDetailsForm.setEmail(user.getEntraUser().getEmail());
        }
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
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String updateUserDetails(@PathVariable String id,
            @Valid EditUserDetailsForm editUserDetailsForm, BindingResult result,
            HttpSession session) throws IOException {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        session.setAttribute("user", user);
        session.setAttribute("editUserDetailsForm", editUserDetailsForm);
        if (result.hasErrors()) {
            log.debug("Validation errors occurred while updating user details: {}", result.getAllErrors());
            // If there are validation errors, return to the edit user details page with
            // errors
            return "edit-user-details";
        }
        return "redirect:/admin/users/edit/" + id + "/details-check-answer";
    }

    @GetMapping("/users/edit/{id}/details-check-answer")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String updateUserDetailsCheck(@PathVariable String id, Model model,
            HttpSession session) throws IOException {
        EditUserDetailsForm editUserDetailsForm = (EditUserDetailsForm) session.getAttribute("editUserDetailsForm");
        if (Objects.isNull(editUserDetailsForm)) {
            return "redirect:/admin/users/manage/" + id;
        }
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        model.addAttribute("editUserDetailsForm", editUserDetailsForm);
        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE,
                "Edit user details - Check your answers - " + user.getFullName());
        return "edit-user-details-check-answer";
    }

    /**
     * Update user details
     *
     * @param id      User ID
     * @param session HttpSession to store user details
     * @return Redirect to user management page
     * @throws IOException              If an error occurs during user update
     * @throws IllegalArgumentException If the user ID is invalid or not found
     */
    @PostMapping("/users/edit/{id}/details-check-answer")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String updateUserDetailsSubmit(@PathVariable String id,
            HttpSession session) throws IOException {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        EditUserDetailsForm editUserDetailsForm = (EditUserDetailsForm) session.getAttribute("editUserDetailsForm");
        if (Objects.isNull(editUserDetailsForm)) {
            return "redirect:/admin/users/manage/" + id;
        }
        // Update user details
        // TODO audit log needed
        userService.updateUserDetails(user.getEntraUser().getId(), editUserDetailsForm.getFirstName(),
                editUserDetailsForm.getLastName());
        session.removeAttribute("editUserDetailsForm");
        return "redirect:/admin/users/edit/" + id + "/confirmation";
    }

    /**
     * Retrieves available apps for user and their currently assigned apps.
     */
    @GetMapping("/users/edit/{id}/apps")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String editUserApps(@PathVariable String id,
            @RequestParam(value = "errorMessage", required = false) String errorMessage,
            Model model, HttpSession session, Authentication authentication) {
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        UserType userType = user.getUserType();
        Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(id);
        List<AppDto> availableApps = userService.getAppsByUserType(userType);

        List<AppDto> editableApps = availableApps.stream()
                .filter(AppDto::isEnabled)
                .filter(app -> roleAssignmentService.canUserAssignRolesForApp(currentUserProfile, app))
                .toList();

        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> selectedAppRole = (Map<Integer, List<String>>) session
                .getAttribute("editUserAllSelectedRoles");
        if (Objects.isNull(selectedAppRole)) {
            // Add selected attribute to available apps based on user assigned apps
            editableApps.forEach(app -> {
                app.setSelected(userAssignedApps.stream()
                        .anyMatch(userApp -> userApp.getId().equals(app.getId())));
            });
        } else {
            List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class)
                    .orElse(List.of());
            editableApps.forEach(app -> {
                app.setSelected(selectedApps.contains(app.getId()));
            });
        }

        model.addAttribute("user", user);
        model.addAttribute("apps", editableApps);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Edit user services - " + user.getFullName());
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "edit-user-apps";
    }

    @PostMapping("/users/edit/{id}/apps")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public RedirectView setSelectedAppsEdit(@PathVariable String id,
            @RequestParam(value = "apps", required = false) List<String> apps,
            HttpSession session) {

        // Handle case where no apps are selected (apps will be null)
        List<String> selectedApps = apps != null ? apps : new ArrayList<>();
        session.setAttribute("selectedApps", selectedApps);
        if (selectedApps.isEmpty()) {
            // Ensure passed in ID is a valid UUID to avoid open redirects.
            session.setAttribute("editUserAllSelectedRoles", new HashMap<>());
            UUID uuid = UUID.fromString(id);
            return new RedirectView(String.format("/admin/users/edit/%s/roles-check-answer", uuid));
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
            @RequestParam(value = "errorMessage", required = false) String errorMessage,
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

        Integer currentSelectedAppIndex = selectedAppIndex != null ? selectedAppIndex : 0;

        // Ensure the index is within bounds
        if (currentSelectedAppIndex >= selectedApps.size()) {
            currentSelectedAppIndex = 0;
        }
        FirmType userFirmType = user.getFirm() != null ? user.getFirm().getType() : null;

        List<AppRoleDto> roles = userService.getAppRolesByAppIdAndUserType(selectedApps.get(currentSelectedAppIndex),
                user.getUserType(), userFirmType);
        UserProfile editorProfile = loginService.getCurrentProfile(authentication);
        roles = roleAssignmentService.filterRoles(editorProfile.getAppRoles(),
                roles.stream().map(role -> UUID.fromString(role.getId())).toList());
        List<AppRoleDto> userRoles = userService.getUserAppRolesByUserId(id);
        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> editUserAllSelectedRoles = (Map<Integer, List<String>>) session
                .getAttribute("editUserAllSelectedRoles");
        if (Objects.isNull(editUserAllSelectedRoles)) {
            editUserAllSelectedRoles = new HashMap<>();
        }
        // Get currently selected roles from session or use user's existing roles
        List<String> selectedRoles;
        if (editUserAllSelectedRoles.get(currentSelectedAppIndex) != null) {
            selectedRoles = editUserAllSelectedRoles.get(currentSelectedAppIndex);
        } else {
            selectedRoles = userRoles.stream().map(AppRoleDto::getId).collect(Collectors.toList());
        }
        AppDto currentApp = userService.getAppByAppId(selectedApps.get(currentSelectedAppIndex)).orElseThrow();

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

        String rolesBackUrl = currentSelectedAppIndex == 0
                ? "/admin/users/edit/" + id + "/apps"
                : "/admin/users/edit/" + id + "/roles?selectedAppIndex=" + (currentSelectedAppIndex - 1);
        model.addAttribute("backUrl", rolesBackUrl);

        session.setAttribute("editProfileUserRolesModel", model);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "Edit user roles - " + user.getFullName());
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }
        return "edit-user-roles";
    }

    /**
     * Update user roles for a specific app.
     *
     * @param id               User ID
     * @param selectedAppIndex Index of the currently selected app
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
            HttpSession session, Model model) {

        Model modelFromSession = (Model) session.getAttribute("editProfileUserRolesModel");
        if (result.hasErrors()) {
            final UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
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
            model.addAttribute("user", user);
            model.addAttribute("roles", roles);
            model.addAttribute("entraUser", modelFromSession.getAttribute("entraUser"));
            model.addAttribute("editUserRolesSelectedAppIndex", selectedAppIndex);
            model.addAttribute("editUserRolesCurrentApp",
                    modelFromSession.getAttribute("editUserRolesCurrentApp"));

            String rolesBackUrl = selectedAppIndex == 0
                    ? "/admin/users/edit/" + id + "/apps"
                    : "/admin/users/edit/" + id + "/roles?selectedAppIndex=" + (selectedAppIndex - 1);
            model.addAttribute("backUrl", rolesBackUrl);

            return "edit-user-roles";
        }

        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("editUserAllSelectedRoles");
        if (allSelectedRolesByPage == null) {
            allSelectedRolesByPage = new HashMap<>();
        }
        // Add the roles for the currently selected app to a map for lookup.
        if (rolesForm.getRoles() != null) {
            allSelectedRolesByPage.put(selectedAppIndex, rolesForm.getRoles());
        } else {
            allSelectedRolesByPage.put(selectedAppIndex, new ArrayList<>());
        }
        session.setAttribute("editUserAllSelectedRoles", allSelectedRolesByPage);
        List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class)
                .orElseGet(ArrayList::new);
        // Ensure passed in ID is a valid UUID to avoid open redirects.
        UUID uuid = UUID.fromString(id);
        if (selectedAppIndex >= selectedApps.size() - 1) {
            return "redirect:/admin/users/edit/" + uuid + "/roles-check-answer";
        } else {
            return "redirect:/admin/users/edit/" + uuid + "/roles?selectedAppIndex=" + (selectedAppIndex + 1);
        }
    }

    @GetMapping("/users/edit/{id}/roles-check-answer")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String editUserRolesCheckAnswer(@PathVariable String id,
            @RequestParam(value = "errorMessage", required = false) String errorMessage,
            Model model, HttpSession session, Authentication authentication) {
        UserProfile editorUserProfile = loginService.getCurrentProfile(authentication);
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> editUserAllSelectedRoles = (Map<Integer, List<String>>) session
                .getAttribute("editUserAllSelectedRoles");
        if (editUserAllSelectedRoles == null) {
            return "redirect:/admin/users/manage/" + id;
        }
        UserType userType = user.getUserType();
        List<String> selectedApps = getListFromHttpSession(session, "selectedApps", String.class)
                .orElseGet(ArrayList::new);
        Map<String, AppDto> editableApps = userService.getAppsByUserType(userType).stream()
                .filter(AppDto::isEnabled)
                .filter(app -> roleAssignmentService.canUserAssignRolesForApp(editorUserProfile, app))
                .filter(app -> selectedApps.contains(app.getId()))
                .collect(Collectors.toMap(AppDto::getId, Function.identity()));

        List<UUID> roleIds = editUserAllSelectedRoles.values()
                .stream()
                .flatMap(List::stream)
                .map(UUID::fromString)
                .toList();
        Map<String, AppRoleDto> roles = userService.getRolesByIdIn(roleIds);
        String backUrl = "";
        List<UserRole> selectedAppRole = new ArrayList<>();
        if (editUserAllSelectedRoles.isEmpty()) {
            backUrl = "/admin/users/edit/" + id + "/apps";
        } else {
            int size = editUserAllSelectedRoles.size();
            backUrl = "/admin/users/edit/" + id + "/roles?selectedAppIndex=" + Math.max(0, size - 1);
            for (Integer key : editUserAllSelectedRoles.keySet()) {
                String url = "/admin/users/edit/" + id + "/roles?selectedAppIndex=" + key;
                if (Objects.nonNull(editUserAllSelectedRoles.get(key))
                        && !editUserAllSelectedRoles.get(key).isEmpty()) {
                    List<String> selectedRoles = editUserAllSelectedRoles.get(key);
                    for (String selectedRole : selectedRoles) {
                        AppRoleDto role = roles.get(selectedRole);
                        UserRole userRole = new UserRole();
                        userRole.setRoleName(role.getName());
                        userRole.setAppName(role.getApp().getName());
                        userRole.setUrl(url);
                        selectedAppRole.add(userRole);
                    }
                } else {
                    UserRole userRole = new UserRole();
                    if (selectedApps.size() <= key) {
                        userRole.setAppName("Unknown app");
                        String err = "Unknown app selected, please re-select apps";
                        if (Objects.isNull(errorMessage)) {
                            errorMessage = err;
                        } else {
                            errorMessage += " " + err;
                        }
                    } else {
                        userRole.setAppName(editableApps.get(selectedApps.get(key)).getName());
                    }
                    userRole.setRoleName("No Role selected");
                    userRole.setUrl(url);
                    selectedAppRole.add(userRole);
                }
            }
        }
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }
        model.addAttribute("user", user);
        model.addAttribute("selectedAppRole", selectedAppRole);
        model.addAttribute("backUrl", backUrl);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Edit user services - " + user.getFullName());
        return "edit-user-roles-check-answer";
    }

    @PostMapping("/users/edit/{id}/roles-check-answer")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String editUserRolesCheckAnswerSubmit(@PathVariable String id, HttpSession session,
            Authentication authentication) {
        UUID uuid = UUID.fromString(id);

        UserProfile editorUserProfile = loginService.getCurrentProfile(authentication);
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        @SuppressWarnings("unchecked")
        Map<Integer, List<String>> allSelectedRolesByPage = (Map<Integer, List<String>>) session
                .getAttribute("editUserAllSelectedRoles");
        if (allSelectedRolesByPage == null) {
            return "redirect:/admin/users/manage/" + id;
        }
        List<String> allSelectedRoles = allSelectedRolesByPage.values().stream().filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
        List<String> nonEditableRoles = userService.getUserAppRolesByUserId(id).stream()
                .filter(role -> !role.getApp().isEnabled()
                        || !roleAssignmentService.canAssignRole(editorUserProfile.getAppRoles(), List.of(role.getId())))
                .map(AppRoleDto::getId)
                .toList();
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UserProfile editorProfile = loginService.getCurrentProfile(authentication);
        if (roleAssignmentService.canAssignRole(editorProfile.getAppRoles(), allSelectedRoles)) {
            Map<String, String> updateResult = userService.updateUserRoles(id, allSelectedRoles,
                    nonEditableRoles, currentUserDto.getUserId());
            if (updateResult.get("error") != null) {
                String errorMessage = updateResult.get("error");
                return "redirect:/admin/users/edit/" + uuid + "/roles-check-answer?errorMessage=" + errorMessage;
            }
            UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
                    editorProfile.getId(),
                    currentUserDto,
                    user.getEntraUser(), updateResult.get("diff"),
                    "role");
            eventService.logEvent(updateUserAuditEvent);
        }
        // Clear the session
        session.removeAttribute("editUserAllSelectedRoles");
        session.removeAttribute("selectedApps");
        return "redirect:/admin/users/edit/" + id + "/confirmation";
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
    public String editUserOffices(@PathVariable String id, Model model, Authentication authentication, HttpSession session) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        session.removeAttribute("editUserOfficesModel");
        // Get user's current offices
        List<OfficeDto> userOffices = userService.getUserOfficesByUserId(id);
        Set<String> userOfficeIds = userOffices.stream()
                .map(office -> office.getId().toString())
                .collect(Collectors.toSet());

        // Get user's available offices by firm
        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(id);
        List<UUID> firmIds = userFirms.stream().map(FirmDto::getId).collect(Collectors.toList());
        List<Office> allOffices = officeService.getOfficesByFirms(firmIds);

        // Check if user has access to all offices or no office
        // Create form object or load from session if exist
        OfficesForm officesForm = (OfficesForm) session.getAttribute("officesForm");
        List<String> selectedOffices = new ArrayList<>();
        AllOfficesNoOffice result = new AllOfficesNoOffice(false, false);
        if (officesForm == null) {
            officesForm = new OfficesForm();
            if (userOfficeIds.isEmpty()) {
                if (user.isUnrestrictedOfficeAccess()) {
                    result = new AllOfficesNoOffice(true, false);
                    selectedOffices.add(ALL);
                } else {
                    result = new AllOfficesNoOffice(false, true);
                    selectedOffices.add(NO_OFFICES);
                }
            } else {
                selectedOffices.addAll(userOfficeIds);
            }
            officesForm.setOffices(selectedOffices);
        } else {
            result = verifyAllOffices(Optional.of(officesForm.getOffices()), user, userOffices);
            if (result.hasAllOffices()) {
                selectedOffices.add(ALL);
            } else if (result.hasNoOffices()) {
                selectedOffices.add(NO_OFFICES);
            } else {
                selectedOffices.addAll(officesForm.getOffices());
                userOfficeIds = new HashSet<>(officesForm.getOffices());
            }
        }

        Set<String> finalUserOfficeIds = userOfficeIds;
        final List<OfficeModel> officeData = allOffices.stream()
                .map(office -> new OfficeModel(
                        office.getCode(),
                        office.getAddress() == null ? null :
                                new OfficeModel.Address(office.getAddress().getAddressLine1(),
                                        office.getAddress().getAddressLine2(),
                                        office.getAddress().getAddressLine3(),
                                        office.getAddress().getCity(), office.getAddress().getPostcode()),
                        office.getId().toString(),
                        finalUserOfficeIds.contains(office.getId().toString())))
                .collect(Collectors.toList());
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        boolean shouldShowNoOffice = shouldShowNoOfficeOption(currentUserProfile, user, session);
        model.addAttribute("user", user);
        model.addAttribute("officesForm", officesForm);
        model.addAttribute("officeData", officeData);
        model.addAttribute("hasAllOffices", result.hasAllOffices());
        model.addAttribute("hasNoOffices", result.hasNoOffices());
        model.addAttribute("shouldShowNoOffice", shouldShowNoOffice);
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
            Model model, HttpSession session) {
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
            model.addAttribute("shouldShowNoOffice", modelFromSession.getAttribute("shouldShowNoOffice"));

            return "edit-user-offices";
        }
        session.setAttribute("officesForm", officesForm);
        return "redirect:/admin/users/edit/" + id + "/offices-check-answer";
    }

    @GetMapping("/users/edit/{id}/offices-check-answer")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_OFFICE) && @accessControlService.canEditUser(#id)")
    public String updateUserOfficesCheck(@PathVariable String id,
            Model model, HttpSession session) {
        OfficesForm officesForm = (OfficesForm) session.getAttribute("officesForm");
        if (officesForm == null) {
            return "redirect:/admin/users/edit/" + id + "/offices";
        }
        // Update user offices
        List<String> selectedOffices = officesForm.getOffices() != null ? officesForm.getOffices() : new ArrayList<>();
        List<OfficeModel> selectOfficesDisplay = new ArrayList<>();
        if (!(selectedOffices.contains(ALL) || selectedOffices.contains(NO_OFFICES))) {
            Model modelFromSession = (Model) session.getAttribute("editUserOfficesModel");
            if (modelFromSession != null) {
                @SuppressWarnings("unchecked")
                List<OfficeModel> officeData = (List<OfficeModel>) modelFromSession.getAttribute("officeData");
                if (officeData != null) {
                    List<String> selectedOfficeIds = officesForm.getOffices() != null ? officesForm.getOffices()
                            : new ArrayList<>();
                    for (OfficeModel office : officeData) {
                        if (selectedOfficeIds.contains(office.getId())) {
                            selectOfficesDisplay.add(office);
                        }
                    }
                }
            }
        }
        Model modelFromSession = (Model) session.getAttribute("editUserOfficesModel");
        model.addAttribute("userOffices", selectOfficesDisplay);
        model.addAttribute("user", modelFromSession.getAttribute("user"));
        model.addAttribute("hasAllOffices", selectedOffices.getFirst().equals(ALL));
        model.addAttribute("hasNoOffices", selectedOffices.getFirst().equals(NO_OFFICES));
        return "edit-user-offices-check-answer";
    }

    @PostMapping("/users/edit/{id}/offices-check-answer")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_OFFICE) && @accessControlService.canEditUser(#id)")
    public String updateUserOfficesSubmit(@PathVariable String id,
            Authentication authentication,
            HttpSession session) throws IOException {
        OfficesForm officesForm = (OfficesForm) session.getAttribute("officesForm");
        if (officesForm == null) {
            return "redirect:/admin/users/edit/" + id + "/offices";
        }
        // Update user offices
        UserProfileDto userProfileDto = userService.getUserProfileById(id).orElseThrow();
        List<String> selectedOffices = officesForm.getOffices() != null ? officesForm.getOffices() : new ArrayList<>();

        String changed = userService.updateUserOffices(id, selectedOffices);
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
                userProfileDto.getId(),
                currentUserDto,
                userProfileDto.getEntraUser(),
                changed, "office");
        eventService.logEvent(updateUserAuditEvent);
        // Clear the session model
        session.removeAttribute("editUserOfficesModel");
        session.removeAttribute("officesForm");
        return "redirect:/admin/users/edit/" + id + "/confirmation";
    }

    /**
     * Update user detail Flow - Show confirmation page
     */
    @GetMapping("/users/edit/{id}/confirmation")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String editUserConfirmation(@PathVariable String id, Model model) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "User updated - " + user.getFullName());
        return "edit-user-confirmation";
    }

    @GetMapping("/users/edit/{id}/cancel/confirmation")
    public String confirmCancelEditUserApps(@PathVariable String id, Model model) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        model.addAttribute("user", user);
        return "cancel-edit-user-confirmation";
    }

    @GetMapping("/users/edit/{id}/cancel/offices/confirmation")
    public String confirmCancelEditUserOffices(@PathVariable String id, Model model) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        model.addAttribute("user", user);
        return "cancel-edit-user-firm-confirmation";
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

        // Edit User Apps Form

        // Edit User Offices Form
        session.removeAttribute("editUserOfficesModel");
        session.removeAttribute("officesForm");

        // Convert to Multi-Firm Form
        session.removeAttribute("convertToMultiFirmForm");

        // Clear any success messages
        session.removeAttribute("successMessage");
        return "redirect:/admin/users/manage/" + id;
    }

    /**
     * Convert to Multi-Firm Flow - Show conversion form
     */
    @GetMapping("/users/edit/{id}/convert-to-multi-firm")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_EXTERNAL_USER)"
            + "&& @accessControlService.canEditUser(#id)"
            + "&& @accessControlService.authenticatedUserIsInternal()"
            + "&& @accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CONVERT_USER_TO_MULTI_FIRM)")
    public String convertToMultiFirm(@PathVariable String id, ConvertToMultiFirmForm convertToMultiFirmForm,
            Model model, HttpSession session, RedirectAttributes redirectAttributes) {

        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();

        // Check if user is not external
        if (user.getUserType() != UserType.EXTERNAL) {
            log.warn("Attempt to convert internal user {} to multi-firm", id);
            throw new RuntimeException("Only external users can be converted to multi-firm users");
        }

        // Check if already multi-firm
        if (user.getEntraUser().isMultiFirmUser()) {
            log.warn("Attempt to convert user {} who is already multi-firm", id);
            redirectAttributes.addFlashAttribute("errorMessage", "This user is already a multi-firm user");
            return "redirect:/admin/users/manage/" + id;
        }

        // Pre-populate form from session if it exists
        ConvertToMultiFirmForm sessionForm = (ConvertToMultiFirmForm) session.getAttribute("convertToMultiFirmForm");
        if (sessionForm != null) {
            convertToMultiFirmForm.setConvertToMultiFirm(sessionForm.isConvertToMultiFirm());
        }

        model.addAttribute("convertToMultiFirmForm", convertToMultiFirmForm);
        model.addAttribute("user", user);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Convert to multi-firm user - " + user.getFullName());
        return "convert-to-multi-firm/index";
    }

    @PostMapping("/users/edit/{id}/convert-to-multi-firm")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_EXTERNAL_USER)"
            + " && @accessControlService.canEditUser(#id)"
            + " && @accessControlService.authenticatedUserIsInternal()"
            + " && @accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).CONVERT_USER_TO_MULTI_FIRM)")
    public String convertToMultiFirmPost(@PathVariable String id,
            @Valid ConvertToMultiFirmForm convertToMultiFirmForm,
            BindingResult result,
            HttpSession session,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            Model model) {

        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();

        // Check if user is not external
        if (user.getUserType() != UserType.EXTERNAL) {
            log.warn("Attempt to convert internal user {} to multi-firm", id);
            throw new RuntimeException("Only external users can be converted to multi-firm users");
        }

        // Check if already multi-firm
        if (user.getEntraUser().isMultiFirmUser()) {
            log.warn("Attempt to convert user {} who is already multi-firm", id);
            redirectAttributes.addFlashAttribute("errorMessage", "This user is already a multi-firm user");
            return "redirect:/admin/users/manage/" + id;
        }

        if (result.hasErrors()) {
            log.debug("Validation errors occurred while converting user to multi-firm: {}", result.getAllErrors());
            model.addAttribute("convertToMultiFirmForm", convertToMultiFirmForm);
            model.addAttribute("user", user);
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Convert to multi-firm user - " + user.getFullName());
            return "convert-to-multi-firm/index";
        }

        // If user chose not to convert, redirect back to manage user page
        if (Boolean.FALSE.equals(convertToMultiFirmForm.isConvertToMultiFirm())) {
            return "redirect:/admin/users/manage/" + id;
        }

        // User chose "Yes" - perform the conversion
        try {
            // Convert the user to multi-firm
            userService.convertToMultiFirmUser(user.getEntraUser().getId());

            // Create audit event
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            EntraUserDto entraUserDto = mapper.map(user.getEntraUser(), EntraUserDto.class);
            ConvertToMultiFirmAuditEvent auditEvent = new ConvertToMultiFirmAuditEvent(currentUserDto, entraUserDto);
            eventService.logEvent(auditEvent);

            log.info("Successfully converted user {} to multi-firm status", id);

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "User has been successfully converted to a multi-firm user");

            // Redirect to manage user page
            return "redirect:/admin/users/manage/" + id;

        } catch (Exception e) {
            log.error("Failed to convert user {} to multi-firm: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to convert user to multi-firm: " + e.getMessage());
            model.addAttribute("convertToMultiFirmForm", convertToMultiFirmForm);
            model.addAttribute("user", user);
            return "convert-to-multi-firm/index";
        }
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
            HttpSession session, Authentication authentication) {
        UserProfile editorUserProfile = loginService.getCurrentProfile(authentication);
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        UserType userType = user.getUserType();
        Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(id);
        List<AppDto> availableApps = userService.getAppsByUserType(userType);

        List<AppDto> editableApps = availableApps.stream()
                .filter(AppDto::isEnabled)
                .filter(app -> roleAssignmentService.canUserAssignRolesForApp(editorUserProfile, app))
                .toList();

        Optional<List<String>> selectedApps = getListFromHttpSession(session, "grantAccessSelectedApps", String.class);
        if (selectedApps.isPresent()) {
            editableApps.forEach(app -> app.setSelected(selectedApps.get().contains(app.getId())));
        } else {
            editableApps.forEach(app -> {
                app.setSelected(userAssignedApps.stream()
                        .anyMatch(userApp -> userApp.getId().equals(app.getId())));
            });
        }

        model.addAttribute("user", user);
        model.addAttribute("apps", editableApps);

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
            // unchecked all the apps
            List<AppDto> apps = (List<AppDto>) modelFromSession.getAttribute("apps");
            if (apps != null) {
                apps.forEach(app -> app.setSelected(false));
            }
            model.addAttribute("user", modelFromSession.getAttribute("user"));
            model.addAttribute("apps", apps);
            return "grant-access-user-apps";
        }

        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);

        // Handle case where no apps are selected (apps will be null)
        List<String> selectedApps = applicationsForm.getApps() != null ? applicationsForm.getApps() : new ArrayList<>();

        session.setAttribute("grantAccessSelectedApps", selectedApps);
        List<String> nonEditableRoles = userService.getUserAppRolesByUserId(id).stream()
                .filter(role -> !role.getApp().isEnabled()
                        || !roleAssignmentService.canAssignRole(currentUserProfile.getAppRoles(), List.of(role.getId())))
                .map(AppRoleDto::getId)
                .toList();
        session.setAttribute("nonEditableRoles", nonEditableRoles);
        session.setAttribute("grantAccessSelectedApps", selectedApps);

        // Clear the grantAccessUserAppsModel from session to avoid stale data
        session.removeAttribute("grantAccessUserAppsModel");

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
            Model model, HttpSession session,
            RedirectAttributes redirectAttributes) {

        final UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        Optional<List<String>> selectedAppsOptional = getListFromHttpSession(session, "grantAccessSelectedApps",
                String.class);

        if (selectedAppsOptional.isEmpty() || selectedAppsOptional.get().isEmpty()) {
            log.warn("No apps to assign while granting access to user {}. Redirecting to app selection.", id);
            redirectAttributes.addFlashAttribute("errorMessage", "You must select an app for assignment.");
            return "redirect:/users/grant-access/" + id + "/apps";
        }

        List<String> selectedApps = selectedAppsOptional.get();

        int currentSelectedAppIndex = selectedAppIndex != null ? selectedAppIndex : 0;

        // Ensure the index is within bounds
        if (currentSelectedAppIndex >= selectedApps.size()) {
            currentSelectedAppIndex = 0;
        }
        FirmType userFirmType = user.getFirm() != null ? user.getFirm().getType() : null;
        UserProfile editorProfile = loginService.getCurrentProfile(authentication);

        List<AppRoleDto> roles = userService.getAppRolesByAppIdAndUserType(selectedApps.get(currentSelectedAppIndex),
                user.getUserType(), userFirmType);
        roles = roleAssignmentService.filterRoles(editorProfile.getAppRoles(),
                roles.stream().map(role -> UUID.fromString(role.getId())).toList());
        List<AppRoleDto> userRoles = userService.getUserAppRolesByUserId(id);

        AppDto currentApp = userService.getAppByAppId(selectedApps.get(currentSelectedAppIndex)).orElseThrow();
        // Get currently selected roles from session or use user's existing roles
        Set<String> selectedRoles = getSetFromHttpSession(session, "allSelectedRoles", String.class)
                .orElseGet(() -> userRoles.stream().map(AppRoleDto::getId).collect(Collectors.toSet()));

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
            UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
            // Clear the grantAccessUserRolesModel and page roles from session to avoid
            // stale data
            session.removeAttribute("grantAccessUserRolesModel");
            session.removeAttribute("grantAccessAllSelectedRoles");
            // Flatten the map to a single list of all selected roles across all pages.
            Set<String> allSelectedRoles = allSelectedRolesByPage.values().stream().filter(Objects::nonNull)
                    .flatMap(List::stream)
                    .collect(Collectors.toSet());
            List<String> nonEditableRoles = userService.getUserAppRolesByUserId(id).stream()
                    .filter(role -> !role.getApp().isEnabled()
                            || !roleAssignmentService.canUserAssignRolesForApp(currentUserProfile, role.getApp()))
                    .map(AppRoleDto::getId)
                    .toList();
            session.setAttribute("allSelectedRoles", allSelectedRoles);
            session.setAttribute("nonEditableRoles", nonEditableRoles);

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
    public String grantAccessEditUserOffices(@PathVariable String id, Model model, Authentication authentication, HttpSession session) {
        Optional<List<String>> selectedOfficesOptional = getListFromHttpSession(session, "selectedOffices", String.class);
        List<OfficeDto> userOffices = List.of();

        if (selectedOfficesOptional.isEmpty()) {
            userOffices = userService.getUserOfficesByUserId(id);
        } else {
            List<String> selectedOffices = selectedOfficesOptional.get();
            if (!selectedOffices.isEmpty()
                    && !(selectedOffices.contains(ALL) || selectedOffices.contains(NO_OFFICES))) {
                userOffices = officeService.getOfficesByIds(selectedOfficesOptional.get());
            }
        }
        // Get user's current offices
        Set<String> userOfficeIds = userOffices.stream()
                .map(office -> office.getId().toString())
                .collect(Collectors.toSet());

        // Get user's available offices by firm
        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(id);
        List<UUID> firmIds = userFirms.stream().map(FirmDto::getId).collect(Collectors.toList());
        List<Office> allOffices = officeService.getOfficesByFirms(firmIds);

        // Check if user has access to all offices
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();

        final List<OfficeModel> officeData = allOffices.stream()
                .map(office -> new OfficeModel(
                        office.getCode(),
                        office.getAddress() == null ? null :
                                OfficeModel.Address.builder().addressLine1(office.getAddress().getAddressLine1())
                                        .addressLine2(office.getAddress().getAddressLine2())
                                        .addressLine3(office.getAddress().getAddressLine3())
                                        .city(office.getAddress().getCity())
                                        .postcode(office.getAddress().getPostcode()).build(),
                        office.getId().toString(),
                        userOfficeIds.contains(office.getId().toString())))
                .collect(Collectors.toList());

        // Create form object
        OfficesForm officesForm = new OfficesForm();
        List<String> selectedOffices = new ArrayList<>();

        AllOfficesNoOffice result = verifyAllOffices(selectedOfficesOptional, user, userOffices);
        if (result.hasAllOffices()) {
            selectedOffices.add(ALL);
        } else if (result.hasNoOffices()) {
            selectedOffices.add(NO_OFFICES);
        } else {
            selectedOffices.addAll(userOfficeIds);
        }

        officesForm.setOffices(selectedOffices);
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);

        boolean shouldShowNoOffice = shouldShowNoOfficeOption(currentUserProfile, user, session);

        model.addAttribute("user", user);
        model.addAttribute("officesForm", officesForm);
        model.addAttribute("officeData", officeData);
        model.addAttribute("hasAllOffices", result.hasAllOffices());
        model.addAttribute("hasNoOffices", result.hasNoOffices());
        model.addAttribute("shouldShowNoOffice", shouldShowNoOffice);
        // Store the model in session to handle validation errors later
        session.setAttribute("grantAccessUserOfficesModel", model);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Grant access - Select offices - " + user.getFullName());
        return "grant-access-user-offices";
    }

    private boolean shouldShowNoOfficeOption(UserProfile currentUserProfile, UserProfileDto selectedUser, HttpSession session) {
        if (selectedUser.getEntraUser().isMultiFirmUser()) {
            return false;
        }
        boolean shouldShowNoOffice = false;
        boolean isProvideAdmin = false;
        boolean isAdminRoles = RolesUtils.isCurrentProfileExternalUserAdmin(currentUserProfile)
                || RolesUtils.isCurrentProfileGlobalAdmin(currentUserProfile);
        if (isAdminRoles) {
            // When the user is still pending, we need to use the option they previously selected on the role screen
            if (selectedUser.getUserProfileStatus().equals(UserProfileStatus.PENDING)) {
                Optional<Set<String>> allSelectedRoles = getSetFromHttpSession(session, "allSelectedRoles", String.class);
                List<AppRoleDto> userAppRoles = appRoleService.getByIds(allSelectedRoles.get());
                isProvideAdmin = RolesUtils.isProvideAdmin(userAppRoles);
            } else {
                isProvideAdmin = RolesUtils.isProvideAdmin(selectedUser.getAppRoles());
            }

            if (!isProvideAdmin) {
                shouldShowNoOffice = true;
            }
        }
        return shouldShowNoOffice;
    }

    private static AllOfficesNoOffice verifyAllOffices(Optional<List<String>> selectedOfficesOptional, UserProfileDto user, List<OfficeDto> userOffices) {
        boolean hasNoOffices;
        boolean hasAllOffices;
        if (selectedOfficesOptional.isEmpty()) {
            hasAllOffices = user.isUnrestrictedOfficeAccess() && userOffices.isEmpty();
            hasNoOffices = !user.isUnrestrictedOfficeAccess() && userOffices.isEmpty();
        } else {
            hasAllOffices = selectedOfficesOptional.get().contains(ALL);
            hasNoOffices = selectedOfficesOptional.get().contains(NO_OFFICES);
        }
        return new AllOfficesNoOffice(hasAllOffices, hasNoOffices);
    }

    private record AllOfficesNoOffice(boolean hasAllOffices, boolean hasNoOffices) {
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
            model.addAttribute("shouldShowNoOffice", modelFromSession.getAttribute("shouldShowNoOffice"));

            return "grant-access-user-offices";
        }

        // Update user offices
        List<String> selectedOffices = officesForm.getOffices() != null ? officesForm.getOffices() : new ArrayList<>();

        session.setAttribute("selectedOffices", selectedOffices);

        // Clear grant access session data
        session.removeAttribute("grantAccessUserOfficesModel");

        return "redirect:/admin/users/grant-access/" + id + "/check-answers";
    }

    /**
     * Grant Access Flow - Check answers page
     */
    @GetMapping("/users/grant-access/{id}/check-answers")
    @PreAuthorize("@accessControlService.canEditUser(#id)")
    public String grantAccessCheckAnswers(@PathVariable String id,
                                          Model model,
                                          HttpSession session,
                                          Authentication authentication) {
        UserProfile editorUserProfile = loginService.getCurrentProfile(authentication);
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        UserType userType = user.getUserType();
        // Get user's current app roles from session
        Set<String> allSelectedRoles = getSetFromHttpSession(session, "allSelectedRoles", String.class)
                .orElseThrow(() -> new RuntimeException("No roles selected for assignment"));

        List<AppRoleDto> userAppRoles = appRoleService.getByIds(allSelectedRoles);
        List<AppRoleDto> editableUserAppRoles = userAppRoles.stream()
                .filter(role -> roleAssignmentService.canUserAssignRolesForApp(editorUserProfile, role.getApp()))
                .toList();

        // Group roles by app name and sort by app name
        Map<String, List<AppRoleDto>> groupedAppRoles = editableUserAppRoles.stream().sorted()
                .collect(Collectors.groupingBy(
                        appRole -> appRole.getApp().getName(),
                        LinkedHashMap::new, // Preserve insertion order
                        Collectors.toList()));

        // get all offices from session
        List<String> selectedOffices = getListFromHttpSession(session, "selectedOffices", String.class)
                .orElseThrow(() -> new RuntimeException("No Office selected for assignment"));

        List<OfficeDto> userOffices = new ArrayList<>();

        if (!selectedOffices.isEmpty() && !(selectedOffices.contains(ALL)
                || selectedOffices.contains(NO_OFFICES))) {
            userOffices = officeService.getOfficesByIds(selectedOffices);

        }
        // Sort the map by app name
        Map<String, List<AppRoleDto>> sortedGroupedAppRoles = groupedAppRoles.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));

        model.addAttribute("user", user);
        model.addAttribute("userAppRoles", editableUserAppRoles);
        model.addAttribute("groupedAppRoles", sortedGroupedAppRoles);
        model.addAttribute("userOffices", userOffices);
        model.addAttribute("externalUser", user.getUserType() == UserType.EXTERNAL);
        model.addAttribute("hasAllOffices", selectedOffices.contains(ALL));
        model.addAttribute("hasNoOffices", selectedOffices.contains(NO_OFFICES));

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
                    userProfileDto.getId(),
                    currentUserDto,
                    userProfileDto.getEntraUser(),
                    "Removed app role: " + roleName + " for app: " + appId,
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

            Set<String> allSelectedRoles = getSetFromHttpSession(session, "allSelectedRoles", String.class)
                    .orElseThrow(() -> new RuntimeException("No roles selected for assignment"));
            List<String> nonEditableRoles = getListFromHttpSession(session, "nonEditableRoles", String.class)
                    .orElseGet(ArrayList::new);

            UserProfile editorProfile = loginService.getCurrentProfile(authentication);
            if (roleAssignmentService.canAssignRole(editorProfile.getAppRoles(), allSelectedRoles)) {
                Map<String, String> updateResult = userService.updateUserRoles(id, allSelectedRoles, nonEditableRoles,
                        currentUserDto.getUserId());
                UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
                        editorProfile.getId(),
                        currentUserDto,
                        userProfileDto.getEntraUser(), updateResult.get("diff"),
                        "role");
                eventService.logEvent(updateUserAuditEvent);
            }

            List<String> selectedOffices = getListFromHttpSession(session, "selectedOffices", String.class).orElseThrow();

            String changed = userService.updateUserOffices(id, selectedOffices);

            UpdateUserAuditEvent updateUserAuditEvent = new UpdateUserAuditEvent(
                    userProfileDto.getId(),
                    currentUserDto,
                    userProfileDto.getEntraUser(),
                    changed, "office");
            eventService.logEvent(updateUserAuditEvent);


            // Update user profile status to COMPLETE to finalize access grant
            userService.grantAccess(id, currentUserDto.getName());

            // Create audit event for the final access grant
            updateUserAuditEvent = new UpdateUserAuditEvent(
                    userProfileDto.getId(),
                    currentUserDto,
                    userProfileDto.getEntraUser(),
                    "Access granted",
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
        session.removeAttribute("selectedOffices");

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

    @GetMapping("/users/grant-access/{id}/cancel/confirmation")
    public String confirmCancelGrantingAccess(@PathVariable String id, Model model) {
        UserProfileDto user = userService.getUserProfileById(id).orElseThrow();
        model.addAttribute("user", user);
        return "cancel-grant-access-user-confirmation";
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
        session.removeAttribute("selectedOffices");
        session.removeAttribute("allSelectedRoles");
        session.removeAttribute("selectedApps");
        session.removeAttribute("grantAccessSelectedApps");
        session.removeAttribute("nonEditableRoles");

        // Clear any success messages
        session.removeAttribute("successMessage");

        return "redirect:/admin/users/manage/" + id;
    }

    /**
     * Handle authorization exceptions when user lacks permissions to access
     * specific users
     */
    @ExceptionHandler({ AuthorizationDeniedException.class, AccessDeniedException.class })
    public RedirectView handleAuthorizationException(Exception ex, HttpSession session,
            HttpServletRequest request) {
        Object requestedPath = session != null ? session.getAttribute("SPRING_SECURITY_SAVED_REQUEST") : null;
        String uri = request != null ? request.getRequestURI() : "unknown";
        String method = request != null ? request.getMethod() : "unknown";
        String referer = request != null ? request.getHeader("Referer") : null;
        log.warn(
                "Authorization denied while accessing user: reason='{}', method='{}', uri='{}', referer='{}', savedRequest='{}'",
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

    private Map<String, Object> processRequestFilters(int size, int page, String sort, String direction,
            String usertype, String search, boolean showFirmAdmins, boolean showMultiFirmUsers,
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
                showMultiFirmUsers = sessionFilters.containsKey("showMultiFirmUsers")
                        ? (Boolean) sessionFilters.get("showMultiFirmUsers")
                        : showMultiFirmUsers;
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
                "showMultiFirmUsers", showMultiFirmUsers,
                "usertype", usertype != null ? usertype : "",
                "firmSearchForm", firmSearchForm != null ? firmSearchForm : FirmSearchForm.builder().build());

        // Store current filter state in session for future back navigation
        session.setAttribute("userListFilters", result);

        return result;
    }

    private void handleResendVerification(String id, Model model) {
        if (!accessControlService.canSendVerificationEmail(id)) {
            throw new AccessDeniedException("User does not have permission to send verification email.");
        }

        try {
            TechServicesApiResponse<SendUserVerificationEmailResponse> response = userService
                    .sendVerificationEmail(id);
            if (response.isSuccess()) {
                model.addAttribute("successMessage", response.getData().getMessage());
            } else {
                model.addAttribute("errorMessage", response.getError().getMessage());
            }
        } catch (RuntimeException runtimeException) {
            log.error("Error sending activation code for user profile: {}", id, runtimeException);
            throw runtimeException;
        }
    }

    /**
     * Display firm reassignment page for external users
     * Only available to users with EDIT_USER_FIRM permission (External User Admins)
     */
    @GetMapping("/users/reassign-firm/{id}")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_FIRM)")
    public String showFirmReassignmentPage(@PathVariable String id,
            @RequestParam(value = "selectedFirmId", required = false) String selectedFirmId,
            @RequestParam(value = "selectedFirmName", required = false) String selectedFirmName,
            @RequestParam(value = "reason", required = false) String reason,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);

            if (optionalUser.isEmpty()) {
                log.warn("User not found for reassignment: {}", id);
                model.addAttribute("errorMessage", "User not found");
                return "errors/error-generic";
            }

            UserProfileDto user = optionalUser.get();

            if (user.getUserType() != UserType.EXTERNAL) {
                log.warn("Attempted to reassign internal user: {}", id);
                model.addAttribute("errorMessage", "Only external users can be reassigned to different firms");
                return "errors/error-generic";
            }

            if (!user.getAppRoles().isEmpty()) {
                log.warn("App roles not removed prior to firm switch for user: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage",
                        "All app assignments must be removed before assigning a different firm to the account");
                model.addAttribute("errorMessage",
                        "All app assignments must be removed before assigning a different firm to the account");
                return "redirect:/admin/users/manage/" + id;
            }

            FirmReassignmentForm form = new FirmReassignmentForm();
            if (selectedFirmId != null && selectedFirmName != null) {
                form.setSelectedFirmId(UUID.fromString(selectedFirmId));
                form.setFirmSearch(selectedFirmName);
            }

            model.addAttribute("user", user);
            model.addAttribute("firmReassignmentForm", form);

            if (reason != null && !reason.trim().isEmpty()) {
                model.addAttribute("preservedReason", reason);
            }

            return "reassign-firm/select-firm";

        } catch (Exception e) {
            log.error("Error loading firm reassignment page for user {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the page");
            return "errors/error-generic";
        }
    }

    /**
     * Handle firm selection step of reassignment process
     */
    @PostMapping("/users/reassign-firm/{id}/select-firm")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_FIRM)")
    public String processFirmSelection(@PathVariable String id,
            @Valid @ModelAttribute("firmReassignmentForm") FirmReassignmentForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);

            if (optionalUser.isEmpty()) {
                log.warn("User not found for reassignment: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/admin/users/manage/" + id;
            }

            UserProfileDto user = optionalUser.get();

            if (user.getUserType() != UserType.EXTERNAL) {
                log.warn("Attempted to reassign internal user: {}", id);
                model.addAttribute("errorMessage", "Only external users can be reassigned to different firms");
                model.addAttribute("user", user);
                return "reassign-firm/select-firm";
            }

            if (bindingResult.hasFieldErrors("firmSearch") || bindingResult.hasFieldErrors("selectedFirmId")) {
                model.addAttribute("user", user);
                return "reassign-firm/select-firm";
            }

            if (form.getSelectedFirmId() == null) {
                model.addAttribute("errorMessage", "Please select a firm from the search results");
                model.addAttribute("user", user);
                return "reassign-firm/select-firm";
            }

            FirmDto selectedFirm = firmService.getFirm(form.getSelectedFirmId());

            if (selectedFirm == null) {
                model.addAttribute("errorMessage", "Selected firm not found");
                model.addAttribute("user", user);
                return "reassign-firm/select-firm";
            }

            redirectAttributes.addAttribute("selectedFirmId", form.getSelectedFirmId());
            redirectAttributes.addAttribute("selectedFirmName", selectedFirm.getName());

            return "redirect:/admin/users/reassign-firm/" + id + "/reason";

        } catch (Exception e) {
            log.error("Error processing firm selection for user {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while processing your selection");
            userService.getUserProfileById(id).ifPresent(u -> model.addAttribute("user", u));
            return "reassign-firm/select-firm";
        }
    }

    /**
     * Display reason page for firm reassignment
     */
    @GetMapping("/users/reassign-firm/{id}/reason")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_FIRM)")
    public String showReassignmentReasonPage(@PathVariable String id,
            @RequestParam("selectedFirmId") String selectedFirmId,
            @RequestParam("selectedFirmName") String selectedFirmName,
            @RequestParam(value = "reason", required = false) String existingReason,
            Model model) {
        try {
            Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);

            if (optionalUser.isEmpty()) {
                log.warn("User not found for reassignment: {}", id);
                model.addAttribute("errorMessage", "User not found");
                return "errors/error-generic";
            }

            UserProfileDto user = optionalUser.get();

            if (user.getUserType() != UserType.EXTERNAL) {
                log.warn("Attempted to reassign internal user: {}", id);
                model.addAttribute("errorMessage", "Only external users can be reassigned to different firms");
                return "errors/error-generic";
            }

            if (!user.getAppRoles().isEmpty()) {
                log.warn("App roles not removed prior to firm switch for user: {}", id);
                model.addAttribute("errorMessage",
                        "All app assignments must be removed before assigning a different firm to the account");
                return "errors/error-generic";
            }

            ReassignmentReasonForm reasonForm = new ReassignmentReasonForm();
            if (existingReason != null && !existingReason.trim().isEmpty()) {
                reasonForm.setReason(existingReason);
            }

            model.addAttribute("user", user);
            model.addAttribute("selectedFirmId", selectedFirmId);
            model.addAttribute("selectedFirmName", selectedFirmName);
            model.addAttribute("reassignmentReasonForm", reasonForm);

            return "reassign-firm/reason";

        } catch (Exception e) {
            log.error("Error loading reason page for user {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the page");
            return "errors/error-generic";
        }
    }

    /**
     * Process reason submission and redirect to check answers
     */
    @PostMapping("/users/reassign-firm/{id}/reason")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_FIRM)")
    public String processReasonSubmission(@PathVariable String id,
            @RequestParam("selectedFirmId") String selectedFirmId,
            @RequestParam("firmSearch") String selectedFirmName,
            @Valid @ModelAttribute("reassignmentReasonForm") ReassignmentReasonForm reasonForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);

            if (optionalUser.isEmpty()) {
                log.warn("User not found for reassignment: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/admin/users/manage/" + id;
            }

            UserProfileDto user = optionalUser.get();
            model.addAttribute("user", user);
            model.addAttribute("selectedFirmId", selectedFirmId);
            model.addAttribute("selectedFirmName", selectedFirmName);

            if (user.getUserType() != UserType.EXTERNAL) {
                log.warn("Attempted to reassign internal user: {}", id);
                model.addAttribute("errorMessage", "Only external users can be reassigned to different firms");
                return "errors/error-generic";
            }

            if (bindingResult.hasFieldErrors("reason")) {
                return "reassign-firm/reason";
            }

            if (reasonForm.getReason() == null || reasonForm.getReason().trim().isEmpty()) {
                model.addAttribute("errorMessage", "Please provide a reason for the reassignment");
                return "reassign-firm/reason";
            }

            redirectAttributes.addAttribute("selectedFirmId", selectedFirmId);
            redirectAttributes.addAttribute("selectedFirmName", selectedFirmName);
            redirectAttributes.addAttribute("reason", reasonForm.getReason().trim());

            return "redirect:/admin/users/reassign-firm/" + id + "/check-answers";

        } catch (Exception e) {
            log.error("Error processing reason for user {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while processing your reason");
            model.addAttribute("selectedFirmId", selectedFirmId);
            model.addAttribute("selectedFirmName", selectedFirmName);
            return "reassign-firm/reason";
        }
    }

    /**
     * Display check answers page for firm reassignment
     */
    @GetMapping("/users/reassign-firm/{id}/check-answers")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_FIRM)")
    public String showCheckAnswersPage(@PathVariable String id,
            @RequestParam("selectedFirmId") String selectedFirmId,
            @RequestParam("selectedFirmName") String selectedFirmName,
            @RequestParam("reason") String reason,
            Model model) {
        try {
            Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);

            if (optionalUser.isEmpty()) {
                log.warn("User not found for reassignment: {}", id);
                model.addAttribute("errorMessage", "User not found");
                return "errors/error-generic";
            }

            UserProfileDto user = optionalUser.get();

            if (user.getUserType() != UserType.EXTERNAL) {
                log.warn("Attempted to reassign internal user: {}", id);
                model.addAttribute("errorMessage", "Only external users can be reassigned to different firms");
                return "errors/error-generic";
            }

            if (!user.getAppRoles().isEmpty()) {
                log.warn("App roles not removed prior to firm switch for user: {}", id);
                model.addAttribute("errorMessage", "All app assignments must be removed before assigning a different firm to the account");
                return "errors/error-generic";
            }

            model.addAttribute("user", user);
            model.addAttribute("selectedFirmId", selectedFirmId);
            model.addAttribute("selectedFirmName", selectedFirmName);
            model.addAttribute("reason", reason);

            return "reassign-firm/check-answers";

        } catch (Exception e) {
            log.error("Error loading check answers page for user {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the page");
            return "errors/error-generic";
        }
    }

    /**
     * Process final confirmation and perform firm reassignment
     */
    @PostMapping("/users/reassign-firm/{id}/confirmation")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_FIRM)")
    public String processFirmReassignment(@PathVariable String id,
            @RequestParam("selectedFirmId") String selectedFirmId,
            @RequestParam("selectedFirmName") String selectedFirmName,
            @RequestParam("reason") String reason,
            Model model,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);

            if (optionalUser.isEmpty()) {
                log.warn("User not found for reassignment: {}", id);
                redirectAttributes.addFlashAttribute("errorMessage", "User not found");
                return "redirect:/admin/users/manage/" + id;
            }

            UserProfileDto user = optionalUser.get();

            if (user.getUserType() != UserType.EXTERNAL) {
                log.warn("Attempted to reassign internal user: {}", id);
                model.addAttribute("errorMessage", "Only external users can be reassigned to different firms");
                model.addAttribute("user", user);
                model.addAttribute("selectedFirmId", selectedFirmId);
                model.addAttribute("selectedFirmName", selectedFirmName);
                model.addAttribute("reason", reason);
                return "reassign-firm/check-answers";
            }

            if (reason == null || reason.trim().isEmpty()) {
                model.addAttribute("errorMessage", "Please provide a reason for the reassignment");
                model.addAttribute("user", user);
                model.addAttribute("selectedFirmId", selectedFirmId);
                model.addAttribute("selectedFirmName", selectedFirmName);
                model.addAttribute("reason", reason);
                return "reassign-firm/check-answers";
            }

            CurrentUserDto currentUser = loginService.getCurrentUser(authentication);
            EntraUser currentEntraUser = loginService.getCurrentEntraUser(authentication);

            userService.reassignUserFirm(
                    id,
                    UUID.fromString(selectedFirmId),
                    reason.trim(),
                    currentEntraUser.getId(),
                    currentUser.getName());

            return "redirect:/admin/users/reassign-firm/" + id + "/confirmation";

        } catch (IllegalArgumentException e) {
            log.warn("Invalid reassignment request for user {}: {}", id, e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", userService.getUserProfileById(id).orElse(null));
            model.addAttribute("selectedFirmId", selectedFirmId);
            model.addAttribute("selectedFirmName", selectedFirmName);
            model.addAttribute("reason", reason);
            return "reassign-firm/check-answers";

        } catch (IllegalStateException e) {
            log.warn("Invalid state for reassignment of user {}: {}", id, e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("user", userService.getUserProfileById(id).orElse(null));
            model.addAttribute("selectedFirmId", selectedFirmId);
            model.addAttribute("selectedFirmName", selectedFirmName);
            model.addAttribute("reason", reason);
            return "reassign-firm/check-answers";

        } catch (Exception e) {
            log.error("Error reassigning firm for user {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while reassigning the user");
            model.addAttribute("user", userService.getUserProfileById(id).orElse(null));
            model.addAttribute("selectedFirmId", selectedFirmId);
            model.addAttribute("selectedFirmName", selectedFirmName);
            model.addAttribute("reason", reason);
            return "reassign-firm/check-answers";
        }
    }

    /**
     * Display confirmation page after successful firm reassignment
     */
    @GetMapping("/users/reassign-firm/{id}/confirmation")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_FIRM)")
    public String showConfirmationPage(@PathVariable String id, Model model) {
        try {
            Optional<UserProfileDto> optionalUser = userService.getUserProfileById(id);

            if (optionalUser.isEmpty()) {
                log.warn("User not found for confirmation page: {}", id);
                model.addAttribute("errorMessage", "User not found");
                return "errors/error-generic";
            }

            UserProfileDto user = optionalUser.get();
            model.addAttribute("user", user);

            return "reassign-firm/confirmation";

        } catch (Exception e) {
            log.error("Error loading confirmation page for user {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "An error occurred while loading the page");
            return "errors/error-generic";
        }
    }

    /**
     * Handle firm reassignment for external users (API endpoint)
     * Only available to users with EDIT_USER_FIRM permission (External User Admins)
     */
    @PostMapping("/users/reassign-firm/{id}/api")
    @PreAuthorize("@accessControlService.authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).EDIT_USER_FIRM)")
    @ResponseBody
    public ResponseEntity<?> reassignUserFirmApi(@PathVariable String id,
            @RequestParam("newFirmId") String newFirmId,
            @RequestParam("reason") String reason,
            Authentication authentication) {
        try {
            CurrentUserDto currentUser = loginService.getCurrentUser(authentication);
            EntraUser currentEntraUser = loginService.getCurrentEntraUser(authentication);

            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Reason for reassignment is required"));
            }

            if (newFirmId == null || newFirmId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "New firm selection is required"));
            }

            userService.reassignUserFirm(
                    id,
                    UUID.fromString(newFirmId),
                    reason.trim(),
                    currentEntraUser.getId(),
                    currentUser.getName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User successfully reassigned to new firm"));

        } catch (IllegalArgumentException e) {
            log.warn("Invalid reassignment request for user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("Invalid state for reassignment of user {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));

        } catch (Exception e) {
            log.error("Error reassigning firm for user {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An error occurred while reassigning the user"));
        }
    }
}
