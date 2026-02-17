package uk.gov.justice.laa.portal.landingpage.playwright.pages;
import uk.gov.justice.laa.portal.landingpage.playwright.TestData_ManageAccess;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import java.util.regex.Pattern;

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

    private final Locator manageAccessButton;
    private final Locator grantAccess_heading;
    private final Locator hint;
    private final Locator userDetails;

    //Role access locators
    private final Locator roleAccessHeading;
    private final Locator rolesHint;



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

        this.grantAccess_heading = page.locator("h1.govuk-fieldset__heading");
        this.hint = page.locator("#apps-hint");
        this.manageAccessButton = page.locator("button.govuk-button:has-text('Manage access')");
        this.userDetails = page.locator("a.govuk-tabs__tab[href='#user-details']");

        //firm user manager access
        this.roleAccessHeading =
                page.locator("fieldset.govuk-fieldset legend h1.govuk-fieldset__heading");

        this.rolesHint = page.locator("#roles-hint"); // changed from "#apps-hint"

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
    public String createUserForManageAccess() {
        clickCreateUser();
        String email = fillInUserDetails(true);
        selectMultiFirmAccess(false);
        searchAndSelectFirmByCode("90001");
        clickContinueFirmSelectPage();
        clickConfirmButton();
        clickGoBackToManageUsers();
        return email;
    }
    // Manage access button
    public void manageAccessButton(String email) {
        // Find the row containing the user's email
        searchForUser(email);
        Locator user_link = page.locator("a.govuk-link[href*='/admin/users/manage/']");
        user_link.click();
        assertThat(manageAccessButton).isVisible();
        manageAccessButton.click();
        assertThat(grantAccess_heading).isVisible();
        String headingText = grantAccess_heading.textContent().trim();
        assertTrue(headingText.startsWith("Which of these services will"));
        assertTrue(headingText.contains("need access to?"));
        assertThat(hint).isVisible();
        assertThat(hint).hasText("Select all that apply");
        Locator checkboxLabel = page.locator(
                "label.govuk-label.govuk-checkboxes__label span",
                new Page.LocatorOptions().setHasText("Manage Your Users"));
        assertThat(checkboxLabel).isVisible();
        //checkboxLabel.click();
        goToUserDetailsTab();
        assertThat(continueButton).isVisible();
        continueButton.click();


//        for (String labelText : TestData_ManageAccess.CHECKBOX_LABELS) {
//            Locator checkboxLabel = page.locator(
//                "label.govuk-label.govuk-checkboxes__label span",
//                new Page.LocatorOptions().setHasText(labelText)
//            );
//            assertThat(checkboxLabel).isVisible();
      //  }
    }
    void goToUserDetailsTab() {
        assertThat(cancelLink).isVisible();
        cancelLink.click();
        assertThat(userDetails).isVisible();
        manageAccessButton.click();

    }
    public void roleAccessPage() {
        String headingText = roleAccessHeading.textContent().trim();
        assertTrue(headingText.contains("What roles will Test User have for Manage Your Users?"));
            assertThat(rolesHint).hasText("Select all that apply");
            Locator roleCheckbox = page.locator("input[type='checkbox'][name='roles']");
            Locator checkboxLabel = page.locator("label.govuk-label");
            assertTrue(roleCheckbox.isChecked()); // HTML shows checked="checked" in snippet
            assertThat(checkboxLabel).hasText("Firm User Manager");

            // Checkbox hint
            Locator checkboxHint = page.locator("div.govuk-checkboxes__hint");
            assertThat(checkboxHint).hasText("An external firm user manager");

            // Buttons / links
            assertThat(continueButton).isVisible();
            assertThat(cancelLink).isVisible();
            String cancelHref = cancelLink.getAttribute("href");
            assertTrue(cancelHref != null && cancelHref.contains("/cancel"));
            continueButton.click();
        }

    public void validateOfficesAccessPage() {

        Locator heading = page.locator("h1.govuk-fieldset__heading");
        assertThat(heading).isVisible();
        String headingText = heading.textContent().trim();
        assertTrue(headingText.contains("Which of these offices will"));
        //assertTrue(headingText.contains(expectedUserName));

        Locator officesHint = page.locator("#offices-hint");
        assertThat(officesHint).isVisible();
        assertThat(officesHint).hasText("Select all that apply");

        // "Access to all offices" checkbox + hint
        Locator allOfficesCheckbox = page.locator("input#offices-all[name='offices'][type='checkbox']");
        assertThat(allOfficesCheckbox).isVisible();
        assertTrue(allOfficesCheckbox.isChecked());
        Locator allOfficesHint = page.locator("#all-offices-hint");
        assertThat(allOfficesHint).isVisible();
        assertThat(allOfficesHint).hasText("This will include all current and future offices associated to the firm");

        Locator otherOffices = page.locator(
                "input[type='checkbox'][name='offices']:not([value='ALL'])"
        );
        if (allOfficesCheckbox.isChecked()) {
            allOfficesCheckbox.uncheck();
            for (int i = 0; i < otherOffices.count(); i++) {
                otherOffices.nth(i).check();
            }
        }
        Locator continueBtn = page.locator("button.govuk-button:has-text('Continue')");
        Locator cancelLink = page.locator("a.govuk-link:has-text('Cancel')");
        assertThat(continueBtn).isVisible();
        assertThat(cancelLink).isVisible();
        String cancelHref = cancelLink.getAttribute("href");
        assertTrue(cancelHref != null && cancelHref.contains("/cancel"));
        continueBtn.click();
    }

    public void confirmPage(){
        assertThat(page.locator("h1.govuk-heading-l"))
                        .hasText("Check your answers and confirm");

        Locator form = page.locator("form[action*='check-answers']");
                assertThat(form).isVisible();
                Locator userPermissionsCard = page.locator(
                        "h2.govuk-summary-card__title",
                        new Page.LocatorOptions().setHasText("User permissions")
                ).locator("xpath=ancestor::div[contains(@class,'govuk-summary-card')]");

                assertThat(userPermissionsCard).isVisible();

                // Permission key & value
                assertThat(userPermissionsCard.locator(".govuk-summary-list__key"))
                        .hasText("Manage Your Users");

                assertThat(userPermissionsCard.locator(".govuk-summary-list__value"))
                        .hasText("Firm User Manager");

                // Change link
                assertThat(userPermissionsCard.locator("a.govuk-link",
                        new Locator.LocatorOptions().setHasText("Change")))
                        .isVisible();

                Locator officesCard = page.locator(
                        "h2.govuk-summary-card__title",
                        new Page.LocatorOptions().setHasText("Offices")
                ).locator("xpath=ancestor::div[contains(@class,'govuk-summary-card')]");
]
                assertThat(officesCard).isVisible();

                Locator officeValues = officesCard.locator(".govuk-summary-list__value span");

                if (officeValues.count() < 1) {
                    throw new AssertionError("At least one office must be displayed");
                }

                for (int i = 0; i < officeValues.count(); i++) {
                    String text = officeValues.nth(i).innerText().trim();
                    if (text.isEmpty()) {
                        throw new AssertionError("Office entry text should not be empty");
                    }
                }


                assertThat(officesCard.locator("a.govuk-link",
                        new Locator.LocatorOptions().setHasText("Change")))
                        .isVisible();
                assertThat(page.locator("h2.govuk-heading-m"))
                        .hasText("Confirm access");

                assertThat(page.locator("p.govuk-body"))
                        .containsText("When you confirm");

                // Buttons
                assertThat(page.locator("button[type='submit']"))
                        .hasText("Confirm");

        assertThat(cancelLink)
                .hasAttribute("href",
                        Pattern.compile(".*/admin/users/grant-access/.*/cancel")
                );


    }
        }









