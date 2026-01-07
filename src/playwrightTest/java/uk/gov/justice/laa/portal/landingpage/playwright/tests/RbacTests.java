package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class RbacTests extends BaseFrontEndTest {


    @Test
    @DisplayName("Internal User Manager Can Access SILAS")
    void verifyInternalUserManagerCanAccessSilas() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.INTERNAL_USER_MANAGER);
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("Internal user manager should not be able to create external user")
    void createUserAndVerifyItAppears() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.INTERNAL_USER_MANAGER);
        assertFalse(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should not be visible for Internal User Manager"
        );
    }

    @Test
    @DisplayName("Internal user manager should not be able to view external user")
    void internalUserManagerCannotViewExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.INTERNAL_USER_MANAGER);
        assertFalse(manageUsersPage.searchAndVerifyUser("playwright-firmusermanager@playwrighttest.com"));
    }

}