package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.HomePage;

public class HomePageTest extends BaseFrontEndTest {

    /**
     * Helper method to login as a given TestUser and return a HomePage object.
     */
    private HomePage loginAndGetHome(TestUser user) {
        loginAs(user.email);
        HomePage homePage = new HomePage(page);
        homePage.assertHeaderLoaded();
        return homePage;
    }

    @Test
    @DisplayName("Header is visible and non-blank")
    void homePageHeaderIsVisible() {
        HomePage home = loginAndGetHome(TestUser.GLOBAL_ADMIN);

        String text = home.getHeaderText();
        assertNotNull(text, "Header text should not be null");
        assertFalse(text.isBlank(), "Header text should not be blank");
        assertEquals("Your legal aid services", text, "Header text should match expected value");
    }

    @Test
    @DisplayName("Manage Users panel is visible with correct text")
    void manageUsersPanelVisible() {
        HomePage home = loginAndGetHome(TestUser.GLOBAL_ADMIN);

        // Assert panel is visible
        home.assertManageUsersPanelVisible();

        String linkText = home.getManageUsersLinkText().replaceAll("\\s+", " ").trim();
        assertEquals("Manage your users", linkText, "Link text should match");
    }

    @Test
    @DisplayName("SiLAS Administration panel is visible for global admin")
    void silasAdministrationPanelVisible() {
        HomePage home = loginAndGetHome(TestUser.GLOBAL_ADMIN);

        // Assert panel is visible
        home.assertSilasAdministrationPanelVisible();

        String linkText = home.getSilasAdministrationLinkText().replaceAll("\\s+", " ").trim();
        assertEquals("SiLAS Administration", linkText, "Link text should match");
    }

    @Test
    @DisplayName("Verify access restriction message appears for user with no roles")
    void verifyAccessRestrictedMessageAppearsForUserWithNoRoles() {
        HomePage home = loginAndGetHome(TestUser.NO_ROLES);
        home.assertAccessRestrictionMessageVisible();
    }

    @Test
    @DisplayName("Verify sign-out works as intended")
    void verifySignOut() {
        HomePage home  = loginAndGetHome(TestUser.GLOBAL_ADMIN);
        home.clickSignOut();
        home.clickConfirmSignOut();
        assertTrue(page.url().contains("logout-success"));
        assertTrue(page.locator(".govuk-panel__title").textContent().contains("You're now signed out of your account"));
        page.goBack();
        assertTrue(page.url().contains("login.microsoftonline.com/"));
    }
}
