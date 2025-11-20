package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for User Access Audit Table
 * Contains aggregated user information across all their profiles
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditUserDto implements Serializable {

    /**
     * Full name of the user (First Name + Last Name)
     */
    private String name;

    /**
     * Email address of the user
     */
    private String email;

    /**
     * User ID for linking to manage user page
     */
    private String userId;

    /**
     * User type: External, Internal, or External-3rdParty
     */
    private String userType;

    /**
     * Firm name(s) or "None" if no profile
     * Multiple firms separated by comma for multi-firm users
     */
    private String firmAssociation;

    /**
     * Account status: Active, Inactive, Pending, or Disabled
     */
    private String accountStatus;

    /**
     * Flag indicating if user has multi-firm access
     */
    private boolean isMultiFirmUser;

    /**
     * Number of firm profiles the user has
     */
    private int profileCount;

    /**
     * Date when the user account was created
     */
    private LocalDateTime createdDate;

    /**
     * User who created this account
     */
    private String createdBy;

    /**
     * Last login date from Graph API signInActivity
     */
    private LocalDateTime lastLoginDate;

    /**
     * Entra status from EntraUser.userStatus enum
     */
    private String entraStatus;

    /**
     * Activation status from TechServices API
     * Values: Pending, Accepted, Expired, Revoked
     */
    private String activationStatus;
}
