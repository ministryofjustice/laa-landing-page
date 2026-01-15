package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;

import static org.junit.jupiter.api.Assertions.assertNotNull;


public class LoginLogoutPageTest extends BaseFrontEndTest {

    @Test
    public void loginPageTest() {
        var landingPage = page.getByText("Legal Aid Agency Portal");
        assertNotNull(landingPage, "Failed to find Legal Aid Agency Portal page");
    }

    @Test
    public void logoutPageTest() {
        ManageUsersPage manageUsersPage = loginAndGetManageUsersPage(TestUser.GLOBAL_ADMIN);
        manageUsersPage.clickAndConfirmSignOut();
    }
}
