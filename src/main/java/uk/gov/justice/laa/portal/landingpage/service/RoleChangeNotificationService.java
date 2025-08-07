package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.client.CcmsApiClient;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.model.CcmsMessage;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleChangeNotificationService {

    private final CcmsApiClient ccmsApiClient;

    /**
     * This method will automatically retry up to 3 times with 0.1 second delays
     *
     * @param userProfile The user profile
     * @param newPuiRoles new roles filtered by PUI
     * @param oldPuiRoles old roled filtered by PUI
     * @return true if successful
     */
    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100)
    )
    public boolean sendMessage(UserProfile userProfile, Set<AppRole> newPuiRoles, Set<AppRole> oldPuiRoles) {
        try {
            sendRoleChangeNotification(userProfile, newPuiRoles, oldPuiRoles);
            return true;
        } catch (Exception e) {
            log.warn("Failed to send CCMS role change for user: {}: {}, saving roles to db and moving on",
                userProfile.getEntraUser().getEntraOid(), e.getMessage());
            throw e;
        }
    }
    
    /**
     * Recovery method called when all retry attempts fail.
     * 
     * @param ex The exception that caused the failure
     * @param userProfile The user profile
     * @param newPuiRoles New PUI roles
     * @param oldPuiRoles Old PUI roles
     * @return false to indicate failure
     */
    @Recover
    public boolean recoverFromRoleChangeNotificationFailure(Exception ex, UserProfile userProfile, Set<AppRole> newPuiRoles, Set<AppRole> oldPuiRoles) {
        log.warn("All retry attempts failed to send CCMS role change  for user: {}. Updated roles will be saved with lastCcmsSyncSuccessful=false.",
            userProfile.getEntraUser().getEntraOid(), ex);
        return false;
    }

    private void sendRoleChangeNotification(UserProfile userProfile, Set<AppRole> newPuiRoles, Set<AppRole> oldPuiRoles) {
        EntraUser entraUser = userProfile.getEntraUser();
        if (!newPuiRoles.equals(oldPuiRoles)
                && !UserType.INTERNAL_TYPES.contains(userProfile.getUserType())) {
            try {
                CcmsMessage message = CcmsMessage.builder()
                        .userName(userProfile.getLegacyUserId().toString())
                        .vendorNumber(userProfile.getFirm().getCode())
                        .firstName(entraUser.getFirstName())
                        .lastName(entraUser.getLastName())
                        .timestamp(LocalDateTime.now())
                        .email(entraUser.getEmail())
                        .responsibilityKey(newPuiRoles.stream().map(AppRole::getCcmsCode).toList())
                        .build();

                ccmsApiClient.sendUserRoleChange(message);
                log.info("CCMS role change sent for user: {}", entraUser.getEntraOid());
                
            } catch (Exception e) {
                log.error("Failed to send role change for user: {}", entraUser.getEntraOid(), e);
                throw new RuntimeException("Failed to send message", e);
            }
        }
    }
}
