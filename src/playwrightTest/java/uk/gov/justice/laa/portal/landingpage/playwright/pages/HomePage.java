package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Page Object for the post-login Home/Landing page.
 */
public class HomePage {
    private static final Logger log = LoggerFactory.getLogger(HomePage.class);

    private final Page page;
    private final Locator header;
    private final Locator signOutLink;
    private final Locator manageUsersLink;
    private final Locator silasAdministrationLink;
    private final Locator applyForLegalAidLink;
    private final Locator applyForCriminalLegalAidLink;
    private final Locator ccmsLink;
    private final Locator submitCrimeFormLink;
    private final Locator accessRestrictionMessage;



    public HomePage(Page page) {
        this.page = page;
        this.header = page.locator("h1.govuk-heading-xl");
        this.signOutLink = page.locator("a.govuk-header__link:has-text('Sign out')");
        //this.manageUsersDescription = page.locator("div.moj-ticket-panel__content--blue p.govuk-body");
        this.manageUsersDescription  = page.locator("//p[@class='govuk-body' and text()='Manage user access and permissions']");

        this.manageUsersLink = page.locator("a.govuk-link:has-text('Manage your users')");
        this.silasAdministrationLink = page.locator("a.govuk-link:has-text('SiLAS Administration')");
        this.applyForLegalAidLink = page.locator("a.govuk-link:has-text('Apply for civil legal aid')");
        this.applyForCriminalLegalAidLink = page.locator("a.govuk-link:has-text('Apply for criminal legal aid')");
        this.ccmsLink = page.locator("a.govuk-link:has-text('Client and Cost Management System')");
        this.submitCrimeFormLink = page.locator("a.govuk-link:has-text('Submit a crime form')");
        this.accessRestrictionMessage = page.locator("div.govuk-inset-text", new Page.LocatorOptions().setHasText("Your account has been activated. You cannot currently access any services."));


        log.info("HomePage initialised");
    }

    public void assertHeaderLoaded() {
        log.debug("Asserting that header is visible");
        assertThat(header).isVisible();
    }

    public String getHeaderText() {
        String text = header.textContent();
        log.info("Header text retrieved: {}", text);
        return text;
    }

    public void assertManageUsersPanelVisible() {
        PlaywrightAssertions.assertThat(manageUsersLink).isVisible();
    }

    public String getManageUsersLinkText() {
        return manageUsersLink.textContent().trim();
    }

    public void clickManageUsers() {
        log.debug("Clicking 'Manage your users' link");
        manageUsersLink.click();
    }

    public void assertSilasAdministrationPanelVisible() {
        PlaywrightAssertions.assertThat(silasAdministrationLink).isVisible();
    }

    public String getSilasAdministrationLinkText() {
        return silasAdministrationLink.textContent().trim();
    }

    public void clickSilasAdministration() {
        log.debug("Clicking 'SiLAS Administration' link");
        silasAdministrationLink.click();
    }

    public void assertAccessRestrictionMessageVisible() {
        log.debug("Checking that the access restriction message is visible for internal users");
        assertThat(accessRestrictionMessage).isVisible();
    }

    public void clickSignOut() {
        signOutLink.click();
    }

    public void clickConfirmSignOut() {
        page.locator("button[type='submit']:has-text('Sign out')").click();
    }

    public Page getPage() {
        return page;
    }
}
