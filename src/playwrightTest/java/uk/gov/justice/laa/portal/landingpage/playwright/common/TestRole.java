package uk.gov.justice.laa.portal.landingpage.playwright.common;

public enum TestRole {
    GLOBAL_ADMIN("Global Admin"),
    FIRM_USER_MANAGER("Firm User Manager"),
    INTERNAL_USER_VIEWER("Internal User Viewer"),
    INTERNAL_USER_MANAGER("Internal User Manager"),
    EXTERNAL_USER_VIEWER("External User Viewer"),
    EXTERNAL_USER_MANAGER("External User Manager"),
    EXTERNAL_USER_ADMIN("External User Admin");

    public final String roleName;

    TestRole(String roleName) {
        this.roleName = roleName;
    }
}
