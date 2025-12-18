package uk.gov.justice.laa.portal.landingpage.playwright.common;

public enum TestApp {
    MANAGE_YOUR_USERS("Manage Your Users");

    public final String appName;

    TestApp(String appName) {
        this.appName = appName;
    }
}
