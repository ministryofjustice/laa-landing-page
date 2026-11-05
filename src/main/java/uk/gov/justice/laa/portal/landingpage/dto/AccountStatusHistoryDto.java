package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.utils.DateTimeDisplayUtil;

/**
 * DTO for account status history records
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatusHistoryDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Date and time when the status changed
     */
    private LocalDateTime statusChangedDate;

    /**
     * Status change (ACTIVATED or DEACTIVATED)
     */
    private String statusChange;

    /**
     * Reason for disabling the user (if applicable)
     */
    private String disableReason;

    /**
     * User who changed the status
     */
    private String statusChangedBy;

    /**
     * The delegation level of the user who disabled the account (only populated on DISABLED records).
     * NULL means unknown/legacy.
     */
    private String disableType;

    /**
     * Returns the date and time when the status changed with formatted for display
     *
     * @return formatted status changed date and time
     */
    public String formattedStatusChangedDate() {
        return DateTimeDisplayUtil.format(
                statusChangedDate, DateTimeDisplayUtil.SHORT_MONTH_DATE_TIME);
    }
}
