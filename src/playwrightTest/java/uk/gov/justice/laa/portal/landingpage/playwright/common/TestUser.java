package uk.gov.justice.laa.portal.landingpage.playwright.common;

public enum TestUser {
    GLOBAL_ADMIN("playwright-globaladmin@playwrighttest.com", "Global Admin"),
    FIRM_USER_MANAGER("playwright-firmusermanager@playwrighttest.com", "Firm User Manager"),
    INTERNAL_USER_VIEWER("playwright-internaluserviewer@playwrighttest.com", "Internal User Viewer"),
    INTERNAL_USER_MANAGER("playwright-internalusermanager@playwrighttest.com", "Internal User Manager"),
    EXTERNAL_USER_VIEWER("playwright-externaluserviewer@playwrighttest.com", "External User Viewer"),
    EXTERNAL_USER_MANAGER("playwright-externalusermanager@playwrighttest.com", "External User Manager"),
    EXTERNAL_USER_ADMIN("playwright-externaluseradmin@playwrighttest.com", "External User Admin"),
    SILAS_ADMINISTRATION("playwright-silasadministration@playwrighttest.com", "SiLAS System Administration"),
    INFORMATION_AND_ASSURANCE("playwright-informationassurance@playwrighttest.com", "Information & Assurance"),
    EXTERNAL_USER_FOR_OFFICE_ASSIGNMENT("playwright-extofficeuser@playwrighttest.com", "External Office User"),
    NO_ROLES("playwright-noroles@playwrighttest.com", null);

    public final String email;
    public final String silasRoleLabel;

    TestUser(String email, String silasRoleLabel) {
        this.email = email;
        this.silasRoleLabel = silasRoleLabel;
    }
}
