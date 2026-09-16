package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum ReactivationRoleType {

    NONE("None"),
    SYNC("System User Sync"),
    LAA_OST("Legal Aid Agency (Online Support)"), // External User Manager
    PROVIDER_ADMIN("Provider Admin"), // Firm user manager
    LAA_USER_REGISTRATION("Legal Aid Agency (User Registration)"),  // External User Admin
    LAA_SUPPORT("Legal Aid Agency (User Support)"), // External User Support
    LAA("Legal Aid Agency"); // Global Admin, Security Response

    private final String displayName;

    ReactivationRoleType(String displayName) {
        this.displayName = displayName;
    }

    public String getLabelForUi(String userName) {
        if (this == ReactivationRoleType.PROVIDER_ADMIN) {
            return userName + " (" + displayName + ")";
        }
        return displayName;
    }

}
