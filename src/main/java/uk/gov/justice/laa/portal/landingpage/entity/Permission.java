package uk.gov.justice.laa.portal.landingpage.entity;

public enum Permission {
    // Some of these are out of scope for MVP but included to be used post-release.
    ACCESS_LANDING_PAGE, // Not used.
    ACCESS_LAA_APPS, // Not used.
    VIEW_INTERNAL_USER,
    VIEW_EXTERNAL_USER,
    CREATE_INTERNAL_USER,
    CREATE_EXTERNAL_USER,
    EDIT_INTERNAL_USER,
    EDIT_EXTERNAL_USER,
    EDIT_USER_FIRM, // Not used.
    EDIT_USER_OFFICE,
    EDIT_USER_DETAILS; // Not used.

    public static final String[] ADMIN_PERMISSIONS = {
            VIEW_INTERNAL_USER.name(),
            VIEW_EXTERNAL_USER.name(),
            CREATE_INTERNAL_USER.name(),
            CREATE_EXTERNAL_USER.name(),
            EDIT_INTERNAL_USER.name(),
            EDIT_EXTERNAL_USER.name(),
            EDIT_USER_FIRM.name(),
            EDIT_USER_OFFICE.name(),
            EDIT_USER_DETAILS.name()
    };
}
