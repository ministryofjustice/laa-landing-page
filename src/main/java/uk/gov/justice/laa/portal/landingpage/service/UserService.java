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

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.RequestInformation;

import uk.gov.justice.laa.portal.landingpage.config.LaaAppsConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.model.PaginatedUsers;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

/**
 * userService
 */
@Service
public class UserService {

    private static final int BATCH_SIZE = 20;
    private final OfficeRepository officeRepository;

    @Value("${spring.security.oauth2.client.registration.azure.redirect-uri}")
    private String redirectUri;

    private final GraphServiceClient graphClient;
    private final EntraUserRepository entraUserRepository;
    private final AppRepository appRepository;
    private final AppRoleRepository appRoleRepository;
    private final ModelMapper mapper;
    private final NotificationService notificationService;
    private final LaaAppsConfig.LaaApplicationsList laaApplicationsList;

    /**
     * The number of pages to load in advance when doing user pagination
     */
    private static final int PAGES_TO_PRELOAD = 5;

    public UserService(@Qualifier("graphServiceClient") GraphServiceClient graphClient, EntraUserRepository entraUserRepository,
            AppRepository appRepository, AppRoleRepository appRoleRepository, ModelMapper mapper,
                       NotificationService notificationService, OfficeRepository officeRepository,
                       LaaAppsConfig.LaaApplicationsList laaApplicationsList) {
        this.graphClient = graphClient;
        this.entraUserRepository = entraUserRepository;
        this.appRepository = appRepository;
        this.appRoleRepository = appRoleRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.officeRepository = officeRepository;
        this.laaApplicationsList = laaApplicationsList;
    }

    Logger logger = LoggerFactory.getLogger(this.getClass());

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

    public void updateUserRoles(String userId, List<String> selectedRoles) {
        List<AppRole> roles = appRoleRepository.findAllById(selectedRoles.stream()
                .map(UUID::fromString)
                .collect(Collectors.toList()));
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            EntraUser user = optionalUser.get();
            updateUserProfileRoles(user, roles);
        } else {
            logger.warn("User with id {} not found. Could not update roles.", userId);
        }
    }

    private void updateUserProfileRoles(EntraUser user, List<AppRole> roles) {
        Optional<UserProfile> userProfile = user.getUserProfiles().stream()
                // Set to default profile for now, will need to receive a user profile from
                // front end at some point.
                .filter(UserProfile::isActiveProfile)
                .findFirst();
        if (userProfile.isPresent()) {
            userProfile.get().setAppRoles(new HashSet<>(roles));
            entraUserRepository.saveAndFlush(user);
        } else {
            logger.warn("User profile for user ID {} not found. Could not update roles.", user.getId());
        }
    }

    public List<DirectoryRole> getDirectoryRolesByUserId(String userId) {
        return Objects.requireNonNull(graphClient.users().byUserId(userId).memberOf().get())
                .getValue()
                .stream()
                .filter(obj -> obj instanceof DirectoryRole)
                .map(obj -> (DirectoryRole) obj)
                .collect(Collectors.toList());
    }

    public Optional<EntraUserDto> getEntraUserById(String userId) {
        return entraUserRepository.findById(UUID.fromString(userId))
                .map(user -> mapper.map(user, EntraUserDto.class));
    }

    public Optional<UserType> getUserTypeByUserId(String userId) {
        Optional<EntraUser> optionalEntraUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalEntraUser.isPresent()) {
            EntraUser user = optionalEntraUser.get();
            return user.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .map(UserProfile::getUserType)
                    .findFirst();
        } else {
            return Optional.empty();
        }
    }

    public String formatLastSignInDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ENGLISH);

        return dateTime.format(formatter);
    }

    private PaginatedUsers getPageOfUsers(Supplier<Page<EntraUser>> pageSupplier) {
        Page<EntraUser> userPage = pageSupplier.get();
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setTotalUsers(userPage.getTotalElements());
        paginatedUsers.setUsers(userPage.stream().map(user -> mapper.map(user, EntraUserDto.class)).toList());
        paginatedUsers.setTotalPages(userPage.getTotalPages());
        return paginatedUsers;
    }

    public PaginatedUsers getPageOfUsersByNameOrEmail(String searchTerm, boolean isInternal, boolean isFirmAdmin, List<UUID> firmList, int page, int pageSize) {
        List<UserType> types;
        Page<EntraUser> pageOfUsers;
        if (Objects.isNull(firmList)) {
            if (isFirmAdmin) {
                types = List.of(UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
            } else if (isInternal) {
                types = UserType.INTERNAL_TYPES;
            } else {
                types = UserType.EXTERNAL_TYPES;
            }
            if (Objects.isNull(searchTerm) || searchTerm.isEmpty()) {
                pageOfUsers = entraUserRepository.findByUserTypes(types, PageRequest.of(Math.max(0, page - 1), pageSize));
            } else {
                pageOfUsers = entraUserRepository.findByNameEmailAndUserTypes(searchTerm, searchTerm,
                        searchTerm, types, PageRequest.of(Math.max(0, page - 1), pageSize));
            }
        } else {
            if (isFirmAdmin) {
                types = List.of(UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
            } else {
                types = UserType.EXTERNAL_TYPES;
            }
            if (Objects.isNull(searchTerm) || searchTerm.isEmpty()) {
                pageOfUsers = entraUserRepository.findByUserTypesAndFirms(types, firmList, PageRequest.of(Math.max(0, page - 1), pageSize));
            } else {
                pageOfUsers = entraUserRepository.findByNameEmailAndUserTypesFirms(searchTerm, searchTerm,
                        searchTerm, types, firmList, PageRequest.of(Math.max(0, page - 1), pageSize));
            }
        }
        return getPageOfUsers(() -> pageOfUsers);
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

        return user.getUserProfiles().stream().map(UserProfile::getUserType).collect(Collectors.toList());

    }

    public EntraUserDto findUserByUserEntraId(String entraId) {
        EntraUser entraUser = entraUserRepository.findByEntraOid(entraId)
                .orElseThrow(() -> {
                    logger.error("User not found for the given user entra user id: {}", entraId);
                    return new RuntimeException(String.format("User not found for the given user entra id: %s", entraId));
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

    static <T> List<List<T>> partitionBasedOnSize(List<T> inputList, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i += size) {
            partitions.add(inputList.subList(i, Math.min(i + size, inputList.size())));
        }
        return partitions;
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

    public EntraUser createUser(User user, List<String> roles, List<String> selectedOffices, FirmDto firm,
            boolean isFirmAdmin, String createdBy) {

        User invitedUser = inviteUser(user);
        assert invitedUser != null;

        return persistNewUser(user, roles, selectedOffices, firm, isFirmAdmin, createdBy);
    }

    private User inviteUser(User user) {
        Invitation invitation = new Invitation();
        invitation.setInvitedUserEmailAddress(user.getMail());
        invitation.setInviteRedirectUrl(redirectUri);
        invitation.setInvitedUserType("Guest");
        invitation.setSendInvitationMessage(false);
        invitation.setInvitedUserDisplayName(user.getGivenName() + " " + user.getSurname());
        Invitation result = graphClient.invitations().post(invitation);

        // Send invitation email
        assert result != null;
        notificationService.notifyCreateUser(invitation.getInvitedUserDisplayName(), user.getMail(),
                result.getInviteRedeemUrl());

        return result.getInvitedUser();
    }

    private EntraUser persistNewUser(User newUser, List<String> roles, List<String> selectedOffices, FirmDto firmDto,
            boolean isFirmAdmin, String createdBy) {
        EntraUser entraUser = mapper.map(newUser, EntraUser.class);
        // TODO revisit to set the user entra ID
        entraUser.setEntraOid(newUser.getMail());
        Firm firm = mapper.map(firmDto, Firm.class);
        List<AppRole> appRoles = appRoleRepository.findAllById(roles.stream().map(UUID::fromString)
                .collect(Collectors.toList()));
        List<UUID> officeIds = selectedOffices.stream().map(UUID::fromString).toList();
        Set<Office> offices = new HashSet<Office>(officeRepository.findOfficeByFirm_IdIn(officeIds));
        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .appRoles(new HashSet<>(appRoles))
                // TODO: Set this dynamically once we have usertype selection on the front end
                .userType(isFirmAdmin ? UserType.EXTERNAL_SINGLE_FIRM_ADMIN : UserType.EXTERNAL_SINGLE_FIRM)
                .createdDate(LocalDateTime.now())
                .offices(offices)
                .createdBy(createdBy)
                .firm(firm)
                .entraUser(entraUser)
                .build();

        entraUser.setUserProfiles(Set.of(userProfile));
        entraUser.setUserStatus(UserStatus.ACTIVE);
        entraUser.setCreatedBy(createdBy);
        entraUser.setCreatedDate(LocalDateTime.now());
        return entraUserRepository.saveAndFlush(entraUser);
    }

    public List<AppRoleDto> getUserAppRolesByUserId(String userId) {
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            EntraUser user = optionalUser.get();
            return user.getUserProfiles().stream()
                    .flatMap(userProfile -> userProfile.getAppRoles().stream())
                    .map(appRole -> mapper.map(appRole, AppRoleDto.class))
                    .collect(Collectors.toList());
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
                    .map(userProfile -> userProfile.getUserType().name())
                    .toList();

        }
        return grantedAuthorities;
    }

    public Set<AppDto> getUserAppsByUserId(String userId) {
        Optional<EntraUser> optionalUser = entraUserRepository.findById(UUID.fromString(userId));
        if (optionalUser.isPresent()) {
            EntraUser user = optionalUser.get();
            return user.getUserProfiles().stream()
                    .flatMap(userProfile -> userProfile.getAppRoles().stream())
                    .map(AppRole::getApp)
                    .map(app -> mapper.map(app, AppDto.class))
                    .collect(Collectors.toSet());
        } else {
            logger.warn("No user found for user id {} when getting user apps", userId);
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
                    .filter(appRole -> appRole.getRoleType().equals(userRoleType) || appRole.getRoleType().equals(RoleType.INTERNAL_AND_EXTERNAL))
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
        Set<AppDto> userApps = getUserAppsByUserId(id);

        return getUserAssignedApps(userApps);
    }

    private Set<LaaApplication> getUserAssignedApps(Set<AppDto> userApps) {
        List<LaaApplication> applications = laaApplicationsList.getApplications();
        return applications.stream().filter(app -> userApps.stream()
                        .map(AppDto::getName).anyMatch(appName -> appName.equals(app.getName())))
                .sorted(Comparator.comparingInt(LaaApplication::getOrdinal)).collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean isInternal(EntraUser entraUser) {
        List<UserType> userTypes = entraUser.getUserProfiles().stream()
                .map(UserProfile::getUserType).toList();
        return userTypes.contains(UserType.INTERNAL);
    }
}
