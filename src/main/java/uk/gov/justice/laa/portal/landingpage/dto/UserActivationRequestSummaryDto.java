package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRoleType;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;
import uk.gov.justice.laa.portal.landingpage.utils.DateTimeDisplayUtil;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public record UserActivationRequestSummaryDto(UUID id, UUID requestId, UUID userProfileId, Integer version,
                                              ReactivationRequestStatus status, String comments, String actorEntraOid,
                                              ReactivationRoleType actorRoleType, Instant createdAt, String actorName) {

    public static final Map<Long, String> AMPM_LOWERCASE = Map.of(
            0L, "am",
            1L, "pm"
    );

    public static final DateTimeFormatter DISPLAY_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("d MMMM yyyy h:mm")
            .appendText(ChronoField.AMPM_OF_DAY, AMPM_LOWERCASE)
            .toFormatter(Locale.UK);

    public String formattedCreatedAt() {
        return createdAt == null ? "" : DateTimeDisplayUtil.formatWithDaylightSavings(createdAt, DISPLAY_FORMATTER);
    }
}
