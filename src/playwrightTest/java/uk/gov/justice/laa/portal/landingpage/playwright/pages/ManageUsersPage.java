package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageUsersPage {

    private static final Logger log = LoggerFactory.getLogger(ManageUsersPage.class);

    private final Page page;

    // Locators
    private final Locator header;
    private final Locator createANewUserButton;


    public ManageUsersPage(Page page) {
        this.page = page;
        this.header = page.locator("h1.govuk-heading-xl"); // main header
        this.createANewUserButton = page.locator("button.govuk-button[onclick*='/admin/user/create/details']");


    }

    public void assertHeaderVisible() {
        log.debug("Asserting Manage Users page header is visible");
        assertThat(header).isVisible();
        assertThat(header).hasText("Manage your users"); // checks exact text
    }

    public void clickCreateUser() {
        log.debug("Clicking 'Create user' button");
        createANewUserButton.click();
    }

}
