package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUtils;


public class ManageUsersPage {

    private static final Logger log = LoggerFactory.getLogger(ManageUsersPage.class);

    private final Page page;
    private final String url;
    private final int port;


    // Locators
    private final Locator header;
    private final Locator createNewUserButton;
    private final Locator notAuthorisedHeading;

    private final Locator searchInputByName;
    private final Locator searchButton;

    private final Locator emailInput;
    private final Locator firstNameInput;
    private final Locator lastNameInput;

    private final Locator providerUserRadio;
    private final Locator providerAdminRadio;

    private final Locator continueButton;
    private final Locator cancelLink;

    private final Locator multiFirmYesRadio;
    private final Locator multiFirmNoRadio;
    private final Locator continueButtonMultiFirm;

    private final Locator confirmButton;
    private final Locator goBackToManageUsersButton;

    private final Locator firmSearchInput;
    private final Locator firmSearchListbox;
    private final Locator firmOptionRows;
    private final Locator continueButtonFirmSelection;

    private final Locator emailFormatError;
    private final Locator emailDomainError;
    private final Locator firstNameInvalidCharsError;
    private final Locator lastNameInvalidCharsError;
    private final Locator selectUserTypeError;


    public ManageUsersPage(Page page, int port) {
        this.page = page;
        this.port = port;
        this.url = "http://localhost:" + port + "/admin/users";

        log.info("Navigating to Manage Users page: {}", url);
        page.navigate(url);

        this.header = page.locator("h1.govuk-heading-xl");
        this.createNewUserButton = page.locator("button.govuk-button[onclick*='/admin/user/create/details']");
        this.notAuthorisedHeading = page.locator("h1.govuk-heading-l");

        this.searchInputByName = page.locator("input[name='search']");
        this.searchButton = page.locator("button:has-text('Search')");

        this.emailInput = page.locator("input#email");
        this.firstNameInput = page.locator("input#firstName");
        this.lastNameInput = page.locator("input#lastName");

        this.providerUserRadio = page.locator("input#providerUser");
        this.providerAdminRadio = page.locator("input#providerAdmin");

        this.continueButton = page.locator("button.govuk-button:has-text('Continue')");
        this.cancelLink = page.locator("a.govuk-link:has-text('Cancel')");

        this.multiFirmYesRadio = page.locator("input#multiFirmYes");
        this.multiFirmNoRadio = page.locator("input#multiFirmNo");
        this.continueButtonMultiFirm = page.locator("button.govuk-button[type='submit']");

        this.confirmButton = page.locator("button:has-text(\"Create new user\")");
        this.goBackToManageUsersButton = page.locator("button.govuk-button.govuk-button--primary:has-text('Go back to manage users')");

        this.firmSearchInput = page.locator("input#firmSearch");
        this.firmSearchListbox = page.locator("ul#firmSearch__listbox");
        this.firmOptionRows = page.locator("ul#firmSearch__listbox li.autocomplete__option");

        this.continueButtonFirmSelection = page.locator("button.govuk-button:has-text('Continue')");

        this.emailFormatError = page.locator("div.govuk-error-message p:has-text('Enter an email address in the correct format')");
        this.emailDomainError = page.locator("div.govuk-error-message p:has-text('The email address domain is not valid or cannot receive emails.')");
        this.firstNameInvalidCharsError = page.locator("div.govuk-error-message p:has-text('First name must not contain numbers or special characters')");
        this.lastNameInvalidCharsError = page.locator("div.govuk-error-message p:has-text('Last name must not contain numbers or special characters')");
        this.selectUserTypeError = page.locator("div.govuk-error-message p:has-text('Select a user type')");
    }


    // Header check
    public void assertHeaderVisible() {
        assertThat(header).isVisible();
        assertThat(header).hasText("Manage your users");
    }

    // Create user
    public void clickCreateUser() {
        createNewUserButton.click();
    }

    // Unauthorised
    public void verifyNotAuthorisedPage() {
        assertEquals(
                "You're not authorised to access this page",
                notAuthorisedHeading.textContent().trim()
        );
    }

    // Search
    public void searchForUser(String userEmail) {
        searchInputByName.fill(userEmail);
        searchButton.click();
    }

    public void searchAndVerifyUser(String email) {
        searchForUser(email);

        Locator row = page.locator("tbody tr").filter(
                new Locator.FilterOptions().setHasText(email)
        );

        assertThat(row).isVisible();
    }

    public void searchForCurrentUser(TestUser currentUser) {
        searchForUser(currentUser.email);
        Locator row = page.locator("tbody tr").filter(
                new Locator.FilterOptions().setHasText(currentUser.email)
        );
        assertThat(row).isVisible();
    }

    // Add user details
    public String fillInUserDetails(boolean isAdmin) {
        String randomEmail = TestUtils.generateRandomEmail(10);

        emailInput.fill(randomEmail);
        firstNameInput.fill("Test");
        lastNameInput.fill("User");

        if (isAdmin) {
            providerAdminRadio.check();
        } else {
            providerUserRadio.check();
        }

        continueButton.click();
        return randomEmail;
    }

    public void clickContinueUserDetails() {
        continueButton.click();
    }

    public void enterInvalidNameAndVerifyError() {
        emailInput.fill(TestUtils.generateRandomEmail(10));
        firstNameInput.fill("Chr!s123");
        lastNameInput.fill("Test!@");

        clickContinueUserDetails();

        assertTrue(firstNameInvalidCharsError.isVisible());
        assertTrue(lastNameInvalidCharsError.isVisible());
        assertTrue(selectUserTypeError.isVisible());
    }

    // Multi firm
    public void selectMultiFirmAccess(boolean requiresAccess) {
        if (requiresAccess) {
            multiFirmYesRadio.check();
        } else {
            multiFirmNoRadio.check();
        }
        continueButtonMultiFirm.click();
    }

    public void clickConfirmButton() {
        confirmButton.click();
    }

    public void clickGoBackToManageUsers() {
        assertThat(goBackToManageUsersButton).isVisible();
        goBackToManageUsersButton.click();
    }

    // Firm selection
    public void searchAndSelectFirmByCode(String firmCode) {

        firmSearchInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));

        firmSearchInput.fill(firmCode);

        // Let autocomplete populate
        firmSearchListbox.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));

        Locator firmOption = page.locator("li.autocomplete__option")
                .filter(new Locator.FilterOptions().setHasText("Firm code: " + firmCode));

        firmOption.first().click();
    }

    public void clickContinueFirmSelectPage() {
        continueButtonFirmSelection.click();
    }

    // Validation helpers
    public Locator getValidationError(String fieldId, String errorText) {
        return page.locator(
                "#" + fieldId + " >> xpath=ancestor::div[contains(@class,'govuk-form-group')] >> div.govuk-error-message p:has-text('" + errorText + "')"
        );
    }

    public void triggerAndAssertEmailFormatError() {
        emailInput.fill("bad email");
        clickContinueUserDetails();

        assertTrue(emailFormatError.isVisible());
    }

    public void triggerAndAssertEmailDomainError() {
        emailInput.fill("user@invalid-domain");
        clickContinueUserDetails();

        assertTrue(emailDomainError.isVisible());
    }

    public AuditPage goToAuditPage() {
        String auditUrl = "http://localhost:" + port + "/admin/users/audit";

        log.info("Navigating to Audit page: {}", auditUrl);
        page.navigate(auditUrl);

        return new AuditPage(page, port);
    }
}
