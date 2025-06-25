package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;

import static org.junit.jupiter.api.Assertions.*;


public class LoginPageTest extends BaseFrontEndTest {

    @Test
    public void loginPageTest() {
        var landingPage = page.getByText("Legal Aid Agency Portal");
        assertNotNull(landingPage, "Failed to find Legal Aid Agency Portal page");
    }
}
