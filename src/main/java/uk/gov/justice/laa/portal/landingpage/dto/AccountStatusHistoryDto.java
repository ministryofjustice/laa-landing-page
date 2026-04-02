package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * Status change (ENABLED or DISABLED)
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
}

