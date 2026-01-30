package uk.gov.justice.laa.portal.landingpage.entity;

public enum Permission {
    // Some of these are out of scope for MVP but included to be used post-release.
    ACCESS_LANDING_PAGE, // Not used.
    ACCESS_LAA_APPS, // Not used.
    VIEW_INTERNAL_USER,
    VIEW_EXTERNAL_USER,
    VIEW_ALL_USER_MULTI_FIRM_PROFILES,
    CREATE_INTERNAL_USER,
    CREATE_EXTERNAL_USER,
    EDIT_INTERNAL_USER,
    EDIT_EXTERNAL_USER,
    EDIT_USER_FIRM, // Not used.
    EDIT_USER_OFFICE,
    VIEW_USER_OFFICE,
    EDIT_USER_DETAILS,
    DELEGATE_EXTERNAL_USER_ACCESS,
    DELEGATE_EXTERNAL_USER_ACCESS_INTERNAL,
    DELETE_EXTERNAL_USER,
    DELETE_AUDIT_USER,
    VIEW_AUDIT_TABLE,
    DISABLE_EXTERNAL_USER,
    ENABLE_EXTERNAL_USER,
    EXPORT_AUDIT_DATA,
    VIEW_FIRM_DIRECTORY,
    TRIGGER_CCMS_ROLE_SYNC,
    DISABLE_EXTERNAL_USER;

    public static final String[] ADMIN_PERMISSIONS = {
            VIEW_INTERNAL_USER.name(),
            VIEW_EXTERNAL_USER.name(),
            CREATE_INTERNAL_USER.name(),
            CREATE_EXTERNAL_USER.name(),
            EDIT_INTERNAL_USER.name(),
            EDIT_EXTERNAL_USER.name(),
            EDIT_USER_FIRM.name(),
            EDIT_USER_OFFICE.name(),
            VIEW_USER_OFFICE.name(),
            EDIT_USER_DETAILS.name(),
            DELETE_EXTERNAL_USER.name(),
            DELETE_AUDIT_USER.name(),
            VIEW_ALL_USER_MULTI_FIRM_PROFILES.name(),
            DISABLE_EXTERNAL_USER.name(),
            ENABLE_EXTERNAL_USER.name(),
            EXPORT_AUDIT_DATA.name(),
            VIEW_AUDIT_TABLE.name()
    };

    public static final String[] DELEGATE_FIRM_ACCESS_PERMISSIONS = {
            DELEGATE_EXTERNAL_USER_ACCESS.name(),
            DELEGATE_EXTERNAL_USER_ACCESS_INTERNAL.name()
    };
}
