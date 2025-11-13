package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.HomePage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HomePageTest extends BaseFrontEndTest {

    private HomePage home;

    @BeforeEach
    void init() {
        // BaseFrontEndTest already logs in once per class and leaves us on Home.

        home = new HomePage(page);
        // Minimal stability check so each test starts from a good state
        home.assertHeaderLoaded();
    }

    @Test
    @DisplayName("Header is visible and non-blank")
    void homePageHeaderIsVisible() {
        String text = home.getHeaderText();
        assertNotNull(text, "Header text should not be null");
        assertFalse(text.isBlank(), "Header text should not be blank");
        assertEquals("Your legal aid services", text, "Header text should match expected value");
    }

    @Test
    @DisplayName("Manage Users panel is visible with correct text")
    void manageUsersPanelVisible() {
        // Assert panel elements are visible
        home.assertManageUsersPanelVisible();

        String linkText = home.getManageUsersLinkText().replaceAll("\\s+", " ").trim();
        assertEquals("Manage your users", linkText, "Link text should match");

        String descriptionText = home.getManageUsersDescriptionText();
        assertEquals("Manage user access and permissions", descriptionText, "Description text should match");

    }

    @Test
    @DisplayName("Click 'Manage your users' link and verify navigation")
    void navigateToManageUsers()  {
        home.clickManageUsers();
        Assert.assertEquals("URL should match the Manage Users page",
                home.getPage().url(), "http://localhost:" + port + "/admin/users");
    }
}



