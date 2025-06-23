package uk.gov.justice.laa.portal.landingpage.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.microsoft.graph.core.content.BatchRequestContent;
import com.microsoft.graph.models.DirectoryRole;
import com.microsoft.graph.models.Invitation;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.kiota.RequestInformation;

import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
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

    /**
     * The number of pages to load in advance when doing user pagination
     */
    private static final int PAGES_TO_PRELOAD = 5;

    public UserService(@Qualifier("graphServiceClient") GraphServiceClient graphClient, EntraUserRepository entraUserRepository,
            AppRepository appRepository, AppRoleRepository appRoleRepository, ModelMapper mapper, NotificationService notificationService, OfficeRepository officeRepository) {
        this.graphClient = graphClient;
        this.entraUserRepository = entraUserRepository;
        this.appRepository = appRepository;
        this.appRoleRepository = appRoleRepository;
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.officeRepository = officeRepository;
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
                // Set to default profile for now, will need to receive a user profile from front end at some point.
                .filter(UserProfile::isDefaultProfile)
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

    public String formatLastSignInDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale.ENGLISH);

        return dateTime.format(formatter);
    }

    public List<LaaApplication> getManagedAppRegistrations() {
        try {
            var response = graphClient.applications().get();
            return (response != null && response.getValue() != null)
                    ? LaaAppDetailsStore.getUserAssignedApps(response.getValue())
                    : Collections.emptyList();
        } catch (Exception ex) {
            logger.error("Error fetching managed app registrations: ", ex);
            return Collections.emptyList();
        }
    }

    private PaginatedUsers getPageOfUsers(Supplier<Page<EntraUser>> pageSupplier) {
        Page<EntraUser> userPage = pageSupplier.get();
        PaginatedUsers paginatedUsers = new PaginatedUsers();
        paginatedUsers.setTotalUsers(userPage.getTotalElements());
        paginatedUsers.setUsers(userPage.stream().map(user -> mapper.map(user, EntraUserDto.class)).toList());
        paginatedUsers.setTotalPages(userPage.getTotalPages());
        return paginatedUsers;
    }

    public PaginatedUsers getPageOfUsers(int page, int pageSize) {
        return getPageOfUsers(() -> entraUserRepository.findAll(PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    public PaginatedUsers getPageOfUsersByNameOrEmail(int page, int pageSize, String searchTerm) {
        return getPageOfUsers(() -> entraUserRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                searchTerm, searchTerm, searchTerm, PageRequest.of(Math.max(0, page - 1), pageSize)));
    }

    public List<EntraUserDto> getSavedUsers() {
        return entraUserRepository.findAll().stream()
                .map(user -> mapper.map(user, EntraUserDto.class))
                .collect(Collectors.toList());
    }

    public List<UserType> findUserTypeByUserEntraId(String entraId) {
        EntraUser user = entraUserRepository.findByEntraId(entraId)
                .orElseThrow(() -> {
                    logger.error("User not found for the given user entra id: {}", entraId);
                    return new RuntimeException(String.format("User not found for the given user entra id: %s", entraId));
                });

        if (user.getUserProfiles() == null || user.getUserProfiles().isEmpty()) {
            logger.error("User profile not found for the given entra id: {}", entraId);
            throw new RuntimeException(String.format("User profile not found for the given entra id: %s", entraId));
        }

        return user.getUserProfiles().stream().map(UserProfile::getUserType).collect(Collectors.toList());

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

    public EntraUser createUser(User user, List<String> roles, List<String> selectedOffices, FirmDto firm, boolean isFirmAdmin, String createdBy) {

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

        //Send invitation email
        assert result != null;
        notificationService.notifyCreateUser(invitation.getInvitedUserDisplayName(), user.getMail(),
                result.getInviteRedeemUrl());

        return result.getInvitedUser();
    }

    private EntraUser persistNewUser(User newUser, List<String> roles, List<String> selectedOffices, FirmDto firmDto, boolean isFirmAdmin, String createdBy) {
        EntraUser entraUser = mapper.map(newUser, EntraUser.class);
        // TODO revisit to set the user entra ID
        entraUser.setEntraId(newUser.getMail());
        Firm firm = mapper.map(firmDto, Firm.class);
        List<AppRole> appRoles = appRoleRepository.findAllById(roles.stream().map(UUID::fromString)
                .collect(Collectors.toList()));
        List<UUID> officeIds = selectedOffices.stream().map(UUID::fromString).toList();
        Set<Office> offices = new HashSet<Office>(officeRepository.findOfficeByFirm_IdIn(officeIds));
        UserProfile userProfile = UserProfile.builder()
                .defaultProfile(true)
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
        EntraUser user = entraUserRepository.findByEntraId(entraId)
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

    public EntraUser getUserByEntraUserId(UUID userId) {
        Optional<EntraUser> optionalUser = entraUserRepository.findByEntraId(userId.toString());
        return optionalUser.orElse(null);
    }
}
