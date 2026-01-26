package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUtils;

import java.util.List;


public class ManageUsersPage {

    private static final Logger log = LoggerFactory.getLogger(ManageUsersPage.class);

    private final Page page;
    private final String url;
    private final int port;


    // Locators
    private final Locator header;
    private final Locator createNewUserButton;
    private final Locator confirmNewUserButton;
    private final Locator signOutLink;
    private final Locator signOutConfirmButton;
    private final Locator notAuthorisedHeading;

    private final Locator searchInputByName;
    private final Locator searchButton;

    private final Locator emailInput;
    private final Locator firstNameInput;
    private final Locator lastNameInput;
    private final Locator userFullNameLink;

    private final Locator providerUserRadio;
    private final Locator providerAdminRadio;

    private final Locator continueButton;
    private final Locator cancelLink;

    private final Locator multiFirmYesRadio;
    private final Locator multiFirmNoRadio;
    private final Locator continueButtonMultiFirm;

    private final Locator confirmButton;
    private final Locator goBackToManageYourUsersButton;
    private final Locator manageAccessButton;

    private final Locator deleteUserLink;
    private final Locator confirmAndDeleteUserButton;
    private final Locator deleteUserReason;
    private final Locator deleteUserMessageHeading;

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
        this.signOutLink = page.locator("a:has-text('Sign out')");
        this.signOutConfirmButton = page.locator("button[type='submit']:has-text('Sign out')");

        this.searchInputByName = page.locator("input[name='search']");
        this.searchButton = page.locator("button:has-text('Search')");

        this.emailInput = page.locator("input#email");
        this.firstNameInput = page.locator("input#firstName");
        this.lastNameInput = page.locator("input#lastName");
        this.userFullNameLink = page.locator("a.govuk-link[href*='/admin/users/manage/']");

        this.providerUserRadio = page.locator("input#providerUser");
        this.providerAdminRadio = page.locator("input#providerAdmin");

        this.continueButton = page.locator("button.govuk-button:has-text('Continue')");
        this.manageAccessButton = page.locator("button.govuk-button:has-text('Manage access')");
        this.cancelLink = page.locator("a.govuk-link:has-text('Cancel')");

        this.multiFirmYesRadio = page.locator("input#multiFirmYes");
        this.multiFirmNoRadio = page.locator("input#multiFirmNo");
        this.continueButtonMultiFirm = page.locator("button.govuk-button[type='submit']");

        this.confirmNewUserButton = page.locator("button:has-text(\"Create new user\")");
        this.confirmButton = page.locator("button:has-text(\"Confirm\")");
        this.goBackToManageYourUsersButton = page.locator("a.govuk-button, button.govuk-button").filter(new Locator.FilterOptions().setHasText("Go back to manage your users"));

        this.deleteUserLink = page.locator("a.govuk-link[href*='/admin/users/manage/'][href$='delete']");
        this.confirmAndDeleteUserButton = page.locator("button:has-text(\"Confirm and delete user\")");
        this.deleteUserReason = page.locator("textarea[name='reason']");
        this.deleteUserMessageHeading = page.locator("h1.govuk-panel__title");

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

    // Create user
    public boolean isCreateUserVisible() {
        return createNewUserButton.isVisible();

    }

    public void clickAndConfirmSignOut() {
        signOutLink.click();
        signOutConfirmButton.click();
        var signedOutPage = page.getByText("You're now signed out of your account");
        assertNotNull(signedOutPage, "Failed to find signed out page");
    }

    public void clickManageUser() {
        userFullNameLink.click();
    }

    public void confirmAndDeleteUser() {
        deleteUserLink.click();
        deleteUserReason.fill("reason for deleting user");
        confirmAndDeleteUserButton.click();

        assertEquals(
                "User deleted",
                deleteUserMessageHeading.textContent().trim()
        );
    }


    // Edit user - backwards compatible
    public void clickEditUser() {
        clickFirstUserLink();
    }

    // Clicks the first user link in the table (waits for visibility)
    public void clickFirstUserLink() {
        Locator firstLink = page.locator("a.govuk-link[href*='/admin/users/manage/']").first();
        firstLink.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000));
        firstLink.click();
    }

    public boolean isNextLinkClickable() {
        return page.locator("a.govuk-link:has-text('Next page')").isVisible();
    }

    public void clickNextPageLink() {
        Locator next = page.locator("a.govuk-link:has-text('Next page')");
        if (next.isVisible()) {
            next.click();
        }
    }

    public void clickExternalUserLink(String user) {
        Locator externalUserLink = page
                .locator("a.govuk-link[href*='/admin/users/manage/']")
                .getByText(user);

        for (int attempts = 0; attempts < 50; attempts++) {
            if (externalUserLink.count() > 0 && externalUserLink.first().isVisible()) {
                externalUserLink.first().click();
                return;
            }

            if (isNextLinkClickable()) {
                clickNextPageLink();
            } else {
                break;
            }
        }

        throw new IllegalStateException("Could not find external user link for user: " + user);
    }

    public void clickContinueLink() {
        continueButton.click();
    }

    public void clickManageAccess() {
        manageAccessButton.click();
    }

    public void clickServicesTab() {
        page.locator("a.govuk-tabs__tab[href*='#services']").click();
    }

    public void clickChangeLink() {
        page.locator("#services a.govuk-link:has-text(\"Change\")").click();
    }

    public void verifySelectedUserServices(List<String> roles) {
        for (String role : roles) {
            Locator row = page.locator("dd:has-text('" + role + "')");
            assertThat(row).isVisible();
        }
    }

    public void verifyIsUserDetailsPage() {
        assertTrue(page.url().contains("/admin/users/manage/"));
    }

    public void verifyUserDetailsPopulated() {
        assertTrue(page.locator(".govuk-summary-list__row:has-text(\"Email\") .govuk-summary-list__value").isVisible());
        assertTrue(page.locator(".govuk-summary-list__row:has-text(\"First name\") .govuk-summary-list__value").isVisible());
        assertTrue(page.locator(".govuk-summary-list__row:has-text(\"Last name\") .govuk-summary-list__value").isVisible());
    }

    public void verifyUserDetailsPopulated(String email, String firstName, String lastName, String firmName, String multiFirmAccess) {
        assertRow("Email", email);
        assertRow("First name", firstName);
        assertRow("Last name", lastName);
        assertRow("Firm name", firmName);
        assertRow("Multi-firm access", multiFirmAccess);
    }

    public void clickOfficesTab() {
        page.locator(".govuk-tabs__tab[href*='#offices']").click();
    }

    public void clickOfficeChange() {
        page.locator("#offices .govuk-link:has-text(\"Change\")").click();
    }

    public void checkSelectedOffices(List<String> offices) {

        for (String office : offices) {
            Locator checkbox = page.getByLabel(office);
            if (!checkbox.isChecked()) {
                checkbox.check();
            }
        }
    }

    public void uncheckSelectedOffices(List<String> offices) {

        for (String office : offices) {
            Locator checkbox = page.getByLabel(office);
            if (checkbox.isChecked()) {
                checkbox.uncheck();
            }
        }
    }

    public void checkSelectedRoles(List<String> roles) {
        page.locator("input[type='checkbox']").first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        for (String role : roles) {
            Locator checkbox = page.getByLabel(role);
            if (!checkbox.isChecked()) {
                checkbox.check();
            }
        }
    }

    public void checkSelectedServices(List<String> services) {
        page.locator("input[type='checkbox']").first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        for (String service : services) {
            Locator checkbox = page.getByLabel(service);
            if (!checkbox.isChecked()) {
                checkbox.check();
            }
        }
    }

    public Locator externalUserRowLocator() {
        return page.locator(
                "tr.govuk-table__row:has(td.govuk-table__cell:has-text(\"externaluser-incomplete@playwrighttest.com\"))"
        );

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


    public boolean searchAndVerifyUser(String email) {
        searchForUser(email);

        Locator row = page.locator("tbody tr").filter(
                new Locator.FilterOptions().setHasText(email)
        );

        return row.isVisible();
    }

    public void searchForCurrentUser(TestUser currentUser) {
        searchForUser(currentUser.email);
        Locator row = page.locator("tbody tr").filter(
                new Locator.FilterOptions().setHasText(currentUser.email)
        );
        assertThat(row).isVisible();
    }

    public void searchAndVerifyUserNotExists(String email) {
        searchForUser(email);

        Locator row = page.locator("tbody tr").filter(
                new Locator.FilterOptions().setHasText(email)
        );

        assertThat(row).not().isVisible();
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

    public void clickConfirmNewUserButton() {
        confirmNewUserButton.click();
    }

    public void clickConfirmButton() {
        confirmButton.click();
    }

    public void clickGoBackToManageUsers() {
        assertThat(goBackToManageYourUsersButton).isVisible();
        goBackToManageYourUsersButton.click();
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

    private void assertRow(String key, String value) {
        final var row = page.locator(".govuk-summary-list__row:has(.govuk-summary-list__key:has-text('" + key + "'))");
        assertTrue(row.isVisible());
        assertTrue(row.allInnerTexts().getFirst().contains(value));
    }
}
