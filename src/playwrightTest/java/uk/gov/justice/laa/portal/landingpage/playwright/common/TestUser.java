package uk.gov.justice.laa.portal.landingpage.playwright.common;

public enum TestUser {
    GLOBAL_ADMIN("playwright-globaladmin@playwrighttest.com"),
    FIRM_USER_MANAGER("playwright-firmusermanager@playwrighttest.com"),
    INTERNAL_USER_VIEWER("playwright-internaluserviewer@playwrighttest.com"),
    INTERNAL_USER_MANAGER("playwright-internalusermanager@playwrighttest.com"),
    EXTERNAL_USER_VIEWER("playwright-externaluserviewer@playwrighttest.com"),
    EXTERNAL_USER_MANAGER("playwright-externalusermanager@playwrighttest.com"),
    EXTERNAL_USER_ADMIN("playwright-externaluseradmin@playwrighttest.com"),
    NO_ROLES("playwright-noroles@playwrighttest.com");

    public final String email;

    TestUser(String email) {
        this.email = email;
    }
}