package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
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
        String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmButton();
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchAndVerifyUser(email);
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

    /**
     * Logs in as the specified user and navigates to the Manage Users page.
     */
    private ManageUsersPage loginAndGetManageUsersPage(TestUser user) {
        loginAs(user.email);
        page.navigate(String.format("http://localhost:%d/admin/users", port));
        return new ManageUsersPage(page, port);
    }
}
