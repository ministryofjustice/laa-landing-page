package uk.gov.justice.laa.portal.landingpage.playwright.tests;


import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

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
        loginAs(email);
        assertThat(page.getByText("Sorry, but we're having trouble signing you in.")).isVisible();
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
        manageUsersPage.verifyUserDetailsPopulated();
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
        manageUsersPage.searchForUser("playwright-firmtwouserviewer@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
        // Disable link visible
        assertTrue(page.locator("#user-details .govuk-summary-list__actions a.govuk-link:has-text(\"Disable user\")").isVisible());
    }

    @Test
    @DisplayName("Verify Disable User link is not visible for unverified users")
    void verifyUserDetailsPageDonotShowsDisableUserLinkForUnVerifiedUsers() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        manageUsersPage.searchForUser("externaluser-incomplete3@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.verifyUserDetailsPopulated();
        // Disable link visible
        assertFalse(page.locator("#user-details .govuk-summary-list__actions a.govuk-link:has-text(\"Disable user\")").isVisible());
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
        assertTrue(page.locator(".govuk-panel__title:has-text('Access and permissions updated')").isVisible());
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

        String userEmail = "playwright-informationassurance@playwrighttest.com";

        // First, add services to ensure they exist
        manageUsersPage.searchForUser(userEmail);
        manageUsersPage.clickFirstUserLink();

        assertTrue(
                page.url().contains("/admin/users/manage/"),
                "User details page should be displayed"
        );

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

        assertTrue(
                page.locator(".govuk-panel__title:has-text('Access and permissions updated')").isVisible(),
                "Access and permissions updated confirmation should be displayed after adding roles"
        );

        // Now remove some services
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchForUser(userEmail);
        manageUsersPage.clickFirstUserLink();

        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();
        manageUsersPage.clickContinueFirmSelectPage();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        List<String> rolesToRemove = List.of(
                TestRole.INTERNAL_USER_MANAGER.roleName,
                TestRole.EXTERNAL_USER_VIEWER.roleName
        );

        // Verify the roles exist, are selected, and are enabled before removing them
        for (String role : rolesToRemove) {
            Locator roleCheckbox = page.locator(
                    "//div[contains(@class, 'govuk-checkboxes__item')]"
                            + "[.//label//span[normalize-space()='" + role + "']]"
                            + "//input[@type='checkbox' and @name='roles']"
            );

            roleCheckbox.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(10000));

            assertTrue(
                    roleCheckbox.count() > 0,
                    role + " checkbox should exist on the page"
            );

            assertTrue(
                    roleCheckbox.isChecked(),
                    role + " checkbox should be selected before removal"
            );

            assertTrue(
                    roleCheckbox.isEnabled(),
                    role + " checkbox should be enabled so it can be removed"
            );

            roleCheckbox.uncheck();

            assertFalse(
                    roleCheckbox.isChecked(),
                    role + " checkbox should be unchecked after removal"
            );
        }

        manageUsersPage.clickContinueUserDetails();
        manageUsersPage.clickConfirmButton();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.locator(".govuk-panel__title:has-text('Access and permissions updated')").isVisible(),
                "Access and permissions updated confirmation should be displayed after removing roles"
        );

        // Verify the services were removed
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchForUser(userEmail);
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickServicesTab();

        manageUsersPage.verifyServicesNotPresent(rolesToRemove);

        // Verify remaining service is still present
        List<String> remainingRoles = List.of(
                TestRole.EXTERNAL_USER_MANAGER.roleName
        );

        manageUsersPage.verifySelectedUserServices(remainingRoles);
    }

    @Test
    @DisplayName("Verify offices tab is populated and exists for an external user")
    void editUserOfficesAndVerify() {

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        manageUsersPage.searchForUser(
                "playwright-firmusermanager@playwrighttest.com"
        );

        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickOfficesTab();
        manageUsersPage.clickOfficeChange();

        assertTrue(
                page.url().contains("/admin/users/edit/"),
                "User office edit page should be displayed"
        );

        final List<String> officeAccountNumbers = List.of(
                "THREE",
                "FOUR"
        );

        manageUsersPage.checkSelectedOffices(officeAccountNumbers);
        manageUsersPage.clickContinueUserDetails();
        manageUsersPage.clickConfirmButton();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.locator(
                        ".govuk-panel__title:has-text('Access and permissions updated')"
                ).isVisible(),
                "Access and permissions updated confirmation should be displayed"
        );

        manageUsersPage.clickGoBackToManageUsers();

        manageUsersPage.searchForUser(
                "playwright-firmusermanager@playwrighttest.com"
        );

        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickOfficesTab();

        assertTrue(
                page.locator(".govuk-table__header")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Office Address"))
                        .isVisible(),
                "Office Address heading should be displayed"
        );

        assertTrue(
                page.locator(".govuk-table__header")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Office account number"))
                        .isVisible(),
                "Office account number heading should be displayed"
        );

        assertTrue(
                page.locator(".govuk-summary-card")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Automation Office 1, City1, 12345"))
                        .isVisible(),
                "Automation Office 1 should be displayed"
        );

        assertTrue(
                page.locator(".govuk-summary-card")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Automation Office 2, City2, 23456"))
                        .isVisible(),
                "Automation Office 2 should be displayed"
        );
    }

    @Test
    @DisplayName("Deleted provider admin user is recorded in the audit screen")
    void deleteUserAndVerifyInAudit() {

        // Create new user
        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        manageUsersPage.clickCreateUser();

        final String email =
                manageUsersPage.fillInUserDetails(true);

        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(manageUsersPage.searchAndVerifyUser(email));

        // Delete and confirm newly created user
        manageUsersPage.clickManageUser();
        manageUsersPage.confirmAndDeleteUser();

        // Verify user no longer exists in Manage Users
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchAndVerifyUserNotExists(email);

        // Open the audit page
        Page page = manageUsersPage.getPage();
        AuditPage auditPage = new AuditPage(page, port);

        // Open deleted users
        auditPage.clickViewAllDeletedUsers();
        auditPage.assertDeletedUsersPageDisplayed();

        // Search for deleted user
        auditPage.searchForDeletedUser(email);

        // Verify deleted user appears in the audit table
        auditPage.assertDeletedUserDisplayed(email);
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
            manageUsersPage.clickConfirmButton();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            assertTrue(page.url().contains("/confirmation"));
            assertTrue(page.locator(".govuk-panel__title:has-text('Access and permissions updated')").isVisible());
            manageUsersPage.clickGoBackToManageUsers();
            manageUsersPage.clickAndConfirmSignOut();
        }
    }

    @Test
    @DisplayName("Verify external user admin can view and edit/remove external user offices")
    void verifyExternalUserEditOffices() {
        final List<TestUser> users = List.of(
                TestUser.EXTERNAL_USER_ADMIN,
                TestUser.EXTERNAL_USER_MANAGER
        );

        final List<String> officeAccountNumbers = List.of(
                "THREE",
                "FOUR"
        );

        final List<String> officeAccountNumbersToRemove = List.of(
                "THREE"
        );

        for (TestUser user : users) {
            ManageUsersPage manageUsersPage =
                    loginAndGetManageUsersPage(user);

            // Add both offices
            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickOfficesTab();
            manageUsersPage.clickOfficeChange();

            manageUsersPage.checkSelectedOffices(officeAccountNumbers);
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickConfirmButton();

            manageUsersPage.clickGoBackToManageUsers();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // Verify both offices were added
            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickOfficesTab();

            assertTrue(
                    page.locator(".govuk-summary-card")
                            .filter(new Locator.FilterOptions()
                                    .setHasText("Automation Office 1, City1, 12345"))
                            .isVisible(),
                    "Automation Office 1 should be displayed"
            );

            assertTrue(
                    page.locator(".govuk-summary-card")
                            .filter(new Locator.FilterOptions()
                                    .setHasText("Automation Office 2, City2, 23456"))
                            .isVisible(),
                    "Automation Office 2 should be displayed"
            );

            // Return and remove Office 1
            manageUsersPage.clickGoBackToManageUsers();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickOfficesTab();
            manageUsersPage.clickOfficeChange();

            manageUsersPage.uncheckSelectedOffices(
                    officeAccountNumbersToRemove
            );

            manageUsersPage.clickContinueLink();
            manageUsersPage.clickConfirmButton();

            manageUsersPage.clickGoBackToManageUsers();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // Verify Office 1 was removed and Office 2 remains
            manageUsersPage.clickExternalUserLink("Playwright FirmUserManager");
            manageUsersPage.clickOfficesTab();

            assertTrue(
                    page.locator(".govuk-summary-card")
                            .filter(new Locator.FilterOptions()
                                    .setHasText("Automation Office 1, City1, 12345"))
                            .isHidden(),
                    "Automation Office 1 should no longer be displayed"
            );

            assertTrue(
                    page.locator(".govuk-summary-card")
                            .filter(new Locator.FilterOptions()
                                    .setHasText("Automation Office 2, City2, 23456"))
                            .isVisible(),
                    "Automation Office 2 should remain displayed"
            );

            manageUsersPage.clickAndConfirmSignOut();
        }
    }

    @Test
    @DisplayName("Verify External User Manager can Manage Access for incomplete users")
    public void verifyExternalUserManagerIncompleteUsers() {
        final String firmCode = "90001";
        final String service = "Test LAA App Four";
        final String role = "Test LAA App Four Role One Access";
        final String officeAccountNumber = "THREE";

        ManageUsersPage globalAdminManageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        final String email =
                globalAdminManageUsersPage.createProviderAdminUserWithNonMultiFirmAccess(firmCode);

        page.context().clearCookies();
        page.evaluate("() => window.localStorage.clear()");
        page.evaluate("() => window.sessionStorage.clear()");

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "Created incomplete user should be visible to External User Manager"
        );

        manageUsersPage.assertStatusVisible("INCOMPLETE");

        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.locator(".govuk-button:has-text('Manage Access')").isVisible(),
                "Manage Access button should be visible for incomplete user"
        );

        manageUsersPage.clickManageAccess();

        // Select service
        manageUsersPage.checkSelectedServices(List.of(service));
        manageUsersPage.clickContinueLink();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Select role
        manageUsersPage.checkSelectedRoles(List.of(role));
        manageUsersPage.clickContinueLink();

        // Continue through firm selection
        manageUsersPage.clickContinueLink();
        manageUsersPage.checkSelectedOffices(List.of("Automation Office 1, City1, 12345 (Office account number: THREE)"));
        manageUsersPage.clickContinueLink();

        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.locator(
                        ".govuk-panel__title:has-text('Access and permissions updated')"
                ).isVisible(),
                "Access and permissions updated confirmation should be visible"
        );

        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.refreshUntilStatusVisible(
                email,
                "ACTIVATION PENDING"
        );
    }

    private void openExternalUser(ManageUsersPage manageUsersPage, String userName) {
        manageUsersPage.clickExternalUserLink(userName);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    private void assertAccessUpdated() {
        assertTrue(page.locator(".govuk-panel__title:has-text('Access and permissions updated')").isVisible());
    }

    @Test
    @DisplayName("Verify External User Manager can Manage Access for incomplete users (Single Role App)")
    public void verifyExternalUserManagerIncompleteUsersSkipRoleSelectionForSingleRoleApp() {
        String userName = "Playwright ExternalUserIncompleteTwo";
        final List<String> services = List.of("Test LAA App One");

        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);

        openExternalUser(manageUsersPage, userName);
        assertTrue(page.locator(".govuk-button:has-text('Manage Access')").isVisible());
        manageUsersPage.assertStatusVisible("INCOMPLETE");

        manageUsersPage.clickManageAccess();
        manageUsersPage.checkSelectedServices(services);
        manageUsersPage.clickContinueLink();
        manageUsersPage.clickContinueLink();
        manageUsersPage.clickConfirmButton();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertAccessUpdated();

        loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        openExternalUser(manageUsersPage, userName);
        manageUsersPage.assertStatusVisible("ACTIVATION PENDING");
    }

    @Disabled("Test disabled - user creation logic changed. Users with only firm selection get COMPLETE status instead of PENDING. Needs investigation.")
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

    @Test
    @DisplayName("Global Admin can use Manage Access for user without roles")
    void globalAdminCanUseManageAccessForUserWithoutRoles() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(false);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(manageUsersPage.searchAndVerifyUser(email));
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.locator(".govuk-button:has-text('Manage Access')").isVisible(),
                "Manage Access button should be visible for user without roles"
        );

        manageUsersPage.clickServicesTab();
        assertTrue(
                page.locator("#services p:has-text('No services are currently assigned to this user')").isVisible(),
                "Should show that no services are assigned"
        );

        assertFalse(
                page.locator("#services .govuk-link:has-text('Change')").isVisible(),
                "Change link should NOT be visible for user without roles"
        );
    }

    @Test
    @DisplayName("Provider Admin with default roles shows Change link not Manage Access button")
    void providerAdminWithDefaultRolesShowsChangeLink() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(manageUsersPage.searchAndVerifyUser(email));
        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        //Verify Manage Access button is not visible
        manageUsersPage.clickServicesTab();
        assertTrue(
                page.locator("#services .govuk-link:has-text('Change')").isVisible(),
                "Change link should be visible in Services tab for Provider Admin with default roles"
        );

        assertTrue(
                page.locator("#services dd:has-text('Firm User Manager')").isVisible(),
                "Provider Admin should have Firm User Manager role assigned by default"
        );
    }

    @Test
    @DisplayName("Non-Provider Admin without roles shows Manage Access button not Change link")
    void nonProviderAdminWithoutRolesShowsManageAccessButton() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(false);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(manageUsersPage.searchAndVerifyUser(email));

        manageUsersPage.clickFirstUserLink();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(page.locator(".govuk-button:has-text('Manage Access')").isVisible(),
                "Manage Access button should be visible for user without roles");

        manageUsersPage.clickServicesTab();

        assertFalse(page.locator("#services .govuk-link:has-text('Change')").isVisible(),
                "Change link should not be visible in Services tab for user without roles");

        assertTrue(page.locator("#services p:has-text('No services are currently assigned to this user')").isVisible(),
                "No assigned services message should be displayed");
    }

    @Test
    @DisplayName("Verify status updates once roles are removed from complete user")
    public void verifyStatusUpdatesOnceRolesRemovedFromCompleteUser() {
        final String userName = "Playwright FirmTwoUserViewer";
        final String service = "Manage your users";

        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);

        manageUsersPage.clickExternalUserLink(userName);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.assertStatusVisible("COMPLETE");
        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();

        Locator serviceCheckbox = page.getByLabel(service);
        serviceCheckbox.uncheck();
        assertFalse(serviceCheckbox.isChecked(), service + " checkbox should be unchecked after removal");

        manageUsersPage.clickContinueUserDetails();
        manageUsersPage.clickConfirmButton();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        assertTrue(page.locator(".govuk-panel__title:has-text('Access and permissions updated')").isVisible());

        loginAndGetManageUsersPage(TestUser.EXTERNAL_USER_MANAGER);
        manageUsersPage.clickExternalUserLink(userName);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        manageUsersPage.assertStatusVisible("NO ROLES ASSIGNED");
    }

    @Test
    @DisplayName("Convert an existing user to multi-firm access")
    void convertExistingUserToMultiFirmAccess() throws InterruptedException {
        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        final String email = "externaluser-incomplete2@playwrighttest.com";

        manageUsersPage.searchForUser(email);
        manageUsersPage.clickUserLink(email);
        manageUsersPage.assertUserNotConvertedToMultiFirm();
        manageUsersPage.clickConvertToMultiFirm();
        manageUsersPage.assertConvertToMultiFirmPageVisible();
        manageUsersPage.assertConvertToMultiFirmDefaultsToNo();
        manageUsersPage.selectConvertToMultiFirm(true);
        manageUsersPage.clickConfirmButton();
        manageUsersPage.assertMultiFirmConversionSuccessful();
    }

    @Test
    @DisplayName("Do not convert an existing user to multi-firm access when No is selected")
    void doNotConvertExistingUserToMultiFirmAccess() {
        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        final String email = "externaluser-incomplete3@playwrighttest.com";

        manageUsersPage.searchForUser(email);
        manageUsersPage.clickUserLink(email);
        manageUsersPage.assertUserNotConvertedToMultiFirm();
        manageUsersPage.clickConvertToMultiFirm();
        manageUsersPage.assertConvertToMultiFirmPageVisible();
        manageUsersPage.selectConvertToMultiFirm(false);
        manageUsersPage.clickConfirmButton();
        manageUsersPage.assertUserNotConvertedToMultiFirm();
    }

    @Test
    @DisplayName("Create a non-multi-firm user and assign roles and offices using Manage Access")
    void createNonMultiFirmUserAndAssignRolesAndOffices() {
        final String firmCode = "90001";
        final String service = "Test LAA App Four";
        final String role = "Test LAA App Four Role One Access";

        final List<String> officeAccountNumbers = List.of(
                "THREE",
                "FOUR"
        );

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        // Create a standard provider user without default admin roles
        manageUsersPage.clickCreateUser();
        final String email = manageUsersPage.fillInUserDetails(false);

        // Create as non-multi-firm and associate the user with a firm
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode(firmCode);
        manageUsersPage.clickContinueFirmSelectPage();

        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();

        // Verify the newly created user appears
        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "New non-multi-firm user should appear in Manage Users"
        );

        // Open the newly created user
        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.assertMultiFirmAccess("No");
        manageUsersPage.verifyManageAccessButtonVisible();
        manageUsersPage.clickManageAccess();

        // Select service
        manageUsersPage.checkSelectedServices(List.of(service));
        manageUsersPage.clickContinueLink();

        // Select role
        manageUsersPage.checkSelectedRoles(List.of(role));
        manageUsersPage.clickContinueLink();

        // Select offices using the new office account number locator
        manageUsersPage.checkSelectedOffices(officeAccountNumbers);
        manageUsersPage.clickContinueLink();

        // Confirm the access assignment
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                page.locator(
                        ".govuk-panel__title:has-text('Access and permissions updated')"
                ).isVisible(),
                "Access and permissions updated confirmation should be displayed"
        );

        // Reopen the updated user
        manageUsersPage.clickGoBackToManageUsers();

        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "Updated user should still appear in Manage Users"
        );

        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify assigned role
        manageUsersPage.clickServicesTab();
        manageUsersPage.verifySelectedUserServices(List.of(role));

        // Verify assigned offices
        manageUsersPage.clickOfficesTab();

        assertTrue(
                page.locator(".govuk-summary-card")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Automation Office 1, City1, 12345"))
                        .isVisible(),
                "Automation Office 1 should be assigned"
        );

        assertTrue(
                page.locator(".govuk-summary-card")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Automation Office 2, City2, 23456"))
                        .isVisible(),
                "Automation Office 2 should be assigned"
        );
    }

    @Test
    @DisplayName("Delegate firm access to a newly created multi-firm user")
    void delegateFirmAccessToNewMultiFirmUser() {
        final String firmCode = "90001";

        final List<String> services = List.of(
                "Test LAA App Four"
        );

        final List<String> roles = List.of(
                "Test LAA App Four Role One Access"
        );

        final List<String> offices = List.of(
                "THREE"
        );

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        final String email =
                manageUsersPage.createMultiFirmUserAndDelegateAccess(
                        firmCode,
                        services,
                        roles,
                        offices
                );

        assertTrue(
                page.locator(".govuk-panel__title")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Setup complete"))
                        .isVisible(),
                "Setup complete confirmation should be displayed"
        );

        assertTrue(
                page.locator(".govuk-panel__body")
                        .filter(new Locator.FilterOptions()
                                .setHasText("access and permissions have been added"))
                        .isVisible(),
                "Access and permissions confirmation message should be displayed"
        );

        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "Multi-firm user should appear after firm access has been delegated"
        );

        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.assertMultiFirmAccess("Yes");

        manageUsersPage.clickServicesTab();
        manageUsersPage.verifySelectedUserServices(roles);

        manageUsersPage.clickOfficesTab();

        assertTrue(
                page.locator(".govuk-summary-card")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Automation Office 1, City1, 12345"))
                        .isVisible(),
                "Delegated office should be displayed against the user"
        );
    }

    @Test
    @DisplayName("Revoked multi-firm user can log in but has no delegated service access")
    void revokeDelegatedFirmAccessFromMultiFirmUser() {
        final String firmCode = "90001";

        final List<String> services = List.of(
                "Test LAA App Four"
        );

        final List<String> roles = List.of(
                "Test LAA App Four Role One Access"
        );

        final List<String> offices = List.of(
                "THREE"
        );

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        // Create the multi-firm user and delegate access to the firm
        final String email =
                manageUsersPage.createMultiFirmUserAndDelegateAccess(
                        firmCode,
                        services,
                        roles,
                        offices
                );

        // Verify delegated access was set up successfully
        assertTrue(
                page.locator(".govuk-panel__title")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Setup complete"))
                        .isVisible(),
                "Setup complete confirmation should be displayed"
        );

        assertTrue(
                page.locator(".govuk-panel__body")
                        .filter(new Locator.FilterOptions()
                                .setHasText("access and permissions have been added"))
                        .isVisible(),
                "Access and permissions confirmation message should be displayed"
        );

        // Return to Manage Your Users
        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify the delegated profile exists
        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "The delegated multi-firm user should appear in Manage Users"
        );

        // Open the delegated profile
        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify the account remains a multi-firm user
        manageUsersPage.assertMultiFirmAccess("Yes");

        // Open the revoke access journey
        manageUsersPage.verifyRevokeAccessLinkVisible();
        manageUsersPage.clickRevokeAccess();

        // Verify the revoke confirmation page
        manageUsersPage.verifyRevokeAccessConfirmationPageVisible();

        // Confirm access should be revoked
        manageUsersPage.selectRevokeAccessYes();
        manageUsersPage.confirmRevokeAccess();

        // Verify revocation completed successfully
        manageUsersPage.verifyAccessRevokedSuccessfully();

        // Verify the revoked firm profile is no longer listed
        manageUsersPage.searchAndVerifyUserNotExists(email);

        // Sign out as Global Admin
        manageUsersPage.clickAndConfirmSignOut();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Log in as the revoked multi-firm user
        loginAs(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify the account can still log in but has no active firm access
        manageUsersPage.verifyAwaitingFirmAccessMessage();
    }

    @Test
    @DisplayName("Filter Manage Users to display multi-firm users only")
    void filterManageUsersByThirdPartyUsers() {
        final String firmCode = "90001";

        final List<String> services = List.of(
                "Test LAA App Four"
        );

        final List<String> roles = List.of(
                "Test LAA App Four Role One Access"
        );

        final List<String> offices = List.of(
                "THREE"
        );

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        // Create a standard non-multi-firm user
        final String nonMultiFirmEmail =
                manageUsersPage.createProviderAdminUserWithNonMultiFirmAccess(
                        firmCode
                );

        // Create a multi-firm user and delegate access to the same firm
        final String multiFirmEmail =
                manageUsersPage.createMultiFirmUserAndDelegateAccess(
                        firmCode,
                        services,
                        roles,
                        offices
                );

        // Return to Manage Your Users
        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Apply the 3rd Party filter
        manageUsersPage.selectThirdPartyUserFilter();

        assertTrue(
                page.url().contains("showMultiFirmUsers=true"),
                "The 3rd Party filter should be applied"
        );

        // Verify all users displayed are third-party users
        manageUsersPage.verifyOnlyThirdPartyUsersDisplayed();

        // Verify the newly created multi-firm user is returned
        assertTrue(
                manageUsersPage.searchAndVerifyUser(multiFirmEmail),
                "The multi-firm user should appear when the 3rd Party filter is selected"
        );

        assertTrue(
                page.url().contains("showMultiFirmUsers=true"),
                "The 3rd Party filter should remain applied after searching"
        );

        // Search again using the same filtered form.
        // fill() replaces the previous email, so no separate clear is required.
        manageUsersPage.searchAndVerifyUserNotExists(nonMultiFirmEmail);

        assertTrue(
                page.url().contains("showMultiFirmUsers=true"),
                "The 3rd Party filter should remain applied when searching for a non-multi-firm user"
        );
    }

    @Test
    @DisplayName("Change the assigned role for a multi-firm user")
    void changeAssignedRoleForMultiFirmUser() {
        final String firmCode = "90001";

        final List<String> services = List.of(
                "Test LAA App Four"
        );

        final List<String> initialRoles = List.of(
                "Test LAA App Four Role One Access"
        );

        final List<String> updatedRoles = List.of(
                "Test LAA App Four Role Two Access"
        );

        final List<String> offices = List.of(
                "THREE"
        );

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        // Create a multi-firm user with initial access
        final String email =
                manageUsersPage.createMultiFirmUserAndDelegateAccess(
                        firmCode,
                        services,
                        initialRoles,
                        offices
                );

        // Return to Manage Your Users
        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Find and open the created user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "The created multi-firm user should appear in Manage Users"
        );

        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Confirm the user is multi-firm
        manageUsersPage.assertMultiFirmAccess("Yes");

        // Open services and begin editing
        manageUsersPage.clickServicesTab();
        manageUsersPage.clickChangeLink();

        // Services screen
        manageUsersPage.clickContinueUserDetails();

        // Roles screen
        manageUsersPage.uncheckSelectedRoles(initialRoles);
        manageUsersPage.checkSelectedRoles(updatedRoles);
        manageUsersPage.clickContinueUserDetails();

        // Now on Check your answers
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Return to Manage Your Users
        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Reopen the updated user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "The updated multi-firm user should still appear in Manage Users"
        );

        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify the new role is present
        manageUsersPage.clickServicesTab();
        manageUsersPage.verifySelectedUserServices(updatedRoles);

        // Verify the old role is gone
        manageUsersPage.verifyServicesNotPresent(initialRoles);
    }

    @Test
    @DisplayName("Change the assigned office for a multi-firm user")
    void changeAssignedOfficeForMultiFirmUser() {
        final String firmCode = "90001";

        final List<String> services = List.of(
                "Test LAA App Four"
        );

        final List<String> roles = List.of(
                "Test LAA App Four Role One Access"
        );

        final List<String> initialOffices = List.of(
                "THREE"
        );

        final List<String> updatedOffices = List.of(
                "FOUR"
        );

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        // Create a multi-firm user with initial office access
        final String email =
                manageUsersPage.createMultiFirmUserAndDelegateAccess(
                        firmCode,
                        services,
                        roles,
                        initialOffices
                );

        // Return to Manage Your Users
        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Find and open the created user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "The created multi-firm user should appear in Manage Users"
        );

        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Confirm the user is multi-firm
        manageUsersPage.assertMultiFirmAccess("Yes");

        // Open the Offices tab and begin editing
        manageUsersPage.clickOfficesTab();
        manageUsersPage.clickOfficeChange();

        // Replace the existing office
        manageUsersPage.uncheckSelectedOffices(initialOffices);
        manageUsersPage.checkSelectedOffices(updatedOffices);
        manageUsersPage.clickContinueUserDetails();

        // Now on Check your answers
        manageUsersPage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Return to Manage Your Users
        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Reopen the updated user
        assertTrue(
                manageUsersPage.searchAndVerifyUser(email),
                "The updated multi-firm user should still appear in Manage Users"
        );

        manageUsersPage.clickUserLink(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify the updated office
        manageUsersPage.clickOfficesTab();

        manageUsersPage.verifySelectedUserOffices(updatedOffices);
        manageUsersPage.verifyOfficesNotPresent(initialOffices);
    }

    @Test
    @DisplayName("Multi-firm user can log in after access is delegated")
    void multiFirmUserCanLogInAfterAccessIsDelegated() {
        final String firmCode = "90001";

        final List<String> services = List.of(
                "Test LAA App Four"
        );

        final List<String> roles = List.of(
                "Test LAA App Four Role One Access"
        );

        final List<String> offices = List.of(
                "THREE"
        );

        ManageUsersPage manageUsersPage =
                loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);

        // Create the multi-firm user and delegate access
        final String email =
                manageUsersPage.createMultiFirmUserAndDelegateAccess(
                        firmCode,
                        services,
                        roles,
                        offices
                );

        // Sign out as Global Admin
        manageUsersPage.clickGoBackToManageUsers();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        manageUsersPage.clickAndConfirmSignOut();

        // Log in using the newly created multi-firm user's email
        loginAs(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Verify the user lands on the home page
        assertTrue(
                page.url().endsWith("/home"),
                "The multi-firm user should land on the home page"
        );

        // Verify the allocated LAA service is visible
        assertThat(
                page.getByRole(
                        AriaRole.LINK,
                        new Page.GetByRoleOptions()
                                .setName("Test LAA App Four")
                )
        ).isVisible();

        // Verify Manage Your Users is not displayed
        assertThat(
                page.getByText(
                        "Manage your users",
                        new Page.GetByTextOptions().setExact(true)
                )
        ).not().isVisible();
    }


}
