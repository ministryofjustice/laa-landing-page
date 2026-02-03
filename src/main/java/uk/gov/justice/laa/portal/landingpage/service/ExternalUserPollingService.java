package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraLastSyncMetadata;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserReasonRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraLastSyncMetadataRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUsersResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalUserPollingService {

    private static final String ENTRA_USER_SYNC_ID = "ENTRA_USER_SYNC";
    
    private final EntraLastSyncMetadataRepository entraLastSyncMetadataRepository;
    private final EntraUserRepository entraUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final DisableUserReasonRepository disableUserReasonRepository;
    private final UserAccountStatusAuditRepository userAccountStatusAuditRepository;
    private final TechServicesClient techServicesClient;
    
    @Value("${app.entra.sync.buffer.minutes:5}")
    private int bufferMinutes;

    /**
     * Updates the sync metadata with current timestamp and last successful sync time.
     * This method is called by the scheduler to track sync operations.
     * It calls the Tech Services API to get users and updates the metadata accordingly.
     * Only updates the database on successful API calls.
     */
    @Transactional
    public void updateSyncMetadata() {
        LocalDateTime toTime = LocalDateTime.now();
        
        try {
            Optional<EntraLastSyncMetadata> existingMetadata = entraLastSyncMetadataRepository.findById(ENTRA_USER_SYNC_ID);

            LocalDateTime lastTo =
                    existingMetadata.map(EntraLastSyncMetadata::getLastSuccessfulTo).orElse(null);

            LocalDateTime fromTime = (lastTo != null)
                    ? lastTo.minusMinutes(bufferMinutes)
                    : toTime.minusMonths(1);

            // Cap the time gap to 30 minutes
            if (ChronoUnit.MINUTES.between(fromTime, toTime) > 30) {
                toTime = fromTime.plusMinutes(30);
            }

            String fromDateTime = fromTime.truncatedTo(ChronoUnit.SECONDS) + ".00Z";
            String toDateTime = toTime.truncatedTo(ChronoUnit.SECONDS) + ".00Z";

            log.debug("Calling Tech Services API to get users from {} to {} (gap: {} minutes)", 
                     fromDateTime, toDateTime, ChronoUnit.MINUTES.between(fromTime, toTime));
            TechServicesApiResponse<GetUsersResponse> response = techServicesClient.getUsers(fromDateTime, toDateTime);
            
            if (response.isSuccess()) {
                GetUsersResponse usersResponse = response.getData();
                int userCount = usersResponse.getUsers() != null ? usersResponse.getUsers().size() : 0;
                log.info("Successfully retrieved {} users from Tech Services", userCount);

                if (usersResponse.getUsers() != null && !usersResponse.getUsers().isEmpty()) {
                    synchronizeUsers(usersResponse.getUsers(), toTime);
                }
                
                updateSyncMetadataOnSuccess(existingMetadata, toTime, "Successfully saved EntraLastSyncMetadata: updatedAt={}, lastSuccessfulTo={}");
            } else {
                String errorMessage = response.getError().getMessage();
                if ("Users not found.".equals(errorMessage)) {
                    log.info("No users updated in the specified time range");
                    updateSyncMetadataOnSuccess(existingMetadata, toTime, "Successfully saved EntraLastSyncMetadata after 'Users not found to update' response: updatedAt={}, lastSuccessfulTo={}");
                } else {
                    log.warn("Failed to retrieve users from Tech Services: {}", errorMessage);
                    throw new RuntimeException("Tech Services API call failed: " + errorMessage);
                }
            }
        } catch (Exception e) {
            log.error("Error during sync process", e);
            throw e;
        }
    }

    /**
     * Synchronizes user data from Tech Services API response to local EntraUser records.
     * Updates firstName, lastName, lastLoginDate, enabled, mailOnly, and lastSyncedOn fields.
     * 
     * @param users List of users from Tech Services API
     * @param syncTime The sync time to set as lastSyncedOn
     */
    private void synchronizeUsers(List<GetUsersResponse.TechServicesUser> users, LocalDateTime syncTime) {
        int updatedCount = 0;
        
        for (GetUsersResponse.TechServicesUser user : users) {
            try {
                Optional<EntraUser> entraUserOptional = entraUserRepository.findByEntraOid(user.getId());
                
                if (entraUserOptional.isPresent()) {
                    EntraUser entraUser = entraUserOptional.get();

                    if (user.isDeleted()) {
                        deleteUser(entraUser);
                        log.info("Deleted user: {}  - marked as deleted in Entra",
                                entraUser.getEmail());
                        continue;
                    }

                    // Update user fields if not deleted
                    if (shouldDisableUser(user, entraUser)) {
                        disableUserWithReason(user, entraUser);
                    }

                    if (user.getGivenName() != null && !user.getGivenName().equals(entraUser.getFirstName())) {
                        entraUser.setFirstName(user.getGivenName());
                    }

                    if (user.getSurname() != null && !user.getSurname().equals(entraUser.getLastName())) {
                        entraUser.setLastName(user.getSurname());
                    }

                    if (user.isAccountEnabled() != entraUser.isEnabled()) {
                        entraUser.setEnabled(user.isAccountEnabled());
                    }

                    if (user.isMailOnly() != entraUser.isMailOnly()) {
                        entraUser.setMailOnly(user.isMailOnly());
                    }

                    if (user.getLastSignIn() != null && !user.getLastSignIn().trim().isEmpty()) {
                        entraUser.setLastLoginDate(LocalDateTime.parse(user.getLastSignIn().substring(0, user.getLastSignIn().length() - 1)));
                    }

                    entraUser.setLastSyncedOn(syncTime);

                    entraUserRepository.save(entraUser);
                    updatedCount++;
                    log.debug("Updated user: {} ({})", entraUser.getEmail(), user.getId());
                }
            } catch (Exception e) {
                log.error("Error synchronizing user {}: {}", user.getId(), e.getMessage(), e);
            }
        }
        
        log.info("User synchronization completed: {} users updated",
                updatedCount);
    }

    private void deleteUser(EntraUser entraUser) {
        try {
            List<UserProfile> userProfiles = entraUser.getUserProfiles() != null 
                ? new ArrayList<>(entraUser.getUserProfiles()) 
                : new ArrayList<>();

            for (UserProfile userProfile : userProfiles) {
                if (userProfile.getAppRoles() != null) {
                    userProfile.getAppRoles().clear();
                }
                
                if (userProfile.getOffices() != null) {
                    userProfile.getOffices().clear();
                }
                entraUser.getUserProfiles().remove(userProfile);
                userProfile.setEntraUser(null);

                userProfileRepository.save(userProfile);
                userProfileRepository.flush();
                userProfileRepository.delete(userProfile);
            }

            userProfileRepository.flush();

            entraUserRepository.delete(entraUser);
            entraUserRepository.flush();
            
            log.debug("Successfully deleted user and all related entities: {} ({})", 
                    entraUser.getEmail(), entraUser.getEntraOid());
                    
        } catch (Exception e) {
            log.error("Error deleting user {} ({}): {}", 
                    entraUser.getEmail(), entraUser.getEntraOid(), e.getMessage(), e);
            throw new RuntimeException("Failed to delete user: " + entraUser.getEmail(), e);
        }
    }

    private boolean shouldDisableUser(GetUsersResponse.TechServicesUser user, EntraUser entraUser) {
        if (user.getCustomSecurityAttributes() != null
                && user.getCustomSecurityAttributes().getGuestUserStatus() != null
                && user.getCustomSecurityAttributes().getGuestUserStatus().getDisabledReason() != null) {

            return entraUser.isEnabled();
        }
        return false;
    }

    private void disableUserWithReason(GetUsersResponse.TechServicesUser user, EntraUser entraUser) {
        try {
            String disabledReasonFromApi = user.getCustomSecurityAttributes()
                    .getGuestUserStatus().getDisabledReason();

            DisableUserReason disableReason = findOrCreateDisableReason(disabledReasonFromApi);

            entraUser.setEnabled(false);
            entraUserRepository.save(entraUser);

            UserAccountStatusAudit audit = UserAccountStatusAudit.builder()
                    .entraUser(entraUser)
                    .disableUserReason(disableReason)
                    .statusChange(UserAccountStatus.DISABLED)
                    .disabledBy("External user sync") // Automated disable from API sync
                    .disabledDate(LocalDateTime.now())
                    .build();
            
            userAccountStatusAuditRepository.save(audit);
            
            log.info("Disabled user: {} ({}) - Reason: {} from API sync", 
                    entraUser.getEmail(), entraUser.getEntraOid(), disableReason.getName());
                    
        } catch (Exception e) {
            log.error("Error disabling user {} ({}): {}", 
                    entraUser.getEmail(), entraUser.getEntraOid(), e.getMessage(), e);
        }
    }

    private DisableUserReason findOrCreateDisableReason(String reasonFromApi) {
        Optional<DisableUserReason> existingReason = disableUserReasonRepository.findAll()
                .stream()
                .filter(reason -> reason.getName().equalsIgnoreCase(reasonFromApi)
                        || reason.getEntraDescription().equalsIgnoreCase(reasonFromApi))
                .findFirst();

        return existingReason.orElseGet(() -> disableUserReasonRepository.findAll()
                .stream()
                .filter(reason -> "Inactivity".equals(reason.getName()))
                .findFirst()
                .get());

    }

    private void updateSyncMetadataOnSuccess(Optional<EntraLastSyncMetadata> existingMetadata, LocalDateTime toTime, String logMessage) {
        LocalDateTime syncCompletedTime = LocalDateTime.now();
        
        EntraLastSyncMetadata metadata = existingMetadata.orElseGet(() -> 
            EntraLastSyncMetadata.builder()
                    .id(ENTRA_USER_SYNC_ID)
                    .build()
        );
        
        metadata.setUpdatedAt(syncCompletedTime);
        metadata.setLastSuccessfulTo(toTime);
        
        entraLastSyncMetadataRepository.save(metadata);
        log.info(logMessage, syncCompletedTime, toTime);
    }
}
