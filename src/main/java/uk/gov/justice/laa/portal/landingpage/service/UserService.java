package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
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

import uk.gov.justice.laa.portal.landingpage.config.LaaAppsConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;

/**
 * userService
 */
@Service
public class UserService {

    private static final int BATCH_SIZE = 20;
    /**
     * The number of pages to load in advance when doing user pagination
     */
    private static final int PAGES_TO_PRELOAD = 5;
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
    Logger logger = LoggerFactory.getLogger(this.getClass());
    @Value("${spring.security.oauth2.client.registration.azure.redirect-uri}")
    private String redirectUri;

    public UserService(@Qualifier("graphServiceClient") GraphServiceClient graphClient,
            EntraUserRepository entraUserRepository,
            AppRepository appRepository, AppRoleRepository appRoleRepository, ModelMapper mapper,
            OfficeRepository officeRepository,
            LaaAppsConfig.LaaApplicationsList laaApplicationsList,
            TechServicesClient techServicesClient, UserProfileRepository userProfileRepository,
            RoleChangeNotificationService roleChangeNotificationService) {
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
    public void updateUserRoles(String userProfileId, List<String> selectedRoles) {
        List<AppRole> roles = appRoleRepository.findAllById(selectedRoles.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList()));
        Optional<UserProfile> optionalUserProfile = userProfileRepository.findById(UUID.fromString(userProfileId));
        if (optionalUserProfile.isPresent()) {
            UserProfile userProfile = optionalUserProfile.get();
            boolean isInternal = UserType.INTERNAL_TYPES.contains(userProfile.getUserType());
            int before = roles.size();
            roles = roles.stream()
                    .filter(appRole -> appRole.isAuthzRole() || (isInternal || appRole.getRoleType().equals(RoleType.EXTERNAL) || appRole.getRoleType().equals(RoleType.INTERNAL_AND_EXTERNAL)))
                    .toList();
            int after = roles.size();
            if (after < before) {
                logger.warn("Attempt to assign internal role user ID {}.", userProfile.getEntraUser().getId());
            }

            Set<AppRole> newRoles = new HashSet<>(roles);

            Set<AppRole> oldPuiRoles = filterByPuiRoles(userProfile.getAppRoles());
            Set<AppRole> newPuiRoles = filterByPuiRoles(newRoles);

            // Update roles
            userProfile.setAppRoles(newRoles);
            
            // Try to send role change notification with retry logic before saving
            boolean notificationSuccess = roleChangeNotificationService.sendMessage(userProfile, newPuiRoles, oldPuiRoles);
            userProfile.setLastCcmsSyncSuccessful(notificationSuccess);
            
            // Save user profile with updated sync status
            userProfileRepository.save(userProfile);
            techServicesClient.updateRoleAssignment(userProfile.getEntraUser().getId());
        } else {
            logger.warn("User profile with id {} not found. Could not update roles.", userProfileId);
        }
    }

    private Set<AppRole> filterByPuiRoles(Set<AppRole> roles) {
        return roles != null && !roles.isEmpty() ? roles.stream()
                .filter(role -> role.getCcmsCode() != null && role.getCcmsCode().contains("CCMS"))
                .filter(AppRole::isLegacySync)
                .collect(Collectors.toSet()) : new HashSet<>();
    }

    public List<DirectoryRole> getDirectoryRolesByUserId(String userId) {
        return Objects.requireNonNull(graphClient.users().byUserId(userId).memberOf().get())
                .getValue()
                .stream()
                .filter(obj -> obj instanceof DirectoryRole)
                .map(obj -> (DirectoryRole) obj)
                .collect(Collectors.toList());
    }

    public Optional<UserProfileDto> getActiveProfileByUserId(String userId) {
        EntraUser user = entraUserRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> {
                    logger.error("User not found for the given user user id: {}", userId);
                    return new RuntimeException(
                            String.format("User not found for the given user user id: %s", userId));
                });

        if (user.getUserProfiles() == null || user.getUserProfiles().isEmpty()) {
            logger.error("User profile not found for the given user id: {}", userId);
            throw new RuntimeException(String.format("User profile not found for the given user id: %s", userId));
        }

        return user.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst()
                .map(userProfile -> mapper.map(userProfile, UserProfileDto.class));
    }

    public Optional<EntraUserDto> getEntraUserById(String userId) {
        return entraUserRepository.findById(UUID.fromString(userId))
                .map(user -> mapper.map(user, EntraUserDto.class));
    }

    public Optional<UserProfileDto> getUserProfileById(String userId) {
        return userProfileRepository.findById(UUID.fromString(userId))
                .map(user -> mapper.map(user, UserProfileDto.class));
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
        return user.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .map(UserProfile::getUserType)
                .findFirst();
    }

    public String formatLastSignInDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ENGLISH);

        return dateTime.format(formatter);
    }

    private PaginatedUsers getPageOfUsers(Supplier<Page<UserProfile>> pageSupplier) {
        Page<UserProfile> userPage = pageSupplier.get();
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setTotalUsers(userPage.getTotalElements());
        paginatedUsers.setUsers(userPage.stream().map(this::mapUserProfileToDto).toList());
        paginatedUsers.setTotalPages(userPage.getTotalPages());
        return paginatedUsers;
    }

    private UserProfileDto mapUserProfileToDto(UserProfile userProfile) {
        UserProfileDto dto = mapper.map(userProfile, UserProfileDto.class);
        // Ensure the nested EntraUser is properly mapped
        if (userProfile.getEntraUser() != null) {
            dto.setEntraUser(mapper.map(userProfile.getEntraUser(), EntraUserDto.class));
        }
        return dto;
    }

    public PaginatedUsers getPageOfUsersByNameOrEmailAndPermissionsAndFirm(String searchTerm, List<Permission> permissions, UUID firmId, int page, int pageSize, String sort, String direction) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page - 1), pageSize, getSort(sort, direction));
        Page<UserProfile> userProfilePage = userProfileRepository.findByNameOrEmailAndPermissionsAndFirm(searchTerm, permissions.isEmpty() ? null : permissions, firmId, pageRequest);
        return getPageOfUsers(() -> userProfilePage);
    }

    protected Sort getSort(String field, String direction) {
        if (Objects.isNull(field) || field.isEmpty()) {
            return Sort.by(Sort.Order.desc("userProfileStatus"), Sort.Order.asc("entraUser.firstName"));
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
            case "USERTYPE" -> Sort.by(order, "userType");
            case "FIRMNAME" -> Sort.by(order, "firm.name");
            default -> throw new IllegalArgumentException("Invalid field: " + field);
        };
    }

    public List<UserType> findUserTypeByUserEntraId(String entraId) {
        EntraUser user = entraUserRepository.findByEntraOid(entraId)
                .orElseThrow(() -> {
                    logger.error("User not found for the given user entra id: {}", entraId);
                    return new RuntimeException(
                            String.format("User not found for the given user entra id: %s", entraId));
                });

        if (user.getUserProfiles() == null || user.getUserProfiles().isEmpty()) {
            logger.error("User profile not found for the given entra id: {}", entraId);
            throw new RuntimeException(String.format("User profile not found for the given entra id: %s", entraId));
        }

        return user.getUserProfiles().stream().filter(UserProfile::isActiveProfile)
                .map(UserProfile::getUserType).collect(Collectors.toList());

    }

    public EntraUserDto findUserByUserEntraId(String entraId) {
        EntraUser entraUser = entraUserRepository.findByEntraOid(entraId)
                .orElseThrow(() -> {
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
        return appRepository.findAll().stream()
                .map(app -> mapper.map(app, AppDto.class))
                .collect(Collectors.toList());
    }

    public List<AppDto> getAppsByUserType(UserType userType) {
        if (userType == UserType.INTERNAL) {
            return getAppsByRoleType(RoleType.INTERNAL);
        } else {
            return getAppsByRoleType(RoleType.EXTERNAL);
        }
    }

    private List<AppDto> getAppsByRoleType(RoleType roleType) {
        return appRoleRepository.findByRoleTypeIn(List.of(roleType, RoleType.INTERNAL_AND_EXTERNAL)).stream()
                .map(AppRole::getApp)
                .distinct()
                .map(app -> mapper.map(app, AppDto.class))
                .toList();
    }

    public List<AppRoleDto> getAllAvailableRolesForApps(List<String> selectedApps) {
        // Fetch selected apps
        List<App> apps = appRepository.findAllById(selectedApps.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList()));
        // Return roles
        return apps.stream()
                .flatMap(app -> app.getAppRoles().stream())
                .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                .collect(Collectors.toList());
    }

    public EntraUser createUser(EntraUserDto user, FirmDto firm,
            UserType userType, String createdBy) {

        RegisterUserResponse registerUserResponse = techServicesClient.registerNewUser(user);
        RegisterUserResponse.CreatedUser createdUser = registerUserResponse.getCreatedUser();

        if (createdUser != null && user.getEmail().equalsIgnoreCase(createdUser.getMail())) {
            user.setEntraOid(createdUser.getId());
        } else {
            throw new RuntimeException("User creation failed");
        }

        return persistNewUser(user, firm, userType, createdBy);
    }

    private EntraUser persistNewUser(EntraUserDto newUser, FirmDto firmDto,
            UserType userType, String createdBy) {
        EntraUser entraUser = mapper.map(newUser, EntraUser.class);
        // TODO revisit to set the user entra ID
        Firm firm = mapper.map(firmDto, Firm.class);
        Set<AppRole> appRoles = getAuthzAppRoleByUserType(userType).map(Set::of).orElseGet(Set::of);
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .userType(userType)
                .appRoles(appRoles)
                .createdDate(LocalDateTime.now())
                .createdBy(createdBy)
                .firm(firm)
                .entraUser(entraUser)
                .userProfileStatus(UserProfileStatus.PENDING)
                .build();

        entraUser.setEntraOid(newUser.getEntraOid());
        entraUser.setUserProfiles(Set.of(userProfile));
        entraUser.setUserStatus(UserStatus.ACTIVE);
        // Audit fields are automatically set by Spring Data JPA auditing
        return entraUserRepository.saveAndFlush(entraUser);
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
                .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                .collect(Collectors.toList());
    }

    /**
     * The method loads the user type from DB and map it as user authorities
     *
     * @param entraId the user entra id
     * @return the list of user types associated with entra user
     */
    public List<String> getUserAuthorities(String entraId) {
        EntraUser user = entraUserRepository.findByEntraOid(entraId)
                .orElseThrow(() -> {
                    logger.error("User not found for the given entra id: {}", entraId);
                    return new RuntimeException(String.format("User not found for the given entra id: %s", entraId));
                });

        List<String> grantedAuthorities = Collections.emptyList();

        if (user != null && user.getUserStatus() == UserStatus.ACTIVE) {
            grantedAuthorities = user.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .flatMap(userProfile -> userProfile.getAppRoles().stream())
                    .filter(AppRole::isAuthzRole)
                    .flatMap(appRole -> appRole.getPermissions().stream())
                    .map(Enum::name)
                    .toList();
        }
        return grantedAuthorities;
    }

    public Set<AppDto> getUserAppsByUserId(String userId) {
        Optional<UserProfileDto> optionalUserProfile = getUserProfileById(userId);
        if (optionalUserProfile.isPresent()) {
            UserProfileDto userProfile = optionalUserProfile.get();
            return userProfile.getAppRoles().stream()
                    .map(AppRoleDto::getApp)
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
                .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                .toList();
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
            graphUser = graphClient.users()
                    .byUserId(email)
                    .get();
        } catch (Exception ex) {
            logger.warn("No user found in Entra with matching email. Catching error and moving on.");
        }
        return user.isPresent() || graphUser != null;
    }

    public List<AppRoleDto> getAppRolesByAppId(String appId) {
        UUID appUuid = UUID.fromString(appId);
        Optional<App> optionalApp = appRepository.findById(appUuid);
        List<AppRoleDto> appRoles = new ArrayList<>();
        if (optionalApp.isPresent()) {
            App app = optionalApp.get();
            appRoles = app.getAppRoles().stream()
                    .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                    .toList();
        }
        return appRoles;
    }

    public List<AppRoleDto> getAppRolesByAppIdAndUserType(String appId, UserType userType) {
        UUID appUuid = UUID.fromString(appId);
        Optional<App> optionalApp = appRepository.findById(appUuid);
        List<AppRoleDto> appRoles = new ArrayList<>();
        if (optionalApp.isPresent()) {
            App app = optionalApp.get();
            RoleType userRoleType = userType == UserType.INTERNAL ? RoleType.INTERNAL : RoleType.EXTERNAL;
            appRoles = app.getAppRoles().stream()
                    .filter(appRole -> appRole.isAuthzRole() || appRole.getRoleType().equals(userRoleType)
                            || appRole.getRoleType().equals(RoleType.INTERNAL_AND_EXTERNAL))
                    .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                    .toList();
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

    public Set<LaaApplication> getUserAssignedAppsforLandingPage(String id) {
        Optional<UserProfileDto> userProfile =  getActiveProfileByUserId(id);

        if (userProfile.isEmpty()) {
            logger.error("Active user profile not found for user: {}", id);
            throw new RuntimeException(String.format("User profile not found for the given user id: %s", id));
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
    public void updateUserDetails(String userId, String firstName, String lastName) throws IOException {
        // Update local database
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            EntraUser entraUser = optionalUser.get();
            entraUser.setFirstName(firstName);
            entraUser.setLastName(lastName);

            try {
                entraUserRepository.saveAndFlush(entraUser);
                logger.info("Successfully updated user details in database for user ID: {}", userId);
            } catch (Exception e) {
                logger.error("Failed to update user details in database for user ID: {}", userId, e);
                throw new IOException("Failed to update user details in database", e);
            }
        } else {
            logger.warn("User with id {} not found in database. Could not update local user details.", userId);
        }
    }

    private Set<LaaApplication> getUserAssignedApps(Set<AppDto> userApps) {
        List<LaaApplication> applications = laaApplicationsList.getApplications();
        return applications.stream().filter(app -> userApps.stream()
                .map(AppDto::getName).anyMatch(appName -> appName.equals(app.getName())))
                .sorted(Comparator.comparingInt(LaaApplication::getOrdinal))
                .collect(Collectors.toCollection(TreeSet::new));
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
    public void updateUserOffices(String userId, List<String> selectedOffices) throws IOException {
        Optional<UserProfile> optionalUserProfile = userProfileRepository.findById(UUID.fromString(userId));
        if (optionalUserProfile.isPresent()) {
            UserProfile userProfile = optionalUserProfile.get();
            if (selectedOffices.contains("ALL")) {
                userProfile.setOffices(null);
            } else {
                List<UUID> officeIds = selectedOffices.stream().map(UUID::fromString).collect(Collectors.toList());
                Set<Office> offices = new HashSet<>(officeRepository.findAllById(officeIds));

                // Update user profile offices
                userProfile.setOffices(offices);
            }
            userProfileRepository.saveAndFlush(userProfile);
            logger.info("Successfully updated user offices for user ID: {}", userId);
        } else {
            logger.warn("User profile with id {} not found. Could not update offices.", userId);
            throw new IOException("User profile not found for user ID: " + userId);
        }
    }

    public boolean isInternal(String userId) {
        return isInternal(UUID.fromString(userId));
    }

    public boolean isInternal(UUID userId) {
        return getUserPermissionsByUserId(userId).contains(Permission.VIEW_INTERNAL_USER);
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

    public boolean isUserCreationAllowed(EntraUser entraUser) {
        Optional<UserType> userType = getUserTypeByEntraUser(entraUser);
        return userType.map(UserType::isAllowedToCreateUsers).orElse(false);
    }

    public void setDefaultActiveProfile(EntraUser entraUser, UUID firmId) throws IOException {
        boolean foundFirm = false;
        for (UserProfile userProfile : entraUser.getUserProfiles()) {
            if (userProfile.getFirm().getId().equals(firmId)) {
                userProfile.setActiveProfile(true);
                foundFirm = true;
            } else {
                userProfile.setActiveProfile(false);
            }
        }
        if (!foundFirm) {
            logger.warn("Firm with id {} not found in user profile. Could not update profile.", firmId);
            throw new IOException("Firm not found for firm ID: " + firmId);
        }
        entraUserRepository.saveAndFlush(entraUser);
    }

    public int createInternalPolledUser(List<EntraUserDto> entraUserDtos) {
        List<EntraUser> entraUsers = new ArrayList<>();
        String createdBy = "INTERNAL_USER_SYNC";
        for (EntraUserDto user : entraUserDtos) {
            EntraUser entraUser = mapper.map(user, EntraUser.class);
            UserProfile userProfile = UserProfile.builder()
                    .activeProfile(true)
                    .userProfileStatus(UserProfileStatus.COMPLETE)
                    .userType(UserType.INTERNAL)
                    .createdDate(LocalDateTime.now())
                    .createdBy(createdBy)
                    .entraUser(entraUser)
                    .build();

            entraUser.setEntraOid(user.getEntraOid());
            entraUser.setUserProfiles(Set.of(userProfile));
            entraUser.setUserStatus(UserStatus.ACTIVE);
            entraUser.setCreatedBy(createdBy);
            entraUser.setCreatedDate(LocalDateTime.now());
            entraUsers.add(entraUser);
            //todo: security group to access authz app
        }
        return persistNewInternalUser(entraUsers);
    }

    private int persistNewInternalUser(List<EntraUser> newUsers) {
        int usersPersisted = 0;
        for (EntraUser newUser : newUsers) {
            try {
                logger.info("Adding new internal user id: {} name: {} {}",
                        newUser.getEntraOid(),
                        newUser.getFirstName(),
                        newUser.getLastName());
                entraUserRepository.saveAndFlush(newUser);
                usersPersisted++;
                logger.info("User {} added", newUser.getEntraOid());
            } catch (Exception e) {
                logger.error("Unexpected error when adding user id: {} name: {} {} {}",
                        newUser.getEntraOid(),
                        newUser.getFirstName(),
                        newUser.getLastName(),
                        e.getMessage());
            }
        }
        return usersPersisted;
    }

    private Optional<AppRole> getAuthzAppRoleByUserType(UserType userType) {
        if (userType.getAuthzRoleName() != null) {
            return appRoleRepository.findByName(userType.getAuthzRoleName()).filter(AppRole::isAuthzRole);
        }
        return Optional.empty();
    }

    public Set<Permission> getUserPermissionsByUserId(String userId) {
        return getUserPermissionsByUserId(UUID.fromString(userId));
    }

    public Set<Permission> getUserPermissionsByUserId(UUID userId) {
        Optional<EntraUser> optionalEntraUser = entraUserRepository.findById(userId);
        if (optionalEntraUser.isPresent()) {
            EntraUser entraUser = optionalEntraUser.get();
            return entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
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
            boolean removed = currentRoles.removeIf(role -> 
                role.getApp().getId().toString().equals(appId) 
                && role.getName().equals(roleName)
            );
            
            if (removed) {
                userProfile.setAppRoles(currentRoles);
                userProfileRepository.saveAndFlush(userProfile);
                logger.info("Removed app role '{}' from app '{}' for user '{}'", roleName, appId, userProfileId);
            } else {
                logger.warn("App role '{}' from app '{}' not found for user '{}'", roleName, appId, userProfileId);
            }
        } else {
            logger.warn("User profile with id {} not found. Could not remove app role.", userProfileId);
        }
    }
}
