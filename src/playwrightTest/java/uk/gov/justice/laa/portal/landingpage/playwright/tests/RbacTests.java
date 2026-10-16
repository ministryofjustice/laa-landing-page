package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
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
    @DisplayName("A Firm User Manager can delete a user from the same firm")
    void firmUserManagerCanDeleteUserFromSameFirm() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);

        assertTrue(
                manageUsersPage.searchAndVerifyUser("playwright-deletetest@playwrighttest.com"),
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
                page.locator("a.govuk-link[href*='/admin/users/manage/'][href*='/deactivate'][href*='referer=manage"
                        + "'][href*='profileId=']").isVisible(),
                "Deactivate user button should also be visible for Firm User Manager"
        );
    }

    @Test
    @DisplayName("A Firm User Manager cannot delete a user if they belong to a different firm")
    void firmUserManagerCannotDeleteUserFromDifferentFirm() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);

        assertFalse(
                manageUsersPage.searchAndVerifyUser("playwright-firmtwouserviewer@playwrighttest.com"),
                "User from different firm (Firm Two) should NOT be visible to Firm User Manager from Firm One"
        );
    }

    @Test
    @DisplayName("A Firm User Manager can see the manage access button")
    void firmUserManagerCanManageAccess() {
        ManageUsersPage globalAdminManageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        final String email =
                globalAdminManageUsersPage.createProviderAdminUserWithNonMultiFirmAccess("90001");

        page.context().clearCookies();
        page.evaluate("() => window.localStorage.clear()");
        page.evaluate("() => window.sessionStorage.clear()");

        ManageUsersPage firmUserManagerManageUsersPage =
                loginAndGetManageUsersPage(TestUser.FIRM_USER_MANAGER);

        assertTrue(
                firmUserManagerManageUsersPage.searchAndVerifyUser(email),
                "Created user should be visible to Firm User Manager"
        );

        firmUserManagerManageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        firmUserManagerManageUsersPage.verifyUserDetailsPopulated();
        firmUserManagerManageUsersPage.verifyManageAccessButtonVisible();
    }

    @Test
    @DisplayName("External User Support can access SILAS")
    void externalUserSupportCanAccessSilas() {
        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
    }


    @Test
    @DisplayName("Global Admin can assign and remove External User Support role")
    void globalAdminCanAssignAndRemoveExternalUserSupportRole() {

        final String targetUserEmail =
                "playwright-rbac-ga-eus-role@playwrighttest.com";

        final String externalUserSupportRole =
                "External User Support";

        final List<String> externalUserSupportServices =
                List.of(
                        "Manage your users",
                        "SILAS System Administration"
                );

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        // Find user with no roles
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "User with no roles should be visible to Global Admin"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        // Open Services and change access
        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();

        // Select services required by External User Support
        manageUsersPage.checkSelectedServices(
                externalUserSupportServices
        );

        manageUsersPage.clickContinueUserDetails();

        // Assign External User Support
        manageUsersPage.checkSelectedRoles(
                List.of(externalUserSupportRole)
        );

        manageUsersPage.clickContinueUserDetails();

        // Confirm assignment
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Confirmation screen
        manageUsersPage.clickGoBackToManageUsers();

        // Re-open user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Updated user should still be visible to Global Admin"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify role was assigned
        manageUsersPage.clickServicesTab();

        manageUsersPage.verifySelectedUserServices(
                List.of(externalUserSupportRole)
        );

        // Open change access again
        manageUsersPage.clickChangeLink();

        // Remove the services that provide External User Support access
        manageUsersPage.uncheckSelectedRoles(
                externalUserSupportServices
        );

        manageUsersPage.clickContinueUserDetails();

        // Confirm removal
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Confirmation screen
        manageUsersPage.clickGoBackToManageUsers();

        // Re-open user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Updated user should still be visible to Global Admin"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify External User Support is no longer assigned
        manageUsersPage.clickServicesTab();

        manageUsersPage.verifyServicesNotPresent(
                List.of(externalUserSupportRole)
        );
    }

    @Test
    @DisplayName("Internal User Manager can assign and remove External User Support role")
    void internalUserManagerCanAssignAndRemoveExternalUserSupportRole() {

        final String targetUserEmail =
                "playwright-rbac-ium-eus-role@playwrighttest.com";

        final String externalUserSupportRole =
                "External User Support";

        final List<String> externalUserSupportServices =
                List.of("Manage your users");

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INTERNAL_USER_MANAGER);

        // Find internal user with no roles
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "User with no roles should be visible to Internal User Manager"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        // Open Services and change access
        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();

        // External User Support sits under Manage your users
        manageUsersPage.checkSelectedServices(
                externalUserSupportServices
        );

        manageUsersPage.clickContinueUserDetails();

        // Assign External User Support
        manageUsersPage.checkSelectedRoles(
                List.of(externalUserSupportRole)
        );

        manageUsersPage.clickContinueUserDetails();

        // Confirm assignment
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Confirmation page
        manageUsersPage.clickGoBackToManageUsers();

        // Re-open user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Updated user should still be visible to Internal User Manager"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify assigned
        manageUsersPage.clickServicesTab();

        manageUsersPage.verifySelectedUserServices(
                List.of(externalUserSupportRole)
        );

        // Change access again
        manageUsersPage.clickChangeLink();

        // Remove Manage your users service to completely remove the role
        manageUsersPage.uncheckSelectedRoles(
                externalUserSupportServices
        );

        manageUsersPage.clickContinueUserDetails();

        // Confirm removal
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Confirmation page
        manageUsersPage.clickGoBackToManageUsers();

        // Re-open user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Updated user should still be visible to Internal User Manager"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify removed
        manageUsersPage.clickServicesTab();

        manageUsersPage.verifyServicesNotPresent(
                List.of(externalUserSupportRole)
        );
    }


    @Test
    @DisplayName("External User Support can access Manage Your Users")
    void externalUserSupportCanAccessManageUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("External User Support can view internal users")
    void externalUserSupportCanViewInternalUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(
                        "playwright-internalusermanager@playwrighttest.com"
                ),
                "Internal user should be visible to External User Support"
        );
    }


    @Test
    @DisplayName("External User Support can view external users")
    void externalUserSupportCanViewExternalUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(
                        "playwright-firmusermanager@playwrighttest.com"
                ),
                "External user should be visible to External User Support"
        );
    }


    @Test
    @DisplayName("External User Support role is visible on user details")
    void externalUserSupportRoleIsVisibleOnUserDetails() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(
                        TestUser.EXTERNAL_USER_SUPPORT.email
                ),
                "External User Support user should be visible"
        );

        manageUsersPage.clickUserLink(
                TestUser.EXTERNAL_USER_SUPPORT.email
        );

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        manageUsersPage.clickServicesTab();

        manageUsersPage.verifySelectedUserServices(
                List.of("External User Support")
        );
    }


    @Test
    @DisplayName("External User Support can deactivate external users")
    void externalUserSupportCanDeactivateExternalUsers() {

        final String externalUserEmail =
                "playwright-deletetest@playwrighttest.com";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(externalUserEmail),
                "External user should be visible to External User Support"
        );

        manageUsersPage.clickUserLink(externalUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        // Verify permission is available
        manageUsersPage.verifyDeactivateUserVisible();

        // Enter the deactivation journey without changing shared test data
        manageUsersPage.clickDeactivateUser();

        manageUsersPage.verifyDeactivateUserReasonPageVisible();
    }


    @Test
    @DisplayName("External User Support can delete external users")
    void externalUserSupportCanDeleteExternalUsers() {

        final String externalUserEmail =
                "playwright-deletetest@playwrighttest.com";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(externalUserEmail),
                "External user should be visible to External User Support"
        );

        manageUsersPage.clickUserLink(externalUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        assertTrue(
                manageUsersPage.isDeleteUserVisible(),
                "Delete user should be available to External User Support"
        );
    }


    @Test
    @DisplayName("External User Support can manage external user access")
    void externalUserSupportCanManageExternalUserAccess() {

        final String firmCode = "90001";

        /*
         * SETUP
         * Global Admin creates a standard external user with no roles.
         *
         * We deliberately do not create the user as a Provider Admin,
         * because users with existing roles show a Change link rather
         * than the Manage Access button.
         */
        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        manageUsersPage.clickCreateUser();

        final String externalUserEmail =
                manageUsersPage.fillInUserDetails(false);

        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode(firmCode);
        manageUsersPage.clickContinueFirmSelectPage();

        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(
                manageUsersPage.searchAndVerifyUser(externalUserEmail),
                "New external user should be visible to Global Admin"
        );

        // Clear Global Admin session
        page.context().clearCookies();
        page.evaluate("() => window.localStorage.clear()");
        page.evaluate("() => window.sessionStorage.clear()");

        /*
         * TEST
         * External User Support should be able to manage access
         * for an external user with no roles.
         */
        manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(externalUserEmail),
                "External user should be visible to External User Support"
        );

        manageUsersPage.clickUserLink(externalUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        manageUsersPage.verifyManageAccessButtonVisible();
    }


    @Test
    @DisplayName("External User Support can access multi-firm conversion")
    void externalUserSupportCanAccessMultiFirmConversion() {

        final String externalUserEmail =
                "playwright-deletetest@playwrighttest.com";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(externalUserEmail),
                "External user should be visible to External User Support"
        );

        manageUsersPage.clickUserLink(externalUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        manageUsersPage.clickConvertToMultiFirm();

        manageUsersPage.assertConvertToMultiFirmPageVisible();
        manageUsersPage.assertConvertToMultiFirmDefaultsToNo();
    }


    @Test
    @DisplayName("External User Support can delegate external user access")
    void externalUserSupportCanDelegateExternalUserAccess() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        manageUsersPage.verifyDelegateAccessButtonVisible();

        manageUsersPage.clickDelegateAccess();

        manageUsersPage.verifyDelegateAccessProfilePageVisible();
    }


    @Test
    @DisplayName("External User Support can access User Audit")
    void externalUserSupportCanAccessUserAudit() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        manageUsersPage.goToAuditPage();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.url().contains("/admin/users/audit"),
                "External User Support should be able to access User Audit"
        );

        assertFalse(
                page.getByText(
                        "You're not authorised to access this page"
                ).isVisible(),
                "External User Support should not receive an unauthorised page"
        );
    }

    @Test
    @DisplayName("External User Support cannot create users")
    void externalUserSupportCannotCreateUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertFalse(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should not be visible for External User Support"
        );
    }

    @Test
    @DisplayName("Global Admin can create external users")
    void globalAdminCanCreateExternalUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        assertTrue(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should be visible for Global Admin"
        );
    }

    @Test
    @DisplayName("Global Admin can view internal users")
    void globalAdminCanViewInternalUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(
                        "playwright-internalusermanager@playwrighttest.com"
                ),
                "Internal user should be visible to Global Admin"
        );
    }

    @Test
    @DisplayName("Global Admin can view external users")
    void globalAdminCanViewExternalUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(
                        "playwright-firmusermanager@playwrighttest.com"
                ),
                "External user should be visible to Global Admin"
        );
    }

    @Test
    @DisplayName("Internal User Manager can view internal users")
    void internalUserManagerCanViewInternalUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INTERNAL_USER_MANAGER);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(
                        "playwright-internalusermanager@playwrighttest.com"
                ),
                "Internal user should be visible to Internal User Manager"
        );
    }

    @Test
    @DisplayName("Security Response can view internal users")
    void securityResponseCanViewInternalUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INFORMATION_AND_ASSURANCE);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(
                        "playwright-internalusermanager@playwrighttest.com"
                ),
                "Internal user should be visible to Security Response"
        );
    }

    @Test
    @DisplayName("Security Response can view external users")
    void securityResponseCanViewExternalUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INFORMATION_AND_ASSURANCE);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(
                        "playwright-firmusermanager@playwrighttest.com"
                ),
                "External user should be visible to Security Response"
        );
    }

    @Test
    @DisplayName("Security Response cannot create users")
    void securityResponseCannotCreateUsers() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INFORMATION_AND_ASSURANCE);

        assertFalse(
                manageUsersPage.isCreateUserVisible(),
                "Create User button should not be visible for Security Response"
        );
    }

    @Test
    @DisplayName("SiLAS Administrator cannot access Manage Users")
    void silasAdministratorCannotAccessManageUsers() {

        loginAndGetManageUsersPage(TestUser.SILAS_ADMINISTRATION);

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.getByText(
                        "You're not authorised to access this page"
                ).isVisible(),
                "SiLAS Administrator should not be authorised to access Manage Users"
        );
    }

    @Test
    @DisplayName("Internal User Manager can assign and remove Internal User Manager role")
    void internalUserManagerCanAssignAndRemoveInternalUserManagerRole() {

        final String targetUserEmail =
                "playwright-rbac-ium-internal-role@playwrighttest.com";

        final String internalUserManagerRole =
                "Internal User Manager";

        final List<String> manageUsersService =
                List.of("Manage your users");

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INTERNAL_USER_MANAGER);

        // Find dedicated internal user with no roles
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Target internal user should be visible to Internal User Manager"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        // Open Services and change access
        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();

        // Select Manage your users
        manageUsersPage.checkSelectedServices(
                manageUsersService
        );

        manageUsersPage.clickContinueUserDetails();

        // Assign Internal User Manager
        manageUsersPage.checkSelectedRoles(
                List.of(internalUserManagerRole)
        );

        manageUsersPage.clickContinueUserDetails();

        // Confirm assignment
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.clickGoBackToManageUsers();

        // Re-open user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Updated internal user should still be visible"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify assigned
        manageUsersPage.clickServicesTab();

        manageUsersPage.verifySelectedUserServices(
                List.of(internalUserManagerRole)
        );

        // Change access again
        manageUsersPage.clickChangeLink();

        // Remove Manage your users access
        manageUsersPage.uncheckSelectedRoles(
                manageUsersService
        );

        manageUsersPage.clickContinueUserDetails();

        // Confirm removal
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.clickGoBackToManageUsers();

        // Re-open user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Updated internal user should still be visible"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify removed
        manageUsersPage.clickServicesTab();

        manageUsersPage.verifyServicesNotPresent(
                List.of(internalUserManagerRole)
        );
    }

    @Test
    @DisplayName("Security Response can remove internal user access")
    void securityResponseCanRemoveInternalUserAccess() {

        final String targetUserEmail =
                "playwright-rbac-security-internal@playwrighttest.com";

        final String internalUserManagerRole =
                "Internal User Manager";

        final List<String> manageUsersService =
                List.of("Manage your users");

        /*
         * SETUP
         * Global Admin gives the dedicated target user Internal User Manager access.
         */
        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Target internal user should be visible to Global Admin"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();

        manageUsersPage.checkSelectedServices(
                manageUsersService
        );

        manageUsersPage.clickContinueUserDetails();

        manageUsersPage.checkSelectedRoles(
                List.of(internalUserManagerRole)
        );

        manageUsersPage.clickContinueUserDetails();

        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.clickGoBackToManageUsers();

        /*
         * Clear Global Admin session.
         */
        page.context().clearCookies();
        page.evaluate("() => window.localStorage.clear()");
        page.evaluate("() => window.sessionStorage.clear()");

        /*
         * TEST
         * Security Response removes the user's access.
         */
        manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INFORMATION_AND_ASSURANCE);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Target internal user should be visible to Security Response"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        manageUsersPage.clickServicesTab();

        // Confirm setup succeeded before testing removal
        manageUsersPage.verifySelectedUserServices(
                List.of(internalUserManagerRole)
        );

        manageUsersPage.clickChangeLink();

        // Remove Manage your users access
        manageUsersPage.uncheckSelectedRoles(
                manageUsersService
        );

        manageUsersPage.clickContinueUserDetails();

        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.clickGoBackToManageUsers();

        // Re-open and verify access has been removed
        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Target internal user should still be visible to Security Response"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.clickServicesTab();

        manageUsersPage.verifyServicesNotPresent(
                List.of(internalUserManagerRole)
        );
    }

    @Test
    @DisplayName("Security Response cannot assign internal user access")
    void securityResponseCannotAssignInternalUserAccess() {

        final String targetUserEmail =
                "playwright-rbac-security-cannot-assign@playwrighttest.com";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INFORMATION_AND_ASSURANCE);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Target internal user should be visible to Security Response"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        assertFalse(
                page.locator(
                        "button.govuk-button:has-text('Manage access')"
                ).isVisible(),
                "Manage access should not be available to Security Response"
        );
    }

    @Test
    @DisplayName("External User Support cannot assign internal user access")
    void externalUserSupportCannotAssignInternalUserAccess() {

        final String targetUserEmail =
                "playwright-rbac-audit-target@playwrighttest.com";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_SUPPORT);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Target internal user should be visible to External User Support"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        assertFalse(
                page.locator(
                        "button.govuk-button:has-text('Manage access')"
                ).isVisible(),
                "Manage access should not be available to External User Support for an internal user"
        );
    }

    @Test
    @DisplayName("Internal User Viewer cannot assign internal user access")
    void internalUserViewerCannotAssignInternalUserAccess() {

        final String targetUserEmail =
                "playwright-rbac-audit-target@playwrighttest.com";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.INTERNAL_USER_VIEWER);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Target internal user should be visible to Internal User Viewer"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        assertFalse(
                page.locator(
                        "button.govuk-button:has-text('Manage access')"
                ).isVisible(),
                "Manage access should not be available to Internal User Viewer"
        );
    }

    @Test
    @DisplayName("External User Viewer cannot manage external user access")
    void externalUserViewerCannotManageExternalUserAccess() {

        final String targetUserEmail =
                "playwright-rbac-external-viewer-target@playwrighttest.com";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_VIEWER);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Target external user should be visible to External User Viewer"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        assertFalse(
                page.locator(
                        "button.govuk-button:has-text('Manage access')"
                ).isVisible(),
                "Manage access should not be available to External User Viewer"
        );
    }

    @Test
    @DisplayName("External User Manager can manage external user access")
    void externalUserManagerCanManageExternalUserAccess() {

        final String targetUserEmail =
                "externaluser-incomplete@playwrighttest.com";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Incomplete external user should be visible to External User Manager"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();

        manageUsersPage.verifyManageAccessButtonVisible();
    }

    @Test
    @DisplayName("External User Admin can manage external user access")
    void externalUserAdminCanManageExternalUserAccess() {

        final String firmCode = "90001";

        // Create a fresh external user with no roles
        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        manageUsersPage.clickCreateUser();

        final String externalUserEmail =
                manageUsersPage.fillInUserDetails(false);

        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode(firmCode);
        manageUsersPage.clickContinueFirmSelectPage();

        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(
                manageUsersPage.searchAndVerifyUser(externalUserEmail),
                "New external user should be visible to Global Admin"
        );

        // Clear Global Admin session
        page.context().clearCookies();
        page.evaluate("() => window.localStorage.clear()");
        page.evaluate("() => window.sessionStorage.clear()");

        // Test as External User Admin
        manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_ADMIN);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(externalUserEmail),
                "External user should be visible to External User Admin"
        );

        manageUsersPage.clickUserLink(externalUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();
        manageUsersPage.verifyManageAccessButtonVisible();
    }

    @Test
    @DisplayName("External User Manager can assign Firm User Manager role")
    void externalUserManagerCanAssignFirmUserManagerRole() {

        final String targetUserEmail =
                "playwright-rbac-eum-fum-role@playwrighttest.com";

        final String service =
                "Manage your users";

        final String role =
                "Firm User Manager";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Incomplete external user should be visible to External User Manager"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();
        manageUsersPage.verifyManageAccessButtonVisible();

        // Start Manage Access journey
        manageUsersPage.clickManageAccess();

        // Manage your users has a single role - Firm User Manager
        manageUsersPage.checkSelectedServices(
                List.of(service)
        );

        manageUsersPage.clickContinueLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        /*
         * Firm User Manager is automatically selected because it is the
         * only role for Manage your users.
         *
         * The journey therefore goes directly to office selection.
         * Access to all offices is already selected for this fixture.
         */
        manageUsersPage.clickContinueLink();

        // Confirm assignment
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.locator(
                        ".govuk-panel__title:has-text('Access and permissions updated')"
                ).isVisible(),
                "Access and permissions updated confirmation should be visible"
        );

        // Verify the role was actually assigned
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Updated external user should still be visible"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.clickServicesTab();

        manageUsersPage.verifySelectedUserServices(
                List.of(role)
        );
    }

    @Test
    @DisplayName("External User Admin can assign Firm User Manager role")
    void externalUserAdminCanAssignFirmUserManagerRole() {

        final String targetUserEmail =
                "playwright-rbac-eua-fum-role@playwrighttest.com";

        final String service =
                "Manage your users";

        final String role =
                "Firm User Manager";

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_ADMIN);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Incomplete external user should be visible to External User Admin"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.verifyUserDetailsPopulated();
        manageUsersPage.verifyManageAccessButtonVisible();

        // Start Manage Access journey
        manageUsersPage.clickManageAccess();

        // Manage your users has a single role - Firm User Manager
        manageUsersPage.checkSelectedServices(
                List.of(service)
        );

        manageUsersPage.clickContinueLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Firm User Manager is auto-selected, so journey moves to offices
        manageUsersPage.clickContinueLink();

        // Confirm assignment
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.locator(
                        ".govuk-panel__title:has-text('Access and permissions updated')"
                ).isVisible(),
                "Access and permissions updated confirmation should be visible"
        );

        // Verify role landed on the user
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(
                manageUsersPage.searchAndVerifyUser(targetUserEmail),
                "Updated external user should still be visible"
        );

        manageUsersPage.clickUserLink(targetUserEmail);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.clickServicesTab();

        manageUsersPage.verifySelectedUserServices(
                List.of(role)
        );
    }

}

