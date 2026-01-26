package uk.gov.justice.laa.portal.landingpage.entity;

/**
 * Enumeration of Authorization Role names used in the system.
 * These correspond to role names in the app_role table where authz_role = true.
 */
public enum AuthzRole {
    GLOBAL_ADMIN("Global Admin"),
    INTERNAL_USER_MANAGER("Internal User Manager"),
    EXTERNAL_USER_MANAGER("External User Manager"),
    FIRM_USER_MANAGER("Firm User Manager"),
    EXTERNAL_USER_ADMIN("External User Admin"),
    INTERNAL_USER_VIEWER("Internal User Viewer"),
    EXTERNAL_USER_VIEWER("External User Viewer"),
    SILAS_ADMINISTRATION("SiLAS System Administration");

    private final String roleName;

    AuthzRole(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

    @Override
    public String toString() {
        return roleName;
    }
}
