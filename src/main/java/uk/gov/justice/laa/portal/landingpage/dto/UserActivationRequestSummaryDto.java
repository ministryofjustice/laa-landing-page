package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.AuthzRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public record UserActivationRequestSummaryDto(UUID id, UUID requestId, UUID userProfileId, Integer version,
                                              ReactivationRequestStatus status, String comments, String actorEntraOid,
                                              AuthzRoleType actorRoleType,Instant createdAt, String actorName) {
    private static final DateTimeFormatter UK_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)
                    .withZone(ZoneOffset.UTC);

    public String formattedCreatedAt() {
        return createdAt == null ? "" : UK_DATE_FORMATTER.format(createdAt);
    }
}
