package uk.gov.justice.laa.portal.landingpage.client;

import java.util.UUID;

/**
 * Legacy /me debug response, retained independently of the evolving query API contracts.
 */
public record UserProfileDetailResponse(UUID userProfileId, String userEntraObjectId, String userType,
                                        String email, String fullName, UUID firmId, String firmName,
                                        String accountStatus, String profileStatus, boolean activeProfile,
                                        boolean hasAppRoles, boolean unrestrictedOfficeAccess) {
}
