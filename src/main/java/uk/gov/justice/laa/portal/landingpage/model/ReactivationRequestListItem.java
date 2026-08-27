package uk.gov.justice.laa.portal.landingpage.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public record ReactivationRequestListItem(
        UUID id,
        UUID requestId,
        UUID userProfileId,
        Integer version,
        ReactivationRequestStatus requestStatus,
        String comments,
        String actorEntraOid,
        String actorRoleType,
        String actorName,
        String actorEmail,
        String userName,
        String userEmail,
        String userType,
        LocalDate dateSubmitted,
        LocalDate lastActivity,
        UUID firmId
) implements Serializable {
}
