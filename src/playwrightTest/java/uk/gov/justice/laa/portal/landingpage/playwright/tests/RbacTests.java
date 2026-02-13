package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RbacTests extends BaseFrontEndTest {

    @Test
    @DisplayName("Internal User Manager Can Access SILAS")
    void verifyInternalUserManagerCanAccessSilas() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.INTERNAL_USER_MANAGER);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("Internal user manager should not be able to create external user")
    void internalUserManagerCannotCreateExternalUser() {
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

    // ---------------------------------------------------------------------
    // EXTERNAL USER MANAGER
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("External User Manager can access landing page")
    void externalUserManagerCanAccessLandingPage() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("External User Manager cannot view internal users")
    void externalUserManagerCannotViewInternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        assertFalse(manageUsersPage.searchAndVerifyUser("playwright-internalusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("External User Manager can view external users")
    void externalUserManagerCanViewExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        assertTrue(manageUsersPage.searchAndVerifyUser("playwright-firmusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("External User Manager cannot create external users")
    void externalUserManagerCannotCreateExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        assertFalse(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should not be visible for External User Manager"
        );
    }

    // ---------------------------------------------------------------------
    // EXTERNAL USER ADMIN
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("External User Admin can access landing page")
    void externalUserAdminCanAccessLandingPage() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_ADMIN);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("External User Admin cannot view internal users")
    void externalUserAdminCannotViewInternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_ADMIN);
        assertFalse(manageUsersPage.searchAndVerifyUser("playwright-internalusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("External User Admin can view external users")
    void externalUserAdminCanViewExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_ADMIN);
        assertTrue(manageUsersPage.searchAndVerifyUser("playwright-firmusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("External User Admin can create external users")
    void externalUserAdminCanCreateExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_ADMIN);
        assertTrue(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should be visible for External User Admin"
        );
    }

    // ---------------------------------------------------------------------
    // EXTERNAL USER VIEWER
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("External User Viewer can access SILAS")
    void externalUserViewerCanAccessSilas() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_VIEWER);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("External User Viewer cannot view internal users")
    void externalUserViewerCannotViewInternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_VIEWER);
        assertFalse(manageUsersPage.searchAndVerifyUser("playwright-internalusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("External User Viewer can view external users")
    void externalUserViewerCanViewExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_VIEWER);
        assertTrue(manageUsersPage.searchAndVerifyUser("playwright-firmusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("External User Viewer cannot create external users")
    void externalUserViewerCannotCreateExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_VIEWER);
        assertFalse(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should not be visible for External User Viewer"
        );
    }

    // ---------------------------------------------------------------------
    // INTERNAL USER VIEWER
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Internal User Viewer can access SILAS")
    void internalUserViewerCanAccessSilas() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.INTERNAL_USER_VIEWER);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("Internal User Viewer can view internal users")
    void internalUserViewerCanViewInternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.INTERNAL_USER_VIEWER);
        assertTrue(manageUsersPage.searchAndVerifyUser("playwright-internalusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("Internal User Viewer cannot view external users")
    void internalUserViewerCannotViewExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.INTERNAL_USER_VIEWER);
        assertFalse(manageUsersPage.searchAndVerifyUser("playwright-firmusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("Internal User Viewer cannot create users")
    void internalUserViewerCannotCreateUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.INTERNAL_USER_VIEWER);
        assertFalse(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should not be visible for Internal User Viewer"
        );
    }

    // ---------------------------------------------------------------------
    // FIRM USER MANAGER
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Firm User Manager can access SILAS")
    void firmUserManagerCanAccessSilas() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("Firm User Manager cannot view internal users")
    void firmUserManagerCannotViewInternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);
        assertFalse(manageUsersPage.searchAndVerifyUser("playwright-internalusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("Firm User Manager can view external users")
    void firmUserManagerCanViewExternalUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);
        assertTrue(manageUsersPage.searchAndVerifyUser("playwright-firmusermanager@playwrighttest.com"));
    }

    @Test
    @DisplayName("Firm User Manager cannot create users")
    void firmUserManagerCannotCreateUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);
        assertFalse(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should not be visible for Firm User Manager"
        );
    }

    @Test
    @DisplayName("Firm User Manager can delete single-firm users in same firm")
    void firmUserManagerCanDeleteSingleFirmUserInSameFirm() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);

        // Firm User Manager should be able to view and manage External User Viewer from same firm
        assertTrue(
                manageUsersPage.searchAndVerifyUser("playwright-externaluserviewer@playwrighttest.com"),
                "External User Viewer should be visible to Firm User Manager"
        );
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify the delete button is visible (Firm User Manager has DELETE_EXTERNAL_USER permission)
        assertTrue(
                manageUsersPage.isDeleteUserVisible(),
                "Delete user button should be visible for Firm User Manager when viewing single-firm user in same firm"
        );
    }

    @Test
    @DisplayName("A Firm User Manager can delete a user from the same firm")
    void firmUserManagerCanDeleteUserFromSameFirm() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);

        assertTrue(
                manageUsersPage.searchAndVerifyUser("playwright-externaluseradmin@playwrighttest.com"),
                "External User Admin should be visible to Firm User Manager"
        );

        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        assertTrue(
                manageUsersPage.isDeleteUserVisible(),
                "Delete user button must be visible for Firm User Manager viewing a user from the same firm"
        );

        assertTrue(
                page.locator("a.govuk-link[href*='/admin/users/manage/'][href$='disable']").isVisible(),
                "Disable user button should also be visible for Firm User Manager"
        );
    }

    @Test
    @DisplayName("A Firm User Manager cannot delete a user if they belong to a different firm")
    void firmUserManagerCannotDeleteUserFromDifferentFirm() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);

        // Firm User Manager should NOT be able to see users from different firm

        assertFalse(
                manageUsersPage.searchAndVerifyUser("playwright-firmtwouserviewer@playwrighttest.com"),
                "User from different firm (Firm Two) should NOT be visible to Firm User Manager from Firm One"
        );
    }

}
