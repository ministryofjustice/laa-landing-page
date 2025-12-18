package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestRole;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.AuditPage;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;

import java.util.List;

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
        String email = manageUsersPage.fillInUserDetails(true);
        manageUsersPage.selectMultiFirmAccess(false);
        manageUsersPage.searchAndSelectFirmByCode("90001");
        manageUsersPage.clickContinueFirmSelectPage();
        manageUsersPage.clickConfirmNewUserButton();
        manageUsersPage.clickGoBackToManageUsers();
        manageUsersPage.searchAndVerifyUser(email);
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
        manageUsersPage.searchAndVerifyUser(email);
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
        manageUsersPage.clickFirstUserLink();
        manageUsersPage.clickServicesTab();
        manageUsersPage.verifySelectedUserServices(roles);
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
