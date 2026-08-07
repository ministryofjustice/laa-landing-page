package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

/**
 * Enumeration of Authorization Role names used in the system.
 * These correspond to role names in the app_role table where authz_role = true.
 */
@Getter
public enum AuthzRole {
    GLOBAL_ADMIN("Global Admin", AuthzRoleType.LAA),
    INTERNAL_USER_MANAGER("Internal User Manager", AuthzRoleType.LAA),
    EXTERNAL_USER_MANAGER("External User Manager", AuthzRoleType.LAA),
    FIRM_USER_MANAGER("Firm User Manager", AuthzRoleType.PROVIDER_ADMIN),
    EXTERNAL_USER_ADMIN("External User Admin", AuthzRoleType.LAA),
    INTERNAL_USER_VIEWER("Internal User Viewer", AuthzRoleType.LAA),
    EXTERNAL_USER_VIEWER("External User Viewer", AuthzRoleType.LAA),
    SILAS_ADMINISTRATION("SILAS System Administration", AuthzRoleType.LAA),
    USER_ACCESS_AUDIT_TABLE("User Access Audit Table", AuthzRoleType.LAA),
    FIRM_DIRECTORY("Firm Directory", AuthzRoleType.LAA),
    SECURITY_RESPONSE("Security Response", AuthzRoleType.PRIVILEGED),
    MULTI_FIRM_DELEGATION("Multi-firm Delegation", AuthzRoleType.LAA),
    AUDIT_EXPORT("Audit Export", AuthzRoleType.LAA),
    EXTERNAL_USER_SUPPORT("External User Support", AuthzRoleType.LAA);
    private final String roleName;
    private final AuthzRoleType authzRoleType;

    AuthzRole(String roleName, AuthzRoleType authzRoleType) {
        this.roleName = roleName;
        this.authzRoleType = authzRoleType;
    }

    @Override
    public String toString() {
        return roleName;
    }
}
