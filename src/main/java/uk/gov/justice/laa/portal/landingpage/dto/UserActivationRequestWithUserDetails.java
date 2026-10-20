package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;

/**
 * A reactivation request along with the name and email recorded in the DELETED
 * user_account_status_audit row for the target user. The audit values are null unless the user was deleted.
 */
public record UserActivationRequestWithUserDetails(
        UserActivationRequest request,
        String deletedUserName,
        String deletedUserEmail,
        boolean userDeleted) {
}
