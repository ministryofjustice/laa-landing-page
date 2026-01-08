package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.RequestInformation;

import jakarta.transaction.Transactional;
import uk.gov.justice.laa.portal.landingpage.config.LaaAppsConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto.AuditProfileDto;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedAuditUsers;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchResultsDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.forms.UserTypeForm;
import uk.gov.justice.laa.portal.landingpage.model.DeletedUser;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplicationForView;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.repository.projection.UserAuditProjection;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

/**
 * userService
 */
@Service
public class UserService {

    private static final int BATCH_SIZE = 20;
    private final OfficeRepository officeRepository;
    private final GraphServiceClient graphClient;
    private final EntraUserRepository entraUserRepository;
    private final AppRepository appRepository;
    private final AppRoleRepository appRoleRepository;
    private final ModelMapper mapper;
    private final LaaAppsConfig.LaaApplicationsList laaApplicationsList;
    private final TechServicesClient techServicesClient;
    private final UserProfileRepository userProfileRepository;
    private final RoleChangeNotificationService roleChangeNotificationService;
    private final FirmService firmService;
    private final NotificationService notificationService;
    Logger logger = LoggerFactory.getLogger(this.getClass());

    public UserService(@Qualifier("graphServiceClient") GraphServiceClient graphClient,
                       EntraUserRepository entraUserRepository, AppRepository appRepository,
                       AppRoleRepository appRoleRepository, ModelMapper mapper,
                       OfficeRepository officeRepository,
                       LaaAppsConfig.LaaApplicationsList laaApplicationsList,
                       TechServicesClient techServicesClient, UserProfileRepository userProfileRepository,
                       RoleChangeNotificationService roleChangeNotificationService, FirmService firmService,
                       NotificationService notificationService) {
        this.graphClient = graphClient;
        this.entraUserRepository = entraUserRepository;
        this.appRepository = appRepository;
        this.appRoleRepository = appRoleRepository;
        this.mapper = mapper;
        this.officeRepository = officeRepository;
        this.laaApplicationsList = laaApplicationsList;
        this.techServicesClient = techServicesClient;
        this.userProfileRepository = userProfileRepository;
        this.roleChangeNotificationService = roleChangeNotificationService;
        this.firmService = firmService;
        this.notificationService = notificationService;
    }

    static <T> List<List<T>> partitionBasedOnSize(List<T> inputList, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i += size) {
            partitions.add(inputList.subList(i, Math.min(i + size, inputList.size())));
        }
        return partitions;
    }

    /**
     * Returns all Users from Entra
     * <p>
     * Limitations - only returns 100 users currently
     * </p>
     *
     * @return {@code List<User>}
     */
    public List<User> getAllUsers() {
        UserCollectionResponse response = graphClient.users().get();
        return response != null ? response.getValue() : Collections.emptyList();
    }

    @Transactional
    public Map<String, String> updateUserRoles(String userProfileId, Collection<String> selectedRoles,
            List<String> nonEditableRoles, UUID modifierId) {
        Set<String> allAssignableRoles = new HashSet<>(selectedRoles);
        allAssignableRoles.addAll(nonEditableRoles);
        List<AppRole> roles = appRoleRepository.findAllById(
                allAssignableRoles.stream().map(UUID::fromString).collect(Collectors.toList()));
        Optional<UserProfile> optionalUserProfile = userProfileRepository.findById(UUID.fromString(userProfileId));

        String diff = "";
        Map<String, String> result = new HashMap<>();
        if (optionalUserProfile.isPresent()) {
            UserProfile userProfile = optionalUserProfile.get();
            boolean self = userProfile.getEntraUser().getEntraOid().equals(modifierId.toString());
            List<UserType> modifierTypes = findUserTypeByUserEntraId(modifierId.toString());
            boolean internal = modifierTypes.contains(UserType.INTERNAL);
            int before = roles.size();
            roles = roles.stream().filter(appRole -> Arrays.stream(appRole.getUserTypeRestriction())
                    .anyMatch(userType -> userType == userProfile.getUserType())).toList();
            int after = roles.size();
            if (after < before) {
                logger.warn("Attempt to assign internal role user ID {}.",
                        userProfile.getEntraUser().getId());
            }

            Set<AppRole> newRoles = new HashSet<>(roles);
            Set<AppRole> oldRoles = Objects.isNull(userProfile.getAppRoles()) ? new HashSet<>()
                    : new HashSet<>(userProfile.getAppRoles());
            String error = roleCoverage(oldRoles, newRoles, userProfile.getFirm(),
                    userProfile.getId().toString(), self, internal);
            if (!error.isEmpty()) {
                result.put("error", error);
                return result;
            }

            Set<String> oldPuiRoles = filterByPuiRoles(userProfile.getAppRoles());
            Set<String> newPuiRoles = filterByPuiRoles(newRoles);

            // Update roles
            userProfile.setAppRoles(newRoles);
            diff = diffRole(oldRoles, newRoles);

            // Try to send role change notification with retry logic before saving
            boolean notificationSuccess = roleChangeNotificationService.sendMessage(userProfile,
                    newPuiRoles, oldPuiRoles);
            userProfile.setLastCcmsSyncSuccessful(notificationSuccess);

            // Save user profile with ccms sync status
            userProfileRepository.save(userProfile);
            techServicesClient.updateRoleAssignment(userProfile.getEntraUser().getId());
            result.put("diff", diff);
        } else {
            logger.warn("User profile with id {} not found. Could not update roles.",
                    userProfileId);
        }
        return result;
    }

    protected static String diffRole(Set<AppRole> oldRoles, Set<AppRole> newRoles) {
        List<UUID> oldIds = oldRoles.stream().map(AppRole::getId).toList();
        List<UUID> newIds = newRoles.stream().map(AppRole::getId).toList();
        List<String> removed = oldRoles.stream().filter(role -> !newIds.contains(role.getId()))
                .map(AppRole::getName).toList();
        List<String> added = newRoles.stream().filter(role -> !oldIds.contains(role.getId()))
                .map(AppRole::getName).toList();
        String changed = "";
        if (!removed.isEmpty()) {
            changed += "Removed: " + String.join(", ", removed);
        }
        if (!added.isEmpty()) {
            if (!changed.isEmpty()) {
                changed += ", ";
            }
            changed += "Added: " + String.join(", ", added);
        }
        return changed;
    }

    protected String roleCoverage(Set<AppRole> oldRoles, Set<AppRole> newRoles, Firm firm,
            String userId, boolean self, boolean internal) {
        if (oldRoles.isEmpty()) {
            return "";
        }
        List<UUID> newIds = newRoles.stream().map(AppRole::getId).toList();
        List<AppRole> removed = oldRoles.stream().filter(role -> !newIds.contains(role.getId())).toList();
        PageRequest pageRequest = PageRequest.of(0, 2);
        if (Objects.nonNull(firm)) {
            String userManagerRoleName = internal ? "External User Manager" : "Firm User Manager";
            Optional<AppRole> optionalUserManagerRole = appRoleRepository.findByName(userManagerRoleName);
            if (optionalUserManagerRole.isPresent()) {
                AppRole userManagerRole = optionalUserManagerRole.get();
                Page<UserProfile> existingManagers = userProfileRepository.findFirmUserByAuthzRoleAndFirm(firm.getId(),
                        userManagerRole.getName(), pageRequest);
                boolean removeManager = removed.stream().anyMatch(
                        role -> role.getId().equals(optionalUserManagerRole.get().getId()));
                if (removeManager && self) {
                    logger.warn("Attempt to remove own User Manager role, from user profile {}.",
                            userId);
                    return "You cannot remove your own User Manager role";
                }
                if (!internal && existingManagers.getTotalElements() < 2 && removeManager) {
                    logger.warn("Attempt to remove last firm User Manager, from user profile {}.",
                            userId);
                    return "User Manager role could not be removed, this is the last User Manager of "
                            + firm.getName();
                }
            }
        } else {
            Optional<AppRole> globalAdminRole = appRoleRepository.findByName("Global Admin");
            if (globalAdminRole.isPresent()) {
                Page<UserProfile> existingAdmins = userProfileRepository
                        .findInternalUserByAuthzRole("Global Admin", pageRequest);
                boolean removeGlobalAdmin = removed.stream()
                        .anyMatch(role -> role.getId().equals(globalAdminRole.get().getId()));
                if (existingAdmins.getTotalElements() < 2 && removeGlobalAdmin) {
                    logger.warn("Attempt to remove last Global Admin, from user profile {}.",
                            userId);
                    return "Global Admin role could not be removed, this is the last Global Admin";
                }
            }
        }
        return "";
    }

    private Set<String> filterByPuiRoles(Set<AppRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>();
        }

        return roles.stream().filter(AppRole::isLegacySync).filter(Objects::nonNull)
                .map(AppRole::getCcmsCode).collect(Collectors.toSet());
    }

    public TechServicesApiResponse<SendUserVerificationEmailResponse> sendVerificationEmail(
            String userProfileId) {
        Optional<UserProfileDto> optionalUserProfile = getUserProfileById(userProfileId);

        return optionalUserProfile.map(
                userProfile -> techServicesClient.sendEmailVerification(userProfile.getEntraUser()))
                .orElseThrow(() -> new RuntimeException("Failed to send verification email!"));
    }

    public List<DirectoryRole> getDirectoryRolesByUserId(String userId) {
        return Objects.requireNonNull(graphClient.users().byUserId(userId).memberOf().get())
                .getValue().stream().filter(obj -> obj instanceof DirectoryRole)
                .map(obj -> (DirectoryRole) obj).collect(Collectors.toList());
    }

    public Optional<UserProfileDto> getActiveProfileByUserId(String userId) {
        EntraUser user = entraUserRepository.findById(UUID.fromString(userId)).orElseThrow(() -> {
            logger.error("User not found for the given user user id: {}", userId);
            return new RuntimeException(
                    String.format("User not found for the given user user id: %s", userId));
        });

        if (user.getUserProfiles() == null || user.getUserProfiles().isEmpty()) {
            logger.error("User profile not found for the given user id: {}", userId);
            throw new RuntimeException(
                    String.format("User profile not found for the given user id: %s", userId));
        }

        return user.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst()
                .map(userProfile -> mapper.map(userProfile, UserProfileDto.class));
    }

    public Optional<EntraUserDto> getEntraUserById(String userId) {
        return entraUserRepository.findById(UUID.fromString(userId))
                .map(user -> mapper.map(user, EntraUserDto.class));
    }

    public Optional<EntraUserDto> getEntraUserByEmail(String email) {
        return entraUserRepository.findByEmailIgnoreCase(email)
                .map(user -> mapper.map(user, EntraUserDto.class));
    }

    public Optional<UserProfileDto> getUserProfileById(String userId) {
        return userProfileRepository.findById(UUID.fromString(userId))
                .map(user -> mapper.map(user, UserProfileDto.class));
    }

    /**
     * Delete an EXTERNAL user and all related local records.
     *
     * @param userProfileId the ID of the user profile (UUID as String)
     * @param reason        the reason for deletion, used for logging/audit
     * @param actorId       the UUID of the actor performing the deletion (for
     *                      logging)
     */
    @Transactional
    public DeletedUser deleteExternalUser(String userProfileId, String reason, UUID actorId) {
        Optional<UserProfile> optionalUserProfile = userProfileRepository.findById(UUID.fromString(userProfileId));
        if (optionalUserProfile.isEmpty()) {
            throw new RuntimeException("User profile not found: " + userProfileId);
        }

        UserProfile userProfile = optionalUserProfile.get();
        if (userProfile.getUserType() != UserType.EXTERNAL) {
            throw new RuntimeException("Deletion is only permitted for external users");
        }

        EntraUser entraUser = userProfile.getEntraUser();
        if (entraUser == null) {
            throw new RuntimeException(
                    "Associated Entra user not found for profile: " + userProfileId);
        }

        logger.info(
                "Deleting external user. actorId={}, userProfileId={}, entraUserId={}, email={}, reason=\"{}\"",
                actorId, userProfileId, entraUser.getId(), entraUser.getEmail(), reason);

        // first send update to tech services
        techServicesClient.deleteRoleAssignment(userProfile.getEntraUser().getId());

        // hard delete from silas db
        List<UserProfile> profiles = userProfileRepository.findAllByEntraUser(entraUser);
        DeletedUser.DeletedUserBuilder builder = new DeletedUser().toBuilder()
                .deletedUserId(userProfile.getEntraUser().getId());
        if (profiles != null && !profiles.isEmpty()) {
            for (UserProfile up : profiles) {
                Set<String> puiRoles = new HashSet<>();
                if (up.getAppRoles() != null) {
                    builder.removedRolesCount(
                            up.getAppRoles().isEmpty() ? 0 : up.getAppRoles().size());
                    puiRoles = filterByPuiRoles(up.getAppRoles());
                    up.getAppRoles().clear();
                    if (!puiRoles.isEmpty()) {
                        roleChangeNotificationService.sendMessage(up, Collections.emptySet(),
                                puiRoles);
                    }
                }
                if (up.getOffices() != null) {
                    builder.detachedOfficesCount(
                            up.getOffices().isEmpty() ? 0 : up.getOffices().size());
                    up.getOffices().clear();
                }
                up.setEntraUser(null);
                userProfileRepository.save(up);
            }
            userProfileRepository.flush();
            userProfileRepository.deleteAll(profiles);
            userProfileRepository.flush();
        }
        entraUserRepository.delete(entraUser);
        entraUserRepository.flush();
        return builder.build();
    }

    /**
     * Delete a specific firm profile from a multi-firm user.
     *
     * @param userProfileId the ID of the user profile to delete (UUID as String)
     * @param actorId       the UUID of the actor performing the deletion (for
     *                      logging)
     * @return true if deletion was successful, false otherwise
     * @throws RuntimeException if profile not found, user not multi-firm, or
     *                          attempting to delete
     *                          last profile
     */
    @Transactional
    public boolean deleteFirmProfile(String userProfileId, UUID actorId) {
        UserProfile userProfile = userProfileRepository.findById(UUID.fromString(userProfileId)).orElseThrow(
                () -> new RuntimeException("User profile not found: " + userProfileId));

        EntraUser entraUser = userProfile.getEntraUser();

        if (entraUser == null) {
            throw new RuntimeException(
                    "Associated Entra user not found for profile: " + userProfileId);
        }

        // Verify this is a multi-firm user
        if (!entraUser.isMultiFirmUser()) {
            throw new RuntimeException(
                    "User is not a multi-firm user. Profile deletion is only allowed for multi-firm users.");
        }

        final String firmName = userProfile.getFirm() != null ? userProfile.getFirm().getName() : "Unknown";

        logger.info(
                "Deleting firm profile for multi-firm user. actorId={}, userProfileId={}, entraUserId={}, email={}, firm={}",
                actorId, userProfileId, entraUser.getId(), entraUser.getEmail(), firmName);

        // Handle PUI role changes notification (like in deleteExternalUser)
        Set<String> puiRoles = new HashSet<>();
        if (userProfile.getAppRoles() != null && !userProfile.getAppRoles().isEmpty()) {
            puiRoles = filterByPuiRoles(userProfile.getAppRoles());
            userProfile.getAppRoles().clear();
            if (!puiRoles.isEmpty()) {
                roleChangeNotificationService.sendMessage(userProfile, Collections.emptySet(),
                        puiRoles);
            }
        }

        // Clear offices association
        if (userProfile.getOffices() != null && !userProfile.getOffices().isEmpty()) {
            userProfile.getOffices().clear();
        }

        // Remove bidirectional association: profile from entra user and entra user from
        // profile
        entraUser.getUserProfiles().remove(userProfile);
        userProfile.setEntraUser(null);
        userProfileRepository.save(userProfile);
        userProfileRepository.flush();

        // Store whether this was the active profile before deletion
        final boolean wasActiveProfile = userProfile.isActiveProfile();

        // Delete the profile
        userProfileRepository.delete(userProfile);
        userProfileRepository.flush();

        // If the deleted profile was active, set another one as active
        if (wasActiveProfile) {
            // Reload profiles from database to get current state
            List<UserProfile> remainingProfiles = userProfileRepository.findAllByEntraUser(entraUser);

            // Check if any profile is already active
            boolean hasActiveProfile = remainingProfiles.stream().anyMatch(UserProfile::isActiveProfile);

            // If no active profile exists, set the first one as active
            if (!hasActiveProfile && !remainingProfiles.isEmpty()) {
                UserProfile newActiveProfile = remainingProfiles.get(0);
                newActiveProfile.setActiveProfile(true);
                userProfileRepository.save(newActiveProfile);
                logger.info("Set new active profile for user {} to firm {}", entraUser.getEmail(),
                        newActiveProfile.getFirm() != null ? newActiveProfile.getFirm().getName()
                                : "Unknown");
            }
        }

        // Notify revoke firm access
        notificationService.notifyRevokeFirmAccess(UUID.fromString(userProfileId), entraUser.getFirstName(),
                entraUser.getEmail(), firmName);

        return true;
    }

    /**
     * Delete an external user who has no firm profiles (Entra-only user) Notifies
     * Tech Services to
     * remove Entra group memberships and deletes from local database
     *
     * @param entraUserId the ID of the EntraUser to delete (UUID as String)
     * @param reason      the reason for deletion, used for logging/audit
     * @param actorId     the UUID of the actor performing the deletion (for
     *                    logging)
     * @return DeletedUser containing deletion metadata
     * @throws RuntimeException if user not found, has profiles, or is internal user
     */
    @Transactional
    public DeletedUser deleteEntraUserWithoutProfile(String entraUserId, String reason,
            UUID actorId) {
        EntraUser entraUser = entraUserRepository.findById(UUID.fromString(entraUserId))
                .orElseThrow(() -> new RuntimeException("Entra user not found: " + entraUserId));

        // Check if user has any profiles - this method is only for users without
        // profiles
        List<UserProfile> profiles = userProfileRepository.findAllByEntraUser(entraUser);
        if (profiles != null && !profiles.isEmpty()) {
            throw new RuntimeException(
                    "User has existing profiles. Use deleteExternalUser or deleteFirmProfile instead.");
        }

        // Cannot delete internal users
        if (entraUser.getUserProfiles() != null && entraUser.getUserProfiles().stream()
                .anyMatch(p -> p.getUserType() == UserType.INTERNAL)) {
            throw new RuntimeException("Cannot delete internal users");
        }

        logger.info(
                "Deleting Entra user without profile. actorId={}, entraUserId={}, email={}, reason=\"{}\"",
                actorId, entraUserId, entraUser.getEmail(), reason);

        // Notify Tech Services to remove Entra group memberships
        try {
            techServicesClient.deleteRoleAssignment(entraUser.getId());
        } catch (Exception ex) {
            logger.error("Failed to notify Tech Services during user deletion: {}", ex.getMessage(),
                    ex);
            throw new RuntimeException(
                    "Failed to notify Entra of user deletion: " + ex.getMessage(), ex);
        }

        // Delete from local database
        entraUserRepository.delete(entraUser);
        entraUserRepository.flush();

        return DeletedUser.builder().deletedUserId(entraUser.getId()).removedRolesCount(0)
                .detachedOfficesCount(0).build();
    }

    public Optional<UserType> getUserTypeByUserId(String userId) {
        Optional<EntraUser> optionalEntraUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalEntraUser.isPresent()) {
            return getUserTypeByEntraUser(optionalEntraUser.get());
        } else {
            return Optional.empty();
        }
    }

    public Optional<UserType> getUserTypeByEntraUser(EntraUser user) {
        return user.getUserProfiles().stream().filter(UserProfile::isActiveProfile)
                .map(UserProfile::getUserType).findFirst();
    }

    public String formatLastSignInDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ENGLISH);

        return dateTime.format(formatter);
    }

    private PaginatedUsers getPageOfUsers(Supplier<Page<UserSearchResultsDto>> pageSupplier) {
        Page<UserSearchResultsDto> userPage = pageSupplier.get();
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setTotalUsers(userPage.getTotalElements());
        paginatedUsers.setUsers(userPage.stream().toList());
        paginatedUsers.setTotalPages(userPage.getTotalPages());
        return paginatedUsers;
    }

    /**
     * Retrieves a paginated list of users based on the provided search criteria.
     * <p>
     * This method is intended to be the primary entry point for searching users.
     * The
     * {@link UserSearchCriteria} object should be extended to include all necessary
     * search
     * parameters.
     * </p>
     *
     * @param searchCriteria the criteria to filter users by
     * @param page           the page number to retrieve (1-based index)
     * @param pageSize       the number of users per page
     * @param sort           the field to sort by
     * @param direction      the direction of sorting ("asc" or "desc")
     * @return a {@link PaginatedUsers} object containing the users for the
     *         requested page
     */
    public PaginatedUsers getPageOfUsersBySearch(UserSearchCriteria searchCriteria, int page,
            int pageSize, String sort, String direction) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), pageSize, getSort(sort, direction));
        Page<UserSearchResultsDto> userProfilePage = userProfileRepository.findBySearchParams(searchCriteria,
                pageRequest);
        return getPageOfUsers(() -> userProfilePage);
    }

    protected Sort getSort(String field, String direction) {
        if (Objects.isNull(field) || field.isEmpty()) {
            return Sort.by(Sort.Order.desc("userProfileStatus"),
                    Sort.Order.asc("entraUser.firstName"));
        }
        Sort.Direction order;
        if (direction == null || direction.isEmpty()) {
            order = Sort.Direction.ASC;
        } else {
            order = Sort.Direction.valueOf(direction.toUpperCase());
        }
        return switch (field.toUpperCase()) {
            case "FIRSTNAME" -> Sort.by(order, "entraUser.firstName");
            case "LASTNAME" -> Sort.by(order, "entraUser.lastName");
            case "EMAIL" -> Sort.by(order, "entraUser.email");
            case "USERSTATUS" -> Sort.by(order, "userProfileStatus");
            case "USERTYPE" -> Sort.by(order, "userType", "entraUser.multiFirmUser");
            case "FIRMNAME" -> Sort.by(order, "firm.name");
            default -> throw new IllegalArgumentException("Invalid field: " + field);
        };
    }

    public List<UserType> findUserTypeByUserEntraId(String entraId) {
        EntraUser user = entraUserRepository.findByEntraOid(entraId).orElseThrow(() -> {
            logger.error("User not found for the given user entra id: {}", entraId);
            return new RuntimeException(
                    String.format("User not found for the given user entra id: %s", entraId));
        });

        if (user.getUserProfiles() == null || user.getUserProfiles().isEmpty()) {
            logger.error("User profile not found for the given entra id: {}", entraId);
            throw new RuntimeException(
                    String.format("User profile not found for the given entra id: %s", entraId));
        }

        return user.getUserProfiles().stream().filter(UserProfile::isActiveProfile)
                .map(UserProfile::getUserType).collect(Collectors.toList());

    }

    public EntraUserDto findUserByUserEntraId(String entraId) {
        EntraUser entraUser = entraUserRepository.findByEntraOid(entraId).orElseThrow(() -> {
            logger.error("User not found for the given user entra user id: {}", entraId);
            return new RuntimeException(
                    String.format("User not found for the given user entra id: %s", entraId));
        });

        return mapper.map(entraUser, EntraUserDto.class);

    }

    @Async
    public void disableUsers(List<String> ids) throws IOException {
        Collection<List<String>> batchIds = partitionBasedOnSize(ids, BATCH_SIZE);
        for (List<String> batch : batchIds) {
            BatchRequestContent batchRequestContent = new BatchRequestContent(graphClient);
            for (String id : batch) {
                User user = new User();
                user.setAccountEnabled(false);
                RequestInformation patchMessage = graphClient.users().byUserId(id).toPatchRequestInformation(user);
                batchRequestContent.addBatchRequestStep(patchMessage);
            }
            graphClient.getBatchRequestBuilder().post(batchRequestContent, null);
        }
    }

    public List<AppDto> getApps() {
        return appRepository.findAll().stream().map(app -> mapper.map(app, AppDto.class))
                .collect(Collectors.toList());
    }

    public List<AppDto> getAppsByUserType(UserType userType) {
        return appRoleRepository.findByUserTypeRestrictionContains(userType.name()).stream()
                .map(AppRole::getApp).distinct().map(app -> mapper.map(app, AppDto.class)).sorted()
                .toList();
    }

    public List<AppRoleDto> getAllAvailableRolesForApps(List<String> selectedApps) {
        // Fetch selected apps
        List<App> apps = appRepository.findAllById(
                selectedApps.stream().map(UUID::fromString).collect(Collectors.toList()));
        // Return roles
        return apps.stream().flatMap(app -> app.getAppRoles().stream())
                .map(appRole -> mapper.map(appRole, AppRoleDto.class)).collect(Collectors.toList());
    }

    public EntraUser createUser(EntraUserDto user, FirmDto firm, boolean isUserManager,
            String createdBy, boolean isMultiFirmUser) {

        TechServicesApiResponse<RegisterUserResponse> registerUserResponse = techServicesClient.registerNewUser(user);
        if (!registerUserResponse.isSuccess()) {
            throw new TechServicesClientException(registerUserResponse.getError().getMessage(),
                    registerUserResponse.getError().getCode(),
                    registerUserResponse.getError().getErrors());
        }

        RegisterUserResponse.CreatedUser createdUser = registerUserResponse.getData().getCreatedUser();

        if (createdUser != null && user.getEmail().equalsIgnoreCase(createdUser.getMail())) {
            user.setEntraOid(createdUser.getId());
        } else {
            throw new RuntimeException("User creation failed");
        }

        return persistNewUser(user, firm, isUserManager, createdBy, isMultiFirmUser);
    }

    private EntraUser persistNewUser(EntraUserDto newUser, FirmDto firmDto, boolean isUserManager,
            String createdBy, boolean isMultiFirmUser) {
        EntraUser entraUser = mapper.map(newUser, EntraUser.class);

        entraUser.setMultiFirmUser(isMultiFirmUser);
        entraUser.setEntraOid(newUser.getEntraOid());
        entraUser.setUserStatus(UserStatus.ACTIVE);

        if (!isMultiFirmUser && firmDto.isSkipFirmSelection()) {
            logger.error("User {} {} is not a multi-firm user, firm selection can not be skipped",
                    entraUser.getFirstName(), entraUser.getLastName());
            throw new RuntimeException(String.format(
                    "User %s %s is not a multi-firm user, firm selection can not be skipped",
                    entraUser.getFirstName(), entraUser.getLastName()));
        }

        if (!isMultiFirmUser || !firmDto.isSkipFirmSelection()) {
            Set<AppRole> appRoles = new HashSet<>();
            if (isUserManager) {
                Optional<AppRole> firmUserManagerRole = appRoleRepository.findByName("Firm User Manager");
                firmUserManagerRole.ifPresent(appRoles::add);
            }

            Firm firm = mapper.map(firmDto, Firm.class);
            UserProfile userProfile = UserProfile.builder().activeProfile(true)
                    .userType(UserType.EXTERNAL).appRoles(appRoles).createdDate(LocalDateTime.now())
                    .createdBy(createdBy).firm(firm).entraUser(entraUser)
                    .userProfileStatus(UserProfileStatus.PENDING).build();

            entraUser.setUserProfiles(Set.of(userProfile));
        }

        // Audit fields are automatically set by Spring Data JPA auditing
        return entraUserRepository.saveAndFlush(entraUser);
    }

    public UserProfile addMultiFirmUserProfile(EntraUserDto entraUserDto, FirmDto firmDto,
            List<OfficeDto> userOfficeDtos, List<AppRoleDto> appRoleDtos, String createdBy) {
        logger.info("Adding user profile for user: {} ({})", entraUserDto.getFullName(),
                entraUserDto.getId());

        if (!entraUserDto.isMultiFirmUser()) {
            logger.error("User {} {} is not a multi-firm user", entraUserDto.getFirstName(),
                    entraUserDto.getLastName());
            throw new RuntimeException(String.format("User %s %s is not a multi-firm user",
                    entraUserDto.getFirstName(), entraUserDto.getLastName()));
        }

        Set<Office> offices = null;
        if (!(userOfficeDtos == null || userOfficeDtos.isEmpty())) {
            offices = userOfficeDtos.stream().map(userOfficeDto -> officeRepository
                    .findById(userOfficeDto.getId())
                    .orElseThrow(() -> new RuntimeException(
                            String.format("Office not found for: %s", userOfficeDto.getId()))))
                    .collect(Collectors.toSet());
        }

        Firm firm;
        if (!(firmDto == null || firmDto.getId() == null)) {
            firm = firmService.getById(firmDto.getId());
        } else {
            logger.error("Invalid firm details provided for: {}", entraUserDto.getFullName());
            throw new RuntimeException(String.format("Invalid firm details provided for: %s",
                    entraUserDto.getFullName()));
        }

        Set<AppRole> appRoles = null;
        if (!(appRoleDtos == null || appRoleDtos.isEmpty())) {
            appRoles = appRoleDtos.stream().map(appRoleDto -> appRoleRepository
                    .findById(UUID.fromString(appRoleDto.getId()))
                    .orElseThrow(() -> new RuntimeException(
                            String.format("App role not found for: %s", appRoleDto.getId()))))
                    .collect(Collectors.toSet());
        }

        EntraUser entraUser = entraUserRepository.findById(UUID.fromString(entraUserDto.getId()))
                .orElseThrow(() -> {
                    logger.error("User not found for the given user user id: {}",
                            entraUserDto.getId());
                    return new RuntimeException(String.format(
                            "User not found for the given user user id: %s", entraUserDto.getId()));
                });

        if (entraUser.getUserProfiles() != null) {
            boolean firmAlreadyAssigned = entraUser.getUserProfiles().stream()
                    .anyMatch(profile -> profile.getFirm() != null
                            && profile.getFirm().getId().equals(firmDto.getId()));

            if (firmAlreadyAssigned) {
                logger.error("The user is already got a profile for the firm: {}", firmDto);
                throw new RuntimeException(
                        String.format("User profile already exists for this firm %s", firmDto));
            }
        }

        boolean activeProfile = entraUser.getUserProfiles() == null || entraUser.getUserProfiles().isEmpty();

        UserProfile userProfile = UserProfile.builder().entraUser(entraUser)
                .activeProfile(activeProfile).userType(UserType.EXTERNAL)
                .createdDate(LocalDateTime.now()).createdBy(createdBy).appRoles(appRoles).firm(firm)
                .offices(offices).userProfileStatus(UserProfileStatus.COMPLETE).build();

        if (entraUser.getUserProfiles() == null) {
            entraUser.setUserProfiles(Set.of(userProfile));
        } else {
            entraUser.getUserProfiles().add(userProfile);
        }

        userProfile = userProfileRepository.save(userProfile); //save to generate legacy user id for ccms sync
        Set<String> newPuiRoles = appRoles != null ? filterByPuiRoles(appRoles) : Collections.emptySet();

        // Try to send role change notification with retry logic before saving
        boolean notificationSuccess = roleChangeNotificationService.sendMessage(userProfile,
                newPuiRoles, Collections.emptySet());
        userProfile.setLastCcmsSyncSuccessful(notificationSuccess);

        // Save user profile with ccms sync status
        userProfileRepository.save(userProfile);
        entraUserRepository.save(entraUser);

        techServicesClient.updateRoleAssignment(entraUser.getId());

        notificationService.notifyDeleteFirmAccess(userProfile.getId(), entraUserDto.getFirstName(),
                entraUserDto.getEmail(), firmDto.getName());

        logger.info("User profile added successfully for user: {} ({})", entraUserDto.getFullName(),
                entraUserDto.getId());

        return userProfile;
    }

    public List<AppRoleDto> getUserAppRolesByUserId(String userId) {
        Optional<UserProfileDto> optionalUserProfile = getUserProfileById(userId);
        if (optionalUserProfile.isPresent()) {
            UserProfileDto userProfile = optionalUserProfile.get();
            return userProfile.getAppRoles();
        }
        return Collections.emptyList();
    }

    public List<AppRoleDto> getAllAvailableRoles() {
        return appRoleRepository.findAll().stream()
                .map(appRole -> mapper.map(appRole, AppRoleDto.class)).collect(Collectors.toList());
    }

    /**
     * The method loads the user type from DB and map it as user authorities
     *
     * @param entraId the user entra id
     * @return the list of user types associated with entra user
     */
    public List<String> getUserAuthorities(String entraId) {
        EntraUser user = entraUserRepository.findByEntraOid(entraId).orElseThrow(() -> {
            logger.error("User not found for the given entra id: {}", entraId);
            return new RuntimeException(
                    String.format("User not found for the given entra id: %s", entraId));
        });

        List<String> grantedAuthorities = Collections.emptyList();

        if (user != null && user.getUserStatus() == UserStatus.ACTIVE) {
            grantedAuthorities = user.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .flatMap(userProfile -> userProfile.getAppRoles().stream())
                    .filter(AppRole::isAuthzRole)
                    .flatMap(appRole -> appRole.getPermissions().stream()).map(Enum::name).toList();
        }
        return grantedAuthorities;
    }

    public Set<AppDto> getUserAppsByUserId(String userId) {
        Optional<UserProfileDto> optionalUserProfile = getUserProfileById(userId);
        if (optionalUserProfile.isPresent()) {
            UserProfileDto userProfile = optionalUserProfile.get();
            return userProfile.getAppRoles().stream().map(AppRoleDto::getApp)
                    .collect(Collectors.toSet());
        } else {
            logger.warn("No user profile found for user id {} when getting user apps", userId);
            return Collections.emptySet();
        }
    }

    public List<AppRoleDto> getAppRolesByAppIds(List<String> appIds) {
        List<UUID> appUuids = appIds.stream().map(UUID::fromString).toList();
        return appRepository.findAllById(appUuids).stream()
                .flatMap(app -> app.getAppRoles().stream())
                .map(appRole -> mapper.map(appRole, AppRoleDto.class)).toList();
    }

    public EntraUser getUserByEntraId(UUID userId) {
        Optional<EntraUser> optionalUser = entraUserRepository.findByEntraOid(userId.toString());
        return optionalUser.orElse(null);
    }

    public boolean userExistsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        // Check if the user exists in the local repository
        Optional<EntraUser> user = entraUserRepository.findByEmailIgnoreCase(email);

        // Check if the user exists in Entra
        User graphUser = null;
        try {
            graphUser = graphClient.users().byUserId(email).get();
        } catch (Exception ex) {
            logger.debug(
                    "No user found in Entra with matching email. Catching error and moving on.");
        }
        return user.isPresent() || graphUser != null;
    }

    public boolean isMultiFirmUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        // Check if the user exists in the local repository and is a multi-firm user
        Optional<EntraUser> user = entraUserRepository.findByEmailIgnoreCase(email);
        return user.map(EntraUser::isMultiFirmUser).orElse(false);
    }

    public Optional<EntraUser> findEntraUserByEmail(String email) {
        return entraUserRepository.findByEmailIgnoreCase(email);
    }

    public boolean hasUserFirmAlreadyAssigned(String email, UUID firmId) {
        Optional<EntraUser> entraUserOptional = entraUserRepository.findByEmailIgnoreCase(email);
        if (entraUserOptional.isPresent()) {
            EntraUser entraUser = entraUserOptional.get();
            return entraUser.getUserProfiles().stream()
                    .anyMatch(profile -> profile.getFirm() != null
                            && profile.getFirm().getId().equals(firmId));
        }

        return false;
    }

    public List<AppRoleDto> getAppRolesByAppId(String appId) {
        UUID appUuid = UUID.fromString(appId);
        Optional<App> optionalApp = appRepository.findById(appUuid);
        List<AppRoleDto> appRoles = new ArrayList<>();
        if (optionalApp.isPresent()) {
            App app = optionalApp.get();
            appRoles = app.getAppRoles().stream()
                    .map(appRole -> mapper.map(appRole, AppRoleDto.class)).toList();
        }
        return appRoles;
    }

    public List<AppRoleDto> getAppRolesByAppIdAndUserType(String appId, UserType userType,
            FirmType userFirmType) {
        UUID appUuid = UUID.fromString(appId);
        Optional<App> optionalApp = appRepository.findById(appUuid);
        List<AppRoleDto> appRoles = new ArrayList<>();
        if (optionalApp.isPresent()) {
            App app = optionalApp.get();
            appRoles = app.getAppRoles().stream()
                    .filter(appRole -> Arrays.stream(appRole.getUserTypeRestriction())
                            .anyMatch(roleUserType -> roleUserType == userType))
                    .filter(appRole -> appRole.getFirmTypeRestriction() == null
                            || Arrays.stream(appRole.getFirmTypeRestriction())
                            .anyMatch(roleFirmType -> roleFirmType == userFirmType))
                    .map(appRole -> mapper.map(appRole, AppRoleDto.class)).sorted().toList();
        }
        return appRoles;
    }

    public Optional<AppDto> getAppByAppId(String appId) {
        Optional<App> optionalApp = appRepository.findById(UUID.fromString(appId));
        if (optionalApp.isPresent()) {
            App app = optionalApp.get();
            return Optional.of(mapper.map(app, AppDto.class));
        } else {
            return Optional.empty();
        }
    }

    public Set<LaaApplicationForView> getUserAssignedAppsforLandingPage(String id) {
        Optional<UserProfileDto> userProfile = getActiveProfileByUserId(id);

        if (userProfile.isEmpty()) {
            logger.error("Active user profile not found for user: {}", id);
            throw new RuntimeException(
                    String.format("User profile not found for the given user id: %s", id));
        }

        Set<AppDto> userApps = getUserAppsByUserId(String.valueOf(userProfile.get().getId()));

        return getUserAssignedApps(userApps);
    }

    /**
     * Update user details in Microsoft Graph and local database
     *
     * @param userId    The user ID
     * @param firstName The user's first name
     * @param lastName  The user's last name
     * @throws IOException If an error occurs during the update
     */
    public void updateUserDetails(String userId, String firstName, String lastName)
            throws IOException {
        // Update local database
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            EntraUser entraUser = optionalUser.get();
            entraUser.setFirstName(firstName);
            entraUser.setLastName(lastName);

            try {
                entraUserRepository.saveAndFlush(entraUser);
                logger.info("Successfully updated user details in database for user ID: {}",
                        userId);
            } catch (Exception e) {
                logger.error("Failed to update user details in database for user ID: {}", userId,
                        e);
                throw new IOException("Failed to update user details in database", e);
            }
        } else {
            logger.warn(
                    "User with id {} not found in database. Could not update local user details.",
                    userId);
        }
    }

    /**
     * Convert a single-firm user to a multi-firm user This operation is
     * irreversible
     *
     * @param userId The ID of the user to convert
     * @throws RuntimeException if the user is not found or is already a multi-firm
     *                          user
     */
    @Transactional
    public void convertToMultiFirmUser(String userId) {
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isEmpty()) {
            logger.error(
                    "User with id {} not found in database. Cannot convert to multi-firm user.",
                    userId);
            throw new RuntimeException("User not found");
        }

        EntraUser entraUser = optionalUser.get();

        if (entraUser.isMultiFirmUser()) {
            logger.warn("User with id {} is already a multi-firm user.", userId);
            throw new RuntimeException("User is already a multi-firm user");
        }

        // Set the multi-firm flag
        entraUser.setMultiFirmUser(true);

        try {
            entraUserRepository.saveAndFlush(entraUser);
            logger.info("Successfully converted user {} to multi-firm status", userId);
        } catch (Exception e) {
            logger.error("Failed to convert user {} to multi-firm status", userId, e);
            throw new RuntimeException("Failed to convert user to multi-firm status", e);
        }
    }

    private Set<LaaApplicationForView> getUserAssignedApps(Set<AppDto> userApps) {
        List<LaaApplication> applications = laaApplicationsList.getApplications();
        Set<LaaApplicationForView> userAssignedApps = applications.stream()
                .filter(app -> userApps.stream().map(AppDto::getName)
                        .anyMatch(appName -> appName.equals(app.getName())))
                .map(LaaApplicationForView::new)
                .sorted(Comparator.comparingInt(LaaApplicationForView::getOrdinal))
                .collect(Collectors.toCollection(TreeSet::new));

        // Make any necessary adjustments to the app display properties
        makeAppDisplayAdjustments(userAssignedApps);

        return userAssignedApps;
    }

    private void makeAppDisplayAdjustments(Set<LaaApplicationForView> userApps) {
        List<LaaApplication> applications = laaApplicationsList.getApplications();

        Set<String> userAppNames = userApps.stream().map(LaaApplicationForView::getName).collect(Collectors.toSet());

        userApps.forEach(app -> {
            Optional<LaaApplication> matchingApp = applications.stream()
                    .filter(configApp -> configApp.getName().equals(app.getName())).findFirst();

            matchingApp.ifPresent(configApp -> {
                if (configApp.getDescriptionIfAppAssigned() != null
                        && StringUtils.isNotEmpty(
                                configApp.getDescriptionIfAppAssigned().getAppAssigned())
                        && StringUtils.isNotEmpty(
                                configApp.getDescriptionIfAppAssigned().getDescription())
                        && userAppNames.contains(
                                configApp.getDescriptionIfAppAssigned().getAppAssigned())) {
                    app.setDescription(configApp.getDescriptionIfAppAssigned().getDescription());
                }
            });
        });
    }

    /**
     * Get offices assigned to a user
     *
     * @param userId The user ID
     * @return List of offices assigned to the user
     */
    public List<OfficeDto> getUserOfficesByUserId(String userId) {
        Optional<UserProfileDto> optionalUserProfile = getUserProfileById(userId);
        if (optionalUserProfile.isPresent()) {
            UserProfileDto userProfile = optionalUserProfile.get();
            return userProfile.getOffices();
        }
        return Collections.emptyList();
    }

    /**
     * Update user offices
     *
     * @param userId          The user ID
     * @param selectedOffices List of selected office IDs
     * @throws IOException If an error occurs during the update
     */
    public String updateUserOffices(String userId, List<String> selectedOffices)
            throws IOException {
        Optional<UserProfile> optionalUserProfile = userProfileRepository.findById(UUID.fromString(userId));
        String diff;
        if (optionalUserProfile.isPresent()) {
            UserProfile userProfile = optionalUserProfile.get();
            if (selectedOffices.contains("ALL")) {
                diff = diffOffices(userProfile.getOffices(), null, true);
                userProfile.setOffices(null);
                userProfile.setUnrestrictedOfficeAccess(true);
            } else if (selectedOffices.contains("NO_OFFICES")) {
                diff = diffOffices(userProfile.getOffices(), null, false);
                userProfile.setOffices(null);
                userProfile.setUnrestrictedOfficeAccess(false);
            } else {
                List<UUID> officeIds = selectedOffices.stream().map(UUID::fromString).collect(Collectors.toList());
                Set<Office> offices = validateOfficesByUserFirm(userProfile, officeIds);
                diff = diffOffices(userProfile.getOffices(), offices, null);
                // Update user profile offices
                userProfile.setOffices(offices);
                userProfile.setUnrestrictedOfficeAccess(false);
            }
            userProfileRepository.saveAndFlush(userProfile);
            logger.info("Successfully updated user offices for user ID: {}", userId);
        } else {
            logger.warn("User profile with id {} not found. Could not update offices.", userId);
            throw new IOException("User profile not found for user ID: " + userId);
        }
        return diff;
    }

    protected String diffOffices(Set<Office> oldOffices, Set<Office> newOffices, Boolean isUnrestrictedAccess) {
        String removed = "";
        String added = "";
        if (Objects.isNull(oldOffices) || oldOffices.isEmpty()) {
            removed = String.format("Removed : Unrestricted access %s", isUnrestrictedAccess);
            if (!Objects.isNull(newOffices)) {
                added = "Added : " + newOffices.stream().map(Office::getCode)
                        .collect(Collectors.joining(", "));
            }
        }
        if (Objects.isNull(newOffices) || newOffices.isEmpty()) {
            added = String.format("Added : Unrestricted access %s", isUnrestrictedAccess);
            if (!Objects.isNull(oldOffices)) {
                removed = "Removed : " + oldOffices.stream().map(Office::getCode)
                        .collect(Collectors.joining(", "));
            }
        }
        if (Objects.nonNull(oldOffices) && !oldOffices.isEmpty() && Objects.nonNull(newOffices)
                && !newOffices.isEmpty()) {
            List<UUID> oldIds = oldOffices.stream().map(Office::getId).toList();
            List<UUID> newIds = newOffices.stream().map(Office::getId).toList();
            removed = "Removed : "
                    + oldOffices.stream().filter(role -> !newIds.contains(role.getId()))
                            .map(Office::getCode).collect(Collectors.joining(", "));
            added = "Added : " + newOffices.stream().filter(role -> !oldIds.contains(role.getId()))
                    .map(Office::getCode).collect(Collectors.joining(", "));
        }

        return removed + ", " + added;
    }

    private Set<Office> validateOfficesByUserFirm(UserProfile userProfile,
            Iterable<UUID> officeIds) {
        Set<Office> offices = new HashSet<>(officeRepository.findAllById(officeIds));
        // Only allow offices that associated with the same firm as the user.
        UUID userFirmId = userProfile.getFirm() != null ? userProfile.getFirm().getId() : null;
        Set<Office> validOffices = offices.stream().filter(office -> office.getFirm() != null)
                .filter(office -> office.getFirm().getId().equals(userFirmId))
                .collect(Collectors.toSet());
        Set<Office> invalidOffices = new HashSet<>(offices);
        invalidOffices.removeAll(validOffices);
        if (!invalidOffices.isEmpty()) {
            String invalidOfficeIds = invalidOffices.stream()
                    .map(office -> office.getId().toString()).collect(Collectors.joining(","));
            logger.warn(
                    "There was an attempt to assign user with profile id \"{}\" the following offices not associated with their firm: {}",
                    userProfile.getId().toString(), invalidOfficeIds);
        }
        return validOffices;
    }

    public boolean isInternal(String userId) {
        Optional<UserType> userType = getUserTypeByUserId(userId);
        return userType.isPresent() && userType.get() == UserType.INTERNAL;
    }

    public boolean isInternal(UUID userId) {
        return isInternal(userId.toString());
    }

    public boolean isAccessGranted(String userId) {
        // Get user profile by userId
        Optional<UserProfile> optionalUser = userProfileRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            UserProfile user = optionalUser.get();
            // Check if the user has access granted
            return user.getUserProfileStatus() == UserProfileStatus.COMPLETE;

        } else {
            logger.warn("User with id {} not found. Could not check access.", userId);
            return false;
        }
    }

    /**
     * Grant access to a user by updating their profile status to COMPLETE
     *
     * @param userId          The user profile ID
     * @param currentUserName The name of the user granting access
     * @return true if access was granted successfully, false otherwise
     */
    public boolean grantAccess(String userId, String currentUserName) {
        Optional<UserProfile> optionalUser = userProfileRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            UserProfile user = optionalUser.get();
            user.setUserProfileStatus(UserProfileStatus.COMPLETE);
            user.setLastModifiedBy(currentUserName);
            user.setLastModified(LocalDateTime.now());
            userProfileRepository.saveAndFlush(user);
            logger.info("Access granted for user profile ID: {} by {}", userId, currentUserName);
            return true;
        } else {
            logger.warn("User profile with id {} not found. Could not grant access.", userId);
            return false;
        }
    }

    public void setDefaultActiveProfile(EntraUser entraUser, UUID firmId) throws IOException {
        UserProfile active = null;
        for (UserProfile userProfile : entraUser.getUserProfiles()) {
            if (userProfile.getFirm().getId().equals(firmId)) {
                active = userProfile;
            } else {
                userProfile.setActiveProfile(false);
            }
        }
        if (Objects.isNull(active)) {
            logger.warn("Firm with id {} not found in user profile. Could not update profile.",
                    firmId);
            throw new IOException("Firm not found for firm ID: " + firmId);
        }
        entraUserRepository.saveAndFlush(entraUser);
        active.setActiveProfile(true);
        entraUserRepository.saveAndFlush(entraUser);
    }

    public int createInternalPolledUser(List<EntraUserDto> entraUserDtos) {
        List<EntraUser> entraUsers = new ArrayList<>();
        String createdBy = "INTERNAL_USER_SYNC";
        for (EntraUserDto user : entraUserDtos) {
            EntraUser entraUser = mapper.map(user, EntraUser.class);
            UserProfile userProfile = UserProfile.builder().activeProfile(true)
                    .userProfileStatus(UserProfileStatus.COMPLETE).userType(UserType.INTERNAL)
                    .createdDate(LocalDateTime.now()).createdBy(createdBy).entraUser(entraUser)
                    .build();

            entraUser.setEntraOid(user.getEntraOid());
            entraUser.setUserProfiles(Set.of(userProfile));
            entraUser.setUserStatus(UserStatus.ACTIVE);
            entraUser.setCreatedBy(createdBy);
            entraUser.setCreatedDate(LocalDateTime.now());
            entraUsers.add(entraUser);
            // todo: security group to access authz app
        }
        return persistNewInternalUser(entraUsers);
    }

    private int persistNewInternalUser(List<EntraUser> newUsers) {
        int usersPersisted = 0;
        for (EntraUser newUser : newUsers) {
            try {
                logger.info("Adding new internal user id: {} name: {} {}", newUser.getEntraOid(),
                        newUser.getFirstName(), newUser.getLastName());
                entraUserRepository.saveAndFlush(newUser);
                usersPersisted++;
                logger.info("User {} added", newUser.getEntraOid());
            } catch (Exception e) {
                logger.error("Unexpected error when adding user id: {} name: {} {} {}",
                        newUser.getEntraOid(), newUser.getFirstName(), newUser.getLastName(),
                        e.getMessage());
            }
        }
        return usersPersisted;
    }

    public Set<Permission> getUserPermissionsByUserId(String userId) {
        return getUserPermissionsByUserId(UUID.fromString(userId));
    }

    public Set<Permission> getUserPermissionsByUserId(UUID userId) {
        Optional<EntraUser> optionalEntraUser = entraUserRepository.findById(userId);
        if (optionalEntraUser.isPresent()) {
            EntraUser entraUser = optionalEntraUser.get();
            return entraUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile)
                    .flatMap(userProfile -> userProfile.getAppRoles().stream())
                    .filter(AppRole::isAuthzRole)
                    .flatMap(appRole -> appRole.getPermissions().stream())
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public List<UUID> getInternalUserEntraIds() {
        return userProfileRepository.findByUserTypes(UserType.INTERNAL);
    }

    /**
     * Remove a specific app role from a user
     */
    public void removeUserAppRole(String userProfileId, String appId, String roleName) {
        Optional<UserProfile> optionalUserProfile = userProfileRepository.findById(UUID.fromString(userProfileId));
        if (optionalUserProfile.isPresent()) {
            UserProfile userProfile = optionalUserProfile.get();
            Set<AppRole> currentRoles = new HashSet<>(userProfile.getAppRoles());

            // Find and remove the specific role
            boolean removed = currentRoles.removeIf(role -> role.getApp().getId().toString().equals(appId)
                    && role.getName().equals(roleName));

            if (removed) {
                userProfile.setAppRoles(currentRoles);
                userProfileRepository.saveAndFlush(userProfile);
                logger.info("Removed app role '{}' from app '{}' for user '{}'", roleName, appId,
                        userProfileId);
            } else {
                logger.warn("App role '{}' from app '{}' not found for user '{}'", roleName, appId,
                        userProfileId);
            }
        } else {
            logger.warn("User profile with id {} not found. Could not remove app role.",
                    userProfileId);
        }
    }

    public Map<String, AppRoleDto> getRolesByIdIn(Collection<UUID> roleIds) {
        return appRoleRepository.findAllByIdIn(roleIds).stream()
                .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                .collect(Collectors.toMap(AppRoleDto::getId, Function.identity()));
    }

    /**
     * Get user profiles by Entra user ID
     *
     * @param entraUserId The Entra user ID
     * @return List of user profiles for the Entra user
     */
    public List<UserProfile> getUserProfilesByEntraUserId(UUID entraUserId) {
        Optional<EntraUser> optionalEntraUser = entraUserRepository.findById(entraUserId);
        if (optionalEntraUser.isPresent()) {
            EntraUser entraUser = optionalEntraUser.get();
            return entraUser.getUserProfiles().stream().toList();
        }
        return Collections.emptyList();
    }

    /**
     * Get user profiles by Entra user ID with optional search filter
     *
     * @param entraUserId The Entra user ID
     * @param search      Optional search term to filter by firm name or code
     * @return List of user profiles matching the search criteria
     */
    public List<UserProfile> getUserProfilesByEntraUserIdAndSearch(UUID entraUserId,
            String search) {
        return userProfileRepository.findByEntraUserIdAndFirmSearch(entraUserId, search);
    }

    /**
     * Get count of user profiles by Entra user ID
     *
     * @param entraUserId The Entra user ID
     * @return Count of user profiles for the Entra user
     */
    public long getProfileCountByEntraUserId(UUID entraUserId) {
        return userProfileRepository.countByEntraUserId(entraUserId);
    }

    /**
     * Get paginated audit users for the User Access Audit Table Includes all
     * registered users, even
     * those without firm profiles
     *
     * @param searchTerm Search by name or email
     * @param firmId     Filter by firm ID
     * @param silasRole  Filter by SiLAS role (authz role name)
     * @param page       Page number (1-based)
     * @param pageSize   Number of results per page
     * @param sort       Sort field
     * @param direction  Sort direction (asc/desc)
     * @return Paginated audit users
     */
    public PaginatedAuditUsers getAuditUsers(
            String searchTerm, UUID firmId, String silasRole, UUID appId, UserTypeForm userTypeForm,
            int page, int pageSize, String sort, String direction) {
        Boolean multiFirm = userTypeForm == null ? null : userTypeForm.getMultiFirm();
        UserType userType = userTypeForm == null ? null : userTypeForm.getUserType();
        String userTypeStr = userType == null ? null : userType.name();

        // Check if sorting by profile count, firm, or account status (special cases -
        // require different queries)
        boolean sortByProfileCount = sort != null && sort.equalsIgnoreCase("profilecount");
        boolean sortByFirm = sort != null
                && (sort.equalsIgnoreCase("firm") || sort.equalsIgnoreCase("firmassociation"));
        boolean sortByAccountStatus = sort != null && sort.equalsIgnoreCase("accountstatus");

        Page<EntraUser> userPage;

        if (sortByProfileCount || sortByFirm || sortByAccountStatus) {
            // Use special queries for profile count, firm, or account status sorting
            boolean ascending = direction == null || direction.equalsIgnoreCase("asc");
            String sortField;
            if (sortByProfileCount) {
                sortField = "profileCount";
            } else if (sortByFirm) {
                sortField = "firmName";
            } else {
                sortField = "accountStatus";
            }

            Sort sortObj = ascending ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
            PageRequest pageRequest = PageRequest.of(page - 1, pageSize, sortObj);
            // UserType must be treated as a string because we are using native queries
            // here.

            Page<? extends UserAuditProjection> resultPage;
            if (sortByProfileCount) {
                resultPage = entraUserRepository.findAllUsersForAuditWithProfileCount(
                        searchTerm, firmId, silasRole, appId, userTypeStr, multiFirm, pageRequest);
            } else if (sortByFirm) {
                resultPage = entraUserRepository.findAllUsersForAuditWithFirm(
                        searchTerm, firmId, silasRole, appId, userTypeStr, multiFirm, pageRequest);
            } else {
                resultPage = entraUserRepository.findAllUsersForAuditWithAccountStatus(
                        searchTerm, firmId, silasRole, appId, userTypeStr, multiFirm, pageRequest);
            }

            // Extract user IDs in order
            List<UUID> userIds = resultPage.getContent().stream().map(UserAuditProjection::getUserId).toList();

            // Fetch full user details
            List<EntraUser> users = Collections.emptyList();
            if (!userIds.isEmpty()) {
                List<EntraUser> fetchedUsers = entraUserRepository
                        .findUsersWithProfilesAndRoles(new LinkedHashSet<>(userIds));

                // Sort users to match the order from the query result
                Map<UUID, Integer> orderMap = new HashMap<>();
                for (int i = 0; i < userIds.size(); i++) {
                    orderMap.put(userIds.get(i), i);
                }
                users = new ArrayList<>(fetchedUsers);
                users.sort(Comparator
                        .comparingInt(u -> orderMap.getOrDefault(u.getId(), Integer.MAX_VALUE)));
            }

            // Create page with sorted users
            userPage = new PageImpl<>(users, pageRequest, resultPage.getTotalElements());
        } else {
            // Map sort field to entity field
            String mappedSort = mapAuditSortField(sort != null ? sort : "name");
            Sort sortObj = getAuditSort(mappedSort, direction);
            PageRequest pageRequest = PageRequest.of(page - 1, pageSize, sortObj);

            userPage = entraUserRepository.findAllUsersForAudit(
                    searchTerm, firmId, silasRole, appId, userType, multiFirm, pageRequest);

            // Second query: Batch fetch relationships for the paginated users
            if (!userPage.getContent().isEmpty()) {
                Set<UUID> userIds = userPage.getContent().stream().map(EntraUser::getId)
                        .collect(Collectors.toSet());
                List<EntraUser> usersWithRelations = entraUserRepository.findUsersWithProfilesAndRoles(userIds);

                // Replace content with fully loaded entities, preserving order
                Map<UUID, EntraUser> userMap = usersWithRelations.stream()
                        .collect(Collectors.toMap(EntraUser::getId, u -> u));
                List<EntraUser> orderedUsers = userPage.getContent().stream()
                        .map(u -> userMap.getOrDefault(u.getId(), u)).toList();
                userPage = new PageImpl<>(orderedUsers, userPage.getPageable(),
                        userPage.getTotalElements());
            }
        }

        // Map to DTOs
        List<AuditUserDto> auditUsers = userPage.getContent().stream().map(this::mapToAuditUserDto).toList();

        return PaginatedAuditUsers.builder().users(auditUsers)
                .totalUsers(userPage.getTotalElements()).totalPages(userPage.getTotalPages())
                .currentPage(page).pageSize(pageSize).build();
    }

    /**
     * Map EntraUser to AuditUserDto
     */
    private AuditUserDto mapToAuditUserDto(EntraUser user) {
        // Get all user profiles
        List<UserProfile> profiles = user.getUserProfiles() != null ? new ArrayList<>(user.getUserProfiles())
                : Collections.emptyList();

        // Determine user type
        String userType = determineUserType(user, profiles);

        // Get firm associations
        String firmAssociation = determineFirmAssociation(profiles);

        // Get account status
        String accountStatus = determineAccountStatus(user, profiles);

        // Get profile count
        int profileCount = profiles.size();

        // Get first active profile ID for linking, or first profile if none active
        String userId = profiles.stream().filter(UserProfile::isActiveProfile).findFirst()
                .or(() -> profiles.stream().findFirst()).map(profile -> profile.getId().toString())
                .orElse(null);

        return AuditUserDto.builder().name(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail()).userId(userId).entraUserId(user.getId().toString())
                .userType(userType).firmAssociation(firmAssociation).accountStatus(accountStatus)
                .isMultiFirmUser(user.isMultiFirmUser()).profileCount(profileCount)
                .createdDate(user.getCreatedDate()).createdBy(user.getCreatedBy())
                // TODO: Fetch lastLoginDate from Microsoft Graph or Silas API
                .lastLoginDate(null)
                .entraStatus(user.getUserStatus() != null ? user.getUserStatus().name() : "UNKNOWN")
                // TODO: Fetch activationStatus from TechServices API
                .activationStatus(null).build();
    }

    /**
     * Determine user type for audit table Returns: "Internal", "External", or
     * "External - 3rd
     * Party"
     */
    private String determineUserType(EntraUser user, List<UserProfile> profiles) {
        if (profiles.isEmpty()) {
            // If multi-firm user with no profiles, show as External - 3rd Party
            if (user.isMultiFirmUser()) {
                return "External - 3rd Party";
            }
            if (!user.isMultiFirmUser()) {
                return "External";
            }
            return "Unknown";
        }

        // Check if user has internal profile
        boolean hasInternal = profiles.stream().anyMatch(profile -> profile.getUserType() == UserType.INTERNAL);

        if (hasInternal) {
            return "Internal";
        }

        // External user
        if (user.isMultiFirmUser()) {
            return "External - 3rd Party";
        }

        return "External";
    }

    /**
     * Determine firm association for audit table Returns firm name(s) or "None" if
     * no profiles
     */
    private String determineFirmAssociation(List<UserProfile> profiles) {
        if (profiles.isEmpty()) {
            return "Unknown";
        }

        Set<String> firmNames = profiles.stream().map(UserProfile::getFirm).filter(Objects::nonNull)
                .map(Firm::getName).collect(Collectors.toCollection(TreeSet::new));

        if (firmNames.isEmpty()) {
            return "Unknown";
        }

        return String.join(", ", firmNames);
    }

    /**
     * Determine account status for audit table Returns: "Active", "Inactive",
     * "Pending", or
     * "Disabled"
     */
    private String determineAccountStatus(EntraUser user, List<UserProfile> profiles) {
        // Check if user has any pending profiles
        boolean hasPending = profiles.stream()
                .anyMatch(profile -> profile.getUserProfileStatus() == UserProfileStatus.PENDING);

        if (hasPending) {
            return "Pending";
        }

        // Check user status
        if (user.getUserStatus() == UserStatus.DEACTIVE) {
            return "Disabled";
        }

        // All other cases considered active
        return "Active";
    }

    /**
     * Get all SiLAS roles (authz roles) for dropdown filter Returns list of role
     * DTOs
     */
    public List<AppRoleDto> getAllSilasRoles() {
        return appRoleRepository.findAllAuthzRoles().stream()
                .map(role -> mapper.map(role, AppRoleDto.class)).toList();
    }

    /**
     * Map audit table sort field to entity field
     */
    private String mapAuditSortField(String sort) {
        if (sort == null) {
            return "firstName";
        }

        return switch (sort.toLowerCase()) {
            case "name" -> "firstName"; // Sort by first name for name column
            case "email" -> "email";
            // Sort by multiFirmUser for user type (ex 3rd party sorts differently)
            case "usertype" -> "multiFirmUser";
            // Sort by firm name (uses LEFT JOIN with firm)
            case "firm", "firmassociation" -> "f.name";
            case "accountstatus" -> "userStatus"; // Sort by userStatus enum
            case "ismultifirmuser" -> "multiFirmUser"; // Sort by multiFirmUser boolean
            case "profilecount" -> "profilecount"; // Special case - handled separately
            default -> "firstName"; // Default to first name
        };
    }

    /**
     * Get Sort for audit table queries
     */
    private Sort getAuditSort(String field, String direction) {
        Sort.Direction order = Sort.Direction.ASC;
        if (direction != null && !direction.isEmpty()) {
            order = Sort.Direction.valueOf(direction.toUpperCase());
        }
        return Sort.by(order, field);
    }

    /**
     * Get detailed audit information for a specific user Includes all profiles,
     * roles, offices, and
     * audit data
     *
     * @param userId The user profile ID to retrieve
     * @return Detailed audit user DTO
     */
    public AuditUserDetailDto getAuditUserDetail(UUID userId) {
        // Fetch user profile with all relationships
        UserProfile profile = userProfileRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("User profile not found with id: " + userId));

        EntraUser entraUser = profile.getEntraUser();

        // Get all profiles for this user
        List<UserProfile> allProfiles = new ArrayList<>(entraUser.getUserProfiles());

        // Sort with active profile first
        allProfiles.sort((p1, p2) -> Boolean.compare(p2.isActiveProfile(), p1.isActiveProfile()));

        // Map profiles to DTOs (no pagination for this method)
        List<AuditProfileDto> profileDtos = allProfiles.stream().map(this::mapToAuditProfileDto).toList();

        // Determine user type using shared method
        String userType = determineUserType(entraUser, allProfiles);

        // Build detail DTO
        return AuditUserDetailDto.builder().userId(entraUser.getId().toString())
                .email(entraUser.getEmail()).firstName(entraUser.getFirstName())
                .lastName(entraUser.getLastName())
                .fullName(entraUser.getFirstName() + " " + entraUser.getLastName())
                .isMultiFirmUser(entraUser.isMultiFirmUser()).userType(userType)
                .createdDate(entraUser.getCreatedDate()).createdBy(entraUser.getCreatedBy())
                // TODO: Fetch lastLoginDate from Microsoft Graph API
                .lastLoginDate(null)
                // TODO: Fetch activationStatus from TechServices API or SILAS API
                .activationStatus(null)
                .entraStatus(entraUser.getUserStatus() != null ? entraUser.getUserStatus().name()
                        : "UNKNOWN")
                .profiles(profileDtos).totalProfiles(allProfiles.size()).totalProfilePages(1)
                .currentProfilePage(1).build();
    }

    /**
     * Get detailed audit information for a specific user with profile pagination
     * Includes all
     * profiles, roles, offices, and audit data
     *
     * @param userId      The user profile ID to retrieve
     * @param profilePage The page number for profiles (1-indexed)
     * @param profileSize The number of profiles per page
     * @return Detailed audit user DTO with paginated profiles
     */
    public AuditUserDetailDto getAuditUserDetail(UUID userId, int profilePage, int profileSize) {
        // Fetch user profile with all relationships
        UserProfile profile = userProfileRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("User profile not found with id: " + userId));

        EntraUser entraUser = profile.getEntraUser();

        // Get all profiles for this user
        List<UserProfile> allProfiles = new ArrayList<>(entraUser.getUserProfiles());

        // Sort with active profile first
        allProfiles.sort((p1, p2) -> Boolean.compare(p2.isActiveProfile(), p1.isActiveProfile()));

        // Calculate pagination
        int totalProfiles = allProfiles.size();
        int totalPages = (int) Math.ceil((double) totalProfiles / profileSize);
        int startIndex = (profilePage - 1) * profileSize;
        int endIndex = Math.min(startIndex + profileSize, totalProfiles);

        // Get paginated profiles
        List<UserProfile> paginatedProfiles = allProfiles.subList(Math.max(0, startIndex), Math.max(0, endIndex));

        // Map profiles to DTOs
        List<AuditProfileDto> profileDtos = paginatedProfiles.stream().map(this::mapToAuditProfileDto).toList();

        // Determine user type using shared method
        String userType = determineUserType(entraUser, allProfiles);

        // Build detail DTO
        return AuditUserDetailDto.builder().userId(entraUser.getId().toString())
                .email(entraUser.getEmail()).firstName(entraUser.getFirstName())
                .lastName(entraUser.getLastName())
                .fullName(entraUser.getFirstName() + " " + entraUser.getLastName())
                .isMultiFirmUser(entraUser.isMultiFirmUser()).userType(userType)
                .createdDate(entraUser.getCreatedDate()).createdBy(entraUser.getCreatedBy())
                // TODO: Fetch lastLoginDate from Microsoft Graph API
                .lastLoginDate(null)
                // TODO: Fetch activationStatus from TechServices API or SILAS API
                .activationStatus(null)
                .entraStatus(entraUser.getUserStatus() != null ? entraUser.getUserStatus().name()
                        : "UNKNOWN")
                .profiles(profileDtos).totalProfiles(totalProfiles).totalProfilePages(totalPages)
                .currentProfilePage(profilePage).hasNoProfile(false).build();
    }

    /**
     * Get audit user detail for an EntraUser without any profiles Used for
     * multi-firm users or
     * external users who don't yet have a firm association
     *
     * @param entraUserId The EntraUser ID
     * @return AuditUserDetailDto with Entra data only
     */
    public AuditUserDetailDto getAuditUserDetailByEntraId(UUID entraUserId) {
        EntraUser entraUser = entraUserRepository.findById(entraUserId).orElseThrow(
                () -> new IllegalArgumentException("Entra user not found with id: " + entraUserId));

        // Get all profiles for this user (may be empty)
        List<UserProfile> allProfiles = entraUser.getUserProfiles() != null
                ? new ArrayList<>(entraUser.getUserProfiles())
                : Collections.emptyList();

        // Determine user type using shared method
        String userType = determineUserType(entraUser, allProfiles);

        // Build detail DTO with Entra data only
        return AuditUserDetailDto.builder().userId(entraUser.getId().toString())
                .email(entraUser.getEmail()).firstName(entraUser.getFirstName())
                .lastName(entraUser.getLastName())
                .fullName(entraUser.getFirstName() + " " + entraUser.getLastName())
                .isMultiFirmUser(entraUser.isMultiFirmUser()).userType(userType)
                .createdDate(entraUser.getCreatedDate()).createdBy(entraUser.getCreatedBy())
                // TODO: Fetch lastLoginDate from Microsoft Graph API
                .lastLoginDate(null)
                // TODO: Fetch activationStatus from TechServices API or SILAS API
                .activationStatus(null)
                .entraStatus(entraUser.getUserStatus() != null ? entraUser.getUserStatus().name()
                        : "UNKNOWN")
                .profiles(Collections.emptyList()).totalProfiles(0).totalProfilePages(0)
                .currentProfilePage(1).hasNoProfile(true).build();
    }

    /**
     * Map UserProfile to AuditProfileDto
     */
    private AuditProfileDto mapToAuditProfileDto(UserProfile profile) {
        // Get offices
        List<OfficeDto> officeDtos = new ArrayList<>();
        String officeRestrictions = "Access to All Offices";

        if (profile.getOffices() != null && !profile.getOffices().isEmpty()) {
            officeDtos = profile.getOffices().stream()
                    .map(office -> mapper.map(office, OfficeDto.class)).toList();
            officeRestrictions = officeDtos.size() + " office(s) selected";
        }

        // Get roles
        List<AppRoleDto> roleDtos = new ArrayList<>();
        if (profile.getAppRoles() != null && !profile.getAppRoles().isEmpty()) {
            roleDtos = profile.getAppRoles().stream()
                    .map(role -> mapper.map(role, AppRoleDto.class)).toList();
        }

        // Get firm details
        String firmName = profile.getFirm() != null ? profile.getFirm().getName() : "";
        String firmCode = profile.getFirm() != null ? profile.getFirm().getCode() : "";

        return AuditProfileDto.builder().profileId(profile.getId().toString()).firmName(firmName)
                .firmCode(firmCode).officeRestrictions(officeRestrictions).offices(officeDtos)
                .roles(roleDtos)
                .userType(profile.getUserType() != null ? profile.getUserType().name() : "UNKNOWN")
                .activeProfile(profile.isActiveProfile()).build();
    }
}
