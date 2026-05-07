package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO for Deleted Users view
 * Contains information about users that have been deleted from the system
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletedUserAuditDto implements Serializable {

    /**
     * Full name of the deleted user
     */
    private String userName;

    /**
     * Email of the deleted user
     */
    private String userEmail;

    /**
     * Date and time when the user was deleted
     */
    private LocalDateTime deletedDate;

    /**
     * Name of the person who deleted the user (First Name + Last Name)
     */
    private String deletedBy;
}

