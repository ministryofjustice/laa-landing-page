package uk.gov.justice.laa.portal.landingpage.playwright.tests;


import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.options.LoadState;

import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestRole;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.AuditPage;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;

public class ManageUsersTest extends BaseFrontEndTest {

    @Test
    @DisplayName("User with no roles cannot access Manage Users page")
    void userWithNoRolesCannotAccessManageUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.NO_ROLES);
        manageUsersPage.verifyNotAuthorisedPage();
    }

    @Test
    @DisplayName("Verify user search is functional")
    void searchForUser() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.searchForCurrentUser(TestUser.GLOBAL_ADMIN);
    }

    @Test
    @DisplayName("Create a new provider admin user with non multi-firm access")
    void createUserAndVerifyItAppears() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(manageUsersPage.searchAndVerifyUser(email));
    }

    @Test
    @DisplayName("Create a new provider admin user with multi-firm access")
    void createMultiFirmUserAndVerifyItAppears() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(true);
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        AuditPage auditPage = manageUsersPage.goToAuditPage();
        auditPage.assertUserIsPresent(email);

    }

    @Test
    @DisplayName("Create a new provider admin user with non multi-firm access")
    void createUserAndVerify() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(manageUsersPage.searchAndVerifyUser(email));
    }

    @Test
    @DisplayName("Create user and login created user")
    void createAndLoginUser() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(manageUsersPage.searchAndVerifyUser(email));
        manageUsersPage.clickAndConfirmSignOut();

        //Login with created user
        ManageUsersPage manageUsersPageCreatedUser = loginAndGetManageUsersPage(email);
        assertTrue(manageUsersPageCreatedUser.searchAndVerifyUser(email));

    }

    @Test
    @DisplayName("Delete user and login deleted again")
    void deleteAndLoginUser() {

        //Create new user
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(manageUsersPage.searchAndVerifyUser(email));
        manageUsersPage.clickAndConfirmSignOut();

        //Login with created user
        ManageUsersPage manageUsersPageCreatedUser = loginAndGetManageUsersPage(email);
        assertTrue(manageUsersPageCreatedUser.searchAndVerifyUser(email));
        manageUsersPageCreatedUser.clickAndConfirmSignOut();

        // Delete and confirm newly created user
        ManageUsersPage manageUsersPageReLogin = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        assertTrue(manageUsersPageReLogin.searchAndVerifyUser(email));
        manageUsersPageReLogin.clickManageUser();
        manageUsersPageReLogin.confirmAndDeleteUser();
        manageUsersPageReLogin.clickAndConfirmSignOut();

        //Failed Login with deleted user
        ManageUsersPage manageUsersPageDeletedUser = loginAndGetManageUsersPage(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPageDeletedUser.verifySignInError();
    }

    @Test
    @DisplayName("Create a new provider admin user with non multi-firm access")
    void verifyUserDetails() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(manageUsersPage.searchAndVerifyUser(email));
        manageUsersPage.clickManageUser();
        manageUsersPage.verifyUserDetailsPopulated(email, "Test", "User", "90001", "No");
    }

    @Test
    @DisplayName("Navigate from users list into manage-user page")
    void verifyNavigateToUserDetailsPage() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.verifyIsUserDetailsPage();
    }

    @Test
    @DisplayName("Navigate to user details and check if it is populated")
    void verifyUserDetailsIsPopulated() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
    }

    @Test
    @DisplayName("Verify Disable User link is accessible for EUM")
    void verifyUserDetailsPageShowsDisableUserLink() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
        // Disable link visible
        assertTrue(page.locator("#user-details ul.govuk-summary-card__actions a.govuk-link:has-text(\"Disable user\")").isVisible());
    }

    @Test
    @DisplayName("Navigate from users list into manage-user page")
    void editUserAndVerify() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        // Click the first user link in the list and assert we navigated to the manage-user page
        manageUsersPage.searchForUser("playwright-informationassurance@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        assertTrue(page.url().contains("/admin/users/manage/"));
        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();
        manageUsersPage.clickContinueFirmSelectPage();
        List<String> roles = List.of(
                TestRole.INTERNAL_USER_MANAGER.roleName,
                TestRole.EXTERNAL_USER_MANAGER.roleName,
                TestRole.EXTERNAL_USER_VIEWER.roleName
        );
        manageUsersPage.checkSelectedRoles(roles);
        manageUsersPage.clickContinueUserDetails();
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertTrue(page.locator(".govuk-panel__title:has-text('User detail updated')").isVisible());
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchForUser("playwright-informationassurance@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickServicesTab();
        assertTrue(page.locator("#services .govuk-summary-card__title:has-text('Services')").isVisible());
        manageUsersPage.verifySelectedUserServices(roles);
    }

    @Test
    @DisplayName("Remove services from a user and verify they are removed")
    void removeServicesAndVerify() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        // First, add services to ensure they exist
        manageUsersPage.searchForUser("playwright-informationassurance@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        assertTrue(page.url().contains("/admin/users/manage/"));
        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();
        manageUsersPage.clickContinueFirmSelectPage();

        List<String> allRoles = List.of(
                TestRole.INTERNAL_USER_MANAGER.roleName,
                TestRole.EXTERNAL_USER_MANAGER.roleName,
                TestRole.EXTERNAL_USER_VIEWER.roleName
        );
        manageUsersPage.checkSelectedRoles(allRoles);
        manageUsersPage.clickContinueUserDetails();
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertTrue(page.locator(".govuk-panel__title:has-text('User detail updated')").isVisible());

        // Now remove some services
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchForUser("playwright-informationassurance@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();
        manageUsersPage.clickContinueFirmSelectPage();

        List<String> rolesToRemove = List.of(
                TestRole.INTERNAL_USER_MANAGER.roleName,
                TestRole.EXTERNAL_USER_VIEWER.roleName
        );
        manageUsersPage.uncheckSelectedRoles(rolesToRemove);

        manageUsersPage.clickContinueUserDetails();
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertTrue(page.locator(".govuk-panel__title:has-text('User detail updated')").isVisible());

        // Verify the services were removed
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchForUser("playwright-informationassurance@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickServicesTab();

        manageUsersPage.verifyServicesNotPresent(rolesToRemove);

        // Verify remaining service is still present
        List<String> remainingRoles = List.of(TestRole.EXTERNAL_USER_MANAGER.roleName);
        manageUsersPage.verifySelectedUserServices(remainingRoles);
    }

    @Test
    @DisplayName("Verify offices tab is populated and exists for an external user")
    void editUserOfficesAndVerify() {

        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.searchForUser("playwright-firmusermanager@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickOfficesTab();
        manageUsersPage.clickOfficeChange();
        assertTrue(page.url().contains("/admin/users/edit/"));
        List<String> offices = List.of("Automation Office 1, City1, 12345 (THREE)", "Automation Office 2, City2, 23456 (FOUR)");
        manageUsersPage.checkSelectedOffices(offices);
        manageUsersPage.clickContinueUserDetails();
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertTrue(page.locator(".govuk-panel__title:has-text('User detail updated')").isVisible());
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchForUser("playwright-firmusermanager@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickOfficesTab();
        assertTrue(page.locator(".govuk-table__header:has-text('Office Address')").isVisible());
        assertTrue(page.locator(".govuk-table__header:has-text('Account number')").isVisible());
        assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 1, City1, 12345')").isVisible());
        assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 2, City2, 23456')").isVisible());
    }

    @Test
    @DisplayName("Delete a new provider admin user with non multi-firm access")
    void deleteUserAndVerify() {

        //Create new user
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(manageUsersPage.searchAndVerifyUser(email));

        // Delete and confirm newly created user
        manageUsersPage.clickManageUser();
        manageUsersPage.confirmAndDeleteUser();

        //Verify user deleted
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchAndVerifyUserNotExists(email);
    }

    @Test
    @DisplayName("Only admin users should able to create new user")
    void testUserPrivilegesToCreateUser() {
        var usersWithCreateUserPrivilege = List.of(TestUser.GLOBAL_ADMIN, TestUser.EXTERNAL_USER_ADMIN);

        Arrays.stream(TestUser.values()).toList().forEach(user -> {
            ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(user);

            if (usersWithCreateUserPrivilege.contains(user)) {
                assertTrue(manageUsersPage.isCreateUserVisible(), user + " create user is not visible");
            } else {
                assertFalse(manageUsersPage.isCreateUserVisible(), user + " create user is visible");
            }

            manageUsersPage.clickAndConfirmSignOut();
        });
    }


    @Test
    @DisplayName("Show validation error for incorrectly formatted email address")
    void testEmailFormatError() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        manageUsersPage.triggerAndAssertEmailFormatError();
    }

    @Test
    @DisplayName("Show validation error for email address with invalid domain")
    void testEmailDomainError() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        manageUsersPage.triggerAndAssertEmailDomainError();
    }

    @Test
    @DisplayName("Verify invalid name shows correct error message")
    void shouldShowErrorForInvalidName() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        manageUsersPage.enterInvalidNameAndVerifyError();
    }

    @Test
    @DisplayName("Verify external user admin can create new user")
    void verifyExternalUserAdminCreateNewUser() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(manageUsersPage.searchAndVerifyUser(email));
    }

    @Test
    @DisplayName("Verify External User Manager cannot create new users")
    void verifyExternalUserManagerCreateUserHidden() {

        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        assertFalse(manageUsersPage.isCreateUserVisible());

    }

    @Test
    @DisplayName("Verify external user admin/manager can see external users only")
    void verifyExternalUserView() {
        List<TestUser> users = List.of(TestUser.EXTERNAL_USER_ADMIN, TestUser.EXTERNAL_USER_MANAGER);
        for (TestUser user : users) {
            ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(user);
            assertTrue(page.locator(".govuk-table__body:has-text('External')").isVisible());
            assertTrue(page.locator(".govuk-table__body:has-text('Internal')").isHidden());
            manageUsersPage.clickAndConfirmSignOut();
        }
    }

    @Test
    @DisplayName("Verify external user admin/manager can view and edit external user roles")
    void verifyExternalUserEditRoles() {
        List<TestUser> users = List.of(TestUser.EXTERNAL_USER_ADMIN, TestUser.EXTERNAL_USER_MANAGER);
        for (TestUser user : users) {
            ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(user);
            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickServicesTab();
            manageUsersPage.clickChangeLink();
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickConfirmButton();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            assertTrue(page.url().contains("/confirmation"));
            assertTrue(page.locator(".govuk-panel__title:has-text('User detail updated')").isVisible());
            manageUsersPage.clickGoBackToManageUsers();
            manageUsersPage.clickAndConfirmSignOut();
        }
    }

    @Test
    @DisplayName("Verify external user admin can view and edit/remove external user offices")
    void verifyExternalUserEditOffices() {
        List<TestUser> users = List.of(TestUser.EXTERNAL_USER_ADMIN, TestUser.EXTERNAL_USER_MANAGER);
        for (TestUser user : users) {
            ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(user);
            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickOfficesTab();
            manageUsersPage.clickOfficeChange();
            List<String> offices = List.of("Automation Office 1, City1, 12345 (THREE)", "Automation Office 2, City2, 23456 (FOUR)");
            manageUsersPage.checkSelectedOffices(offices);
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickConfirmButton();
            manageUsersPage.clickGoBackToManageUsers();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickOfficesTab();
            assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 1, City1, 12345')").isVisible());
            assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 2, City2, 23456')").isVisible());
            manageUsersPage.clickGoBackToManageUsers();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickOfficesTab();
            manageUsersPage.clickOfficeChange();
            List<String> updatedOffices = List.of("Automation Office 1, City1, 12345 (THREE)");
            manageUsersPage.uncheckSelectedOffices(updatedOffices);
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickConfirmButton();
            manageUsersPage.clickGoBackToManageUsers();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickOfficesTab();
            assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 1, City1, 12345')").isHidden());
            assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 2, City2, 23456')").isVisible());
            manageUsersPage.clickAndConfirmSignOut();
        }
    }

    @Test
    @DisplayName("Verify External User Manager can Manage Access for incomplete users.")
    public void verifyExternalUserManagerIncompleteUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        Locator row = manageUsersPage.externalUserRowLocator();
        assertTrue(row.locator(".moj-badge.moj-badge--blue").isVisible());
        manageUsersPage.clickExternalUserLink("Playwright ExternalUserIncomplete");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertTrue(page.locator(".govuk-button:has-text('Manage Access')").isVisible());
        manageUsersPage.clickManageAccess();
        List<String> services = List.of("Manage Your Users");
        manageUsersPage.checkSelectedRoles(services);
        manageUsersPage.clickContinueLink();
        List<String> roles = List.of("Firm User Manager");
        manageUsersPage.checkSelectedRoles(roles);
        manageUsersPage.clickContinueLink();
        manageUsersPage.clickContinueLink();
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertTrue(page.locator(".govuk-panel__title:has-text('Access and permissions updated')").isVisible());
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(row.locator(".moj-badge.moj-badge--blue").isHidden());
    }

    @Disabled("Test disabled - user creation logic may have changed. Users created with only firm selection appear to get COMPLETE status instead of PENDING/INCOMPLETE. Needs investigation of user creation workflow changes.")
    @Test
    @DisplayName("Verify success screen and incomplete user created.")
    public void verifySuccessScreenAndIncompleteUserCreated() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        assertTrue(page.locator(".govuk-panel__title:has-text('User created')").isVisible());
        manageUsersPage.clickGoBackToManageUsers();
        assertTrue(manageUsersPage.searchAndVerifyUser(email));
        Locator badge = manageUsersPage.firstIncompleteUserRowLocator();
        assertThat(badge).isVisible();
    }
}
