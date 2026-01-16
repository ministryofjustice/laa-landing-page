package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestRole;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.AuditPage;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void createMultiFirmUserAndVerifyItAppears() throws InterruptedException {
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
        manageUsersPage.verifyUserDetailsPopulated();
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
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchForUser("playwright-informationassurance@playwrighttest.com");
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickServicesTab();
        manageUsersPage.verifySelectedUserServices(roles);
    }

    @Test
    @DisplayName("Verify offices tab is populated and exists for an external user")
    void editUserOfficesAndVerify() {

        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickExternalUserLink();
        manageUsersPage.clickOfficesTab();
        manageUsersPage.clickOfficeChange();
        assertTrue(page.url().contains("/admin/users/edit/"));
        List<String> offices = List.of("Automation Office 1, City1, 12345 ()", "Automation Office 2, City2, 23456 ()");
        manageUsersPage.checkSelectedOffices(offices);
        manageUsersPage.clickContinueUserDetails();
        manageUsersPage.clickConfirmButton();
        assertTrue(page.locator(".govuk-panel__title:has-text('User detail updated')").isVisible());
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.clickExternalUserLink();
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
        }
    }

    @Test
    @DisplayName("Verify external user admin/manager can view and edit external user roles")
    void verifyExternalUserEditRoles() {
        List<TestUser> users = List.of(TestUser.EXTERNAL_USER_ADMIN, TestUser.EXTERNAL_USER_MANAGER);
        for (TestUser user : users) {
            ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(user);
            manageUsersPage.clickExternalUserLink();
            manageUsersPage.clickServicesTab();
            manageUsersPage.clickChangeLink();
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickConfirmButton();
            assertTrue(page.url().contains("/confirmation"));
            assertTrue(page.locator(".govuk-panel__title:has-text('User detail updated')").isVisible());
            manageUsersPage.clickGoBackToManageUsers();
        }
    }

    @Test
    @DisplayName("Verify external user admin can view and edit/remove external user offices")
    void verifyExternalUserEditOffices() {
        List<TestUser> users = List.of(TestUser.EXTERNAL_USER_ADMIN, TestUser.EXTERNAL_USER_MANAGER);
        for (TestUser user : users) {
            ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(user);
            manageUsersPage.clickExternalUserLink();
            manageUsersPage.clickOfficesTab();
            manageUsersPage.clickOfficeChange();
            List<String> offices = List.of("Automation Office 1, City1, 12345 ()", "Automation Office 2, City2, 23456 ()");
            manageUsersPage.checkSelectedOffices(offices);
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickConfirmButton();
            manageUsersPage.clickGoBackToManageUsers();
            manageUsersPage.clickExternalUserLink();
            manageUsersPage.clickOfficesTab();
            assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 1, City1, 12345')").isVisible());
            assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 2, City2, 23456')").isVisible());
            manageUsersPage.clickGoBackToManageUsers();
            manageUsersPage.clickExternalUserLink();
            manageUsersPage.clickOfficesTab();
            manageUsersPage.clickOfficeChange();
            List<String> updatedOffices = List.of("Automation Office 1, City1, 12345 ()");
            manageUsersPage.uncheckSelectedOffices(updatedOffices);
            manageUsersPage.clickContinueLink();
            manageUsersPage.clickConfirmButton();
            manageUsersPage.clickGoBackToManageUsers();
            manageUsersPage.clickExternalUserLink();
            manageUsersPage.clickOfficesTab();
            assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 1, City1, 12345')").isHidden());
            assertTrue(page.locator(".govuk-summary-card:has-text('Automation Office 2, City2, 23456')").isVisible());
        }
    }





}
