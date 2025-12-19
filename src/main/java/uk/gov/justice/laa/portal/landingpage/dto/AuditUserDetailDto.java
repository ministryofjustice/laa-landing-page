package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for detailed User Access Audit drill-down view Contains complete user information including
 * all profiles, roles, and audit data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditUserDetailDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * User ID
     */
    private String userId;

    /**
     * Email address of the user
     */
    private String email;

    /**
     * First name
     */
    private String firstName;

    /**
     * Last name
     */
    private String lastName;

    /**
     * Full name (First Name + Last Name)
     */
    private String fullName;

    /**
     * Flag indicating if user has multi-firm access
     */
    private boolean isMultiFirmUser;

    /**
     * User type classification Values: "Internal", "External", or "External - 3rd Party"
     */
    private String userType;

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
     * Activation status from TechServices API Values: Pending, Accepted, Expired, Revoked
     */
    private String activationStatus;

    /**
     * User profiles (one or more for multi-firm users)
     */
    private List<AuditProfileDto> profiles;

    /**
     * Total number of profiles (for pagination)
     */
    private long totalProfiles;

    /**
     * Total pages for profile pagination
     */
    private int totalProfilePages;

    /**
     * Current profile page number
     */
    private int currentProfilePage;

    /**
     * Flag indicating if user has no profile (Entra-only data)
     */
    private boolean hasNoProfile;

    /**
     * Profile DTO for the audit detail view
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditProfileDto implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * Profile ID
         */
        private String profileId;

        /**
         * Firm name
         */
        private String firmName;

        /**
         * Firm code
         */
        private String firmCode;

        /**
         * Office restrictions (list of office names or "Access to All Offices")
         */
        private String officeRestrictions;

        /**
         * List of offices user has access to
         */
        private List<OfficeDto> offices;

        /**
         * Roles assigned (list of app role display names)
         */
        private List<AppRoleDto> roles;

        /**
         * User type for this profile
         */
        private String userType;

        /**
         * Whether this is the active profile
         */
        private boolean activeProfile;
    }
}
