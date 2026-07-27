package uk.gov.justice.laa.portal.landingpage.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

public record ReactivationRequestListItem(
        UUID id,
        String name,
        String email,
        LocalDate dateSubmitted,
        LocalDate lastActivity,
        ReactivationRequestUserType userType,
        ReactivationRequestStatus requestStatus,
        UUID firmId
) implements Serializable {
}
