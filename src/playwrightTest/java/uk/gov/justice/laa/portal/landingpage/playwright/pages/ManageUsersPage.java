package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import java.util.List;

import com.microsoft.playwright.options.AriaRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;



import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;

import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUtils;


public class ManageUsersPage {

    private static final Logger log = LoggerFactory.getLogger(ManageUsersPage.class);

    private final Page page;
    private final String url;
    private final int port;


    // Page and navigation
    private final Locator header;
    private final Locator notAuthorisedHeading;
    private final Locator signOutLink;
    private final Locator signOutConfirmButton;
    private final Locator goBackToManageYourUsersButton;
    private final Locator awaitingFirmAccessMessage;

    // Manage users search and filters
    private final Locator searchInputByName;
    private final Locator searchButton;
    private final Locator userFullNameLink;
    private final Locator thirdPartyFilterCheckbox;

    // Create user
    private final Locator createNewUserButton;
    private final Locator emailInput;
    private final Locator firstNameInput;
    private final Locator lastNameInput;
    private final Locator providerUserRadio;
    private final Locator providerAdminRadio;
    private final Locator confirmNewUserButton;

    // Search and filters
    private final Locator searchUsersHeading;
    private final Locator searchUsersHint;


    private final Locator toggleFiltersButton;
    private final Locator filterPanel;
    private final Locator filtersHeading;

    private final Locator userTypeHeading;
    private final Locator providerUserFilter;
    private final Locator providerAdminFilter;
    private final Locator thirdPartyUserFilter;

    private final Locator userStatusHeading;
    private final Locator noRolesAssignedFilter;
    private final Locator activationPendingFilter;
    private final Locator completeStatusFilter;

    private final Locator applyFiltersButton;

    // Common controls
    private final Locator continueButton;
    private final Locator confirmButton;
    private final Locator cancelLink;

    // Multi-firm selection
    private final Locator multiFirmYesRadio;
    private final Locator multiFirmNoRadio;
    private final Locator continueButtonMultiFirm;

    // Firm selection
    private final Locator firmSearchInput;
    private final Locator firmSearchListbox;
    private final Locator firmOptionRows;
    private final Locator continueButtonFirmSelection;

    // Manage access
    private final Locator manageAccessButton;

    // Delete user
    private final Locator deleteUserLink;
    private final Locator confirmAndDeleteUserButton;
    private final Locator deleteUserReasonRadioFirst;
    private final Locator deleteUserMessageHeading;

    // Validation errors
    private final Locator emailFormatError;
    private final Locator emailDomainError;
    private final Locator firstNameInvalidCharsError;
    private final Locator lastNameInvalidCharsError;
    private final Locator selectUserTypeError;

    // Convert to multi-firm
    private final Locator convertToMultiFirmLink;
    private final Locator convertToMultiFirmYesRadio;
    private final Locator convertToMultiFirmNoRadio;
    private final Locator multiFirmConversionSuccessAlert;
    private final Locator multiFirmUserNotification;
    private final Locator viewAllFirmsLink;

    // Delegate access
    private final Locator delegateAccessButton;
    private final Locator delegateAccessHeading;
    private final Locator delegateAccessEmailInput;
    private final Locator delegateAccessContinueButton;
    private final Locator delegateAccessFirmSelectionHeading;

    // Revoke access
    private final Locator revokeAccessLink;
    private final Locator revokeAccessHeading;
    private final Locator revokeAccessYesRadio;
    private final Locator revokeAccessNoRadio;
    private final Locator revokeAccessConfirmButton;
    private final Locator revokeAccessSuccessBanner;
    private final Locator revokeAccessSuccessMessage;

    public ManageUsersPage(Page page, int port) {
        this.page = page;
        this.port = port;
        this.url = "http://localhost:" + port + "/admin/users";

        log.info("Navigating to Manage Users page: {}", url);
        page.navigate(url);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Page and navigation
        this.header = page.locator("h1.govuk-heading-xl");
        this.notAuthorisedHeading = page.locator("h1.govuk-heading-l");
        this.signOutLink = page.locator("a:has-text('Sign out')");
        this.signOutConfirmButton =
                page.locator("button[type='submit']:has-text('Sign out')");

        this.goBackToManageYourUsersButton =
                page.locator("a.govuk-button, button.govuk-button")
                        .filter(new Locator.FilterOptions()
                                .setHasText("Go back to manage your users"));
        this.awaitingFirmAccessMessage = page.locator(".govuk-inset-text")
                .filter(new Locator.FilterOptions()
                        .setHasText("Your firm admin is working on giving you access to the services you need"));

        // Manage users search and filters
        this.searchInputByName =
                page.locator("input#search[type='search']");

        this.searchButton =
                page.locator("button:has-text('Search')");

        this.userFullNameLink =
                page.locator("a.govuk-link[href*='/admin/users/manage/']");

        this.thirdPartyFilterCheckbox =
                page.locator("#showMultiFirmUsers");



        // Search and filters
        this.searchUsersHeading = page.getByRole(
                AriaRole.HEADING,
                new Page.GetByRoleOptions()
                        .setName("Search users")
                        .setExact(true)
        );

        this.searchUsersHint = page.locator(".search-card .govuk-hint")
                .filter(new Locator.FilterOptions()
                        .setHasText(
                                "You can search by user name, email, firm name, or firm code."
                        ));

        this.toggleFiltersButton = page.locator("#toggle-filters-btn");

        this.filterPanel = page.locator("#filter-panel");

        this.filtersHeading = filterPanel.getByText(
                "Filters",
                new Locator.GetByTextOptions()
                        .setExact(true)
        );

        this.userTypeHeading = filterPanel.getByText(
                "User Type",
                new Locator.GetByTextOptions()
                        .setExact(true)
        );

        this.providerUserFilter = page.locator("#showProviderUsers");

        this.providerAdminFilter = page.locator("#showFirmAdmins");

        this.thirdPartyUserFilter = page.locator("#showMultiFirmUsers");

        this.userStatusHeading = filterPanel.getByText(
                "User Status",
                new Locator.GetByTextOptions()
                        .setExact(true)
        );

        this.noRolesAssignedFilter =
                page.locator("#status-NO_ROLES_ASSIGNED");

        this.activationPendingFilter =
                page.locator("#status-ACTIVATION_PENDING");

        this.completeStatusFilter =
                page.locator("#status-COMPLETE");

        this.applyFiltersButton = filterPanel.getByRole(
                AriaRole.BUTTON,
                new Locator.GetByRoleOptions()
                        .setName("Apply filters")
                        .setExact(true)
        );


        // Create user
        this.createNewUserButton =
                page.locator(
                        "button.govuk-button[onclick*='/admin/user/create/details']"
                );

        this.emailInput = page.locator("input#email");
        this.firstNameInput = page.locator("input#firstName");
        this.lastNameInput = page.locator("input#lastName");

        this.providerUserRadio =
                page.locator("input#providerUser");

        this.providerAdminRadio =
                page.locator("input#providerAdmin");

        this.confirmNewUserButton =
                page.locator("button:has-text(\"Create new user\")");

        // Common controls
        this.continueButton =
                page.locator("button.govuk-button:has-text('Continue')");

        this.confirmButton =
                page.locator("button:has-text(\"Confirm\")");

        this.cancelLink =
                page.locator("a.govuk-link:has-text('Cancel')");

        // Multi-firm selection
        this.multiFirmYesRadio =
                page.locator("input#multiFirmYes");

        this.multiFirmNoRadio =
                page.locator("input#multiFirmNo");

        this.continueButtonMultiFirm =
                page.locator("button.govuk-button[type='submit']");

        // Firm selection
        this.firmSearchInput =
                page.locator("input#firmSearch");

        this.firmSearchListbox =
                page.locator("ul#firmSearch__listbox");

        this.firmOptionRows =
                page.locator(
                        "ul#firmSearch__listbox li.autocomplete__option"
                );

        this.continueButtonFirmSelection =
                page.locator("button.govuk-button:has-text('Continue')");

        // Manage access
        this.manageAccessButton =
                page.locator(
                        "button.govuk-button:has-text('Manage access')"
                );

        // Delete user
        this.deleteUserLink =
                page.locator(
                        "a.govuk-link[href*='/admin/users/manage/'][href$='delete']"
                );

        this.confirmAndDeleteUserButton =
                page.locator(
                        "button:has-text(\"Confirm and delete user\")"
                );

        this.deleteUserReasonRadioFirst =
                page.locator("input[name='reasonId']").first();

        this.deleteUserMessageHeading =
                page.locator("h1.govuk-panel__title");

        // Validation errors
        this.emailFormatError =
                page.locator(
                        "div.govuk-error-message:has-text('Enter an email address in the correct format')"
                );

        this.emailDomainError =
                page.locator(
                        "div.govuk-error-message:has-text('The email address domain is not valid or cannot receive emails.')"
                );

        this.firstNameInvalidCharsError =
                page.locator(
                        "div.govuk-error-message:has-text('First name must not contain numbers or special characters')"
                );

        this.lastNameInvalidCharsError =
                page.locator(
                        "div.govuk-error-message:has-text('Last name must not contain numbers or special characters')"
                );

        this.selectUserTypeError =
                page.locator(
                        "div.govuk-error-message:has-text('Select a user type')"
                );

        // Convert to multi-firm
        this.convertToMultiFirmLink =
                page.locator(
                        "a.govuk-link[href*='/convert-to-multi-firm']"
                ).filter(
                        new Locator.FilterOptions()
                                .setHasText("Convert to multi-firm")
                );

        this.convertToMultiFirmYesRadio =
                page.locator("input#convertToMultiFirmYes");

        this.convertToMultiFirmNoRadio =
                page.locator("input#convertToMultiFirmNo");

        this.multiFirmConversionSuccessAlert =
                page.locator(".moj-alert--success")
                        .filter(
                                new Locator.FilterOptions()
                                        .setHasText(
                                                "User has been successfully converted to a multi-firm user"
                                        )
                        );

        this.multiFirmUserNotification =
                page.locator("p.govuk-body")
                        .filter(
                                new Locator.FilterOptions()
                                        .setHasText(
                                                "This user is set up as a multi-firm user."
                                        )
                        );

        this.viewAllFirmsLink =
                page.locator(".govuk-summary-list__row")
                        .filter(
                                new Locator.FilterOptions()
                                        .setHasText("Multi-firm access")
                        )
                        .locator(
                                "a.govuk-link[href*='/admin/users?search=']"
                        )
                        .filter(
                                new Locator.FilterOptions()
                                        .setHasText("View all firms for")
                        );

        // Delegate access
        this.delegateAccessButton =
                page.locator(
                        "button.govuk-button[onclick=\"location.href='/admin/multi-firm/user/add/profile'\"]"
                );

        this.delegateAccessHeading =
                page.locator("h1.govuk-heading-l")
                        .filter(
                                new Locator.FilterOptions()
                                        .setHasText(
                                                "Giving access to third-party users"
                                        )
                        );

        this.delegateAccessEmailInput =
                page.locator("input#email");

        this.delegateAccessContinueButton =
                page.locator(
                        "form[action='/admin/multi-firm/user/add/profile'] button[type='submit']"
                );

        this.delegateAccessFirmSelectionHeading =
                page.locator("h1.govuk-heading-l")
                        .filter(
                                new Locator.FilterOptions()
                                        .setHasText(
                                                "Select the user's firm"
                                        )
                        );

        // Revoke access
        this.revokeAccessLink =
                page.locator(
                        "a.govuk-link[href*='/admin/multi-firm/user/delete-profile/']"
                );

        this.revokeAccessHeading =
                page.locator("h1.govuk-heading-l")
                        .filter(
                                new Locator.FilterOptions()
                                        .setHasText(
                                                "Are you sure you want to revoke access"
                                        )
                        );

        this.revokeAccessYesRadio =
                page.locator("#confirm-yes");

        this.revokeAccessNoRadio =
                page.locator("#confirm-no");

        this.revokeAccessConfirmButton =
                page.locator(
                        "form[action*='/admin/multi-firm/user/delete-profile/'] button[type='submit']"
                );

        this.revokeAccessSuccessBanner =
                page.locator(
                        ".govuk-notification-banner[role='alert']"
                );

        this.revokeAccessSuccessMessage =
                page.locator(
                        ".govuk-notification-banner__heading"
                );
    }






    // Header check
    public void assertHeaderVisible() {
        assertThat(header).isVisible();
        assertThat(header).hasText("Manage your users");
    }

    public Page getPage() {
        return page;
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
        page.waitForURL("**/logout-success");
        page.getByText("You're now signed out of your account")
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000));
    }

    public void clickManageUser() {
        userFullNameLink.click();
    }

    public void confirmAndDeleteUser() {
        deleteUserLink.click();
        deleteUserReasonRadioFirst.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        deleteUserReasonRadioFirst.click();
        confirmAndDeleteUserButton.click();

        assertEquals(
                "User deleted",
                deleteUserMessageHeading.textContent().trim()
        );
    }

    public boolean isDeleteUserVisible() {
        return deleteUserLink.isVisible();
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
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        }
    }

    public void clickExternalUserLink(String user) {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
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
        // Wait for the page to load by checking if the email row is visible
        Locator emailRow = page.locator(".govuk-summary-list__row:has-text(\"Email\") .govuk-summary-list__value");
        emailRow.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));

        assertTrue(emailRow.isVisible());
        assertTrue(page.locator(".govuk-summary-list__row:has-text(\"First name\") .govuk-summary-list__value").isVisible());
        assertTrue(page.locator(".govuk-summary-list__row:has-text(\"Last name\") .govuk-summary-list__value").isVisible());
    }

    public void verifyUserDetailsPopulated(String email, String firstName, String lastName, String firmName, String multiFirmAccess) {
        page.locator(".govuk-summary-list__row:has-text(\"Email\") .govuk-summary-list__value")
                .waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000));
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
            String officeAccountNumber = office;

            if (office.contains("Office account number:")) {
                officeAccountNumber = office
                        .substring(office.indexOf("Office account number:")
                                + "Office account number:".length())
                        .replace(")", "")
                        .trim();
            }

            Locator officeCheckbox = page.locator(".govuk-checkboxes__item")
                    .filter(new Locator.FilterOptions()
                            .setHasText(officeAccountNumber))
                    .locator("input[name='offices']")
                    .first();

            officeCheckbox.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(10000)
            );

            if (!officeCheckbox.isChecked()) {
                officeCheckbox.check();
            }

            assertThat(officeCheckbox).isChecked();
        }
    }

    public void uncheckSelectedOffices(List<String> offices) {
        for (String office : offices) {
            String officeAccountNumber = office;

            if (office.contains("Office account number:")) {
                officeAccountNumber = office
                        .substring(office.indexOf("Office account number:")
                                + "Office account number:".length())
                        .replace(")", "")
                        .trim();
            }

            Locator officeCheckbox = page.locator(".govuk-checkboxes__item")
                    .filter(new Locator.FilterOptions()
                            .setHasText(officeAccountNumber))
                    .locator("input[name='offices']")
                    .first();

            officeCheckbox.waitFor(
                    new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(10000)
            );

            if (officeCheckbox.isChecked()) {
                officeCheckbox.uncheck();
            }

            assertThat(officeCheckbox).not().isChecked();
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

    public void uncheckSelectedRoles(List<String> roles) {
        page.locator("input[type='checkbox']").first().waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(5000));
        for (String role : roles) {
            Locator checkbox = page.getByLabel(role);
            if (checkbox.isChecked()) {
                checkbox.uncheck();
            }
        }
    }

    public void verifyManageAccessButtonVisible() {
        assertThat(manageAccessButton).isVisible();
    }

    public void verifyServicesNotPresent(List<String> roles) {
        for (String role : roles) {
            Locator row = page.locator("dd:has-text('" + role + "')");
            assertThat(row).not().isVisible();
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

    public Locator firstIncompleteUserRowLocator() {
        Locator locator = page.locator(
                "tr.govuk-table__row:has(span.moj-badge.moj-badge--blue:has-text('INCOMPLETE'))"
        ).first();
        // Wait for the row to be visible
        locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
        return locator;
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

    // SignIn Error
    public void verifySignInError() {
        Locator errorText = page.getByText("Sorry, but we're having trouble signing you in.");
        errorText.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(10000));
        assertTrue(errorText.isVisible());
    }

    // Search
    public void searchForUser(String userEmail) {
        searchInputByName.fill(userEmail);
        searchButton.click();
    }


    public boolean searchAndVerifyUser(String email) {
        searchForUser(email);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        Locator row = page.locator("tbody tr").filter(
                new Locator.FilterOptions().setHasText(email)
        );

        try {
            row.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(5000));
            return true;
        } catch (Exception e) {
            return false;
        }
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

        // Anchor wait: error summary appears (proves validation ran)
        Locator errorSummary = page.locator(".govuk-error-summary");
        assertThat(errorSummary).isVisible();

        assertThat(firstNameInvalidCharsError).isVisible();
        assertThat(lastNameInvalidCharsError).isVisible();
        assertThat(selectUserTypeError).isVisible();
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

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        firmSearchInput.click();
        firmSearchInput.fill("");

        // Trigger autocomplete using real key events
        firmSearchInput.pressSequentially(firmCode);
        firmSearchInput.press("ArrowDown");

        // Wait until the combobox is actually open
        assertThat(firmSearchInput).hasAttribute("aria-expanded", "true");

        // Click option by firm code
        Locator firmOption = page.locator("#firmSearch__listbox li[role='option'] small")
                .filter(new Locator.FilterOptions().setHasText("Firm code: " + firmCode))
                .first()
                .locator("..");

        assertThat(firmOption).isVisible();
        firmOption.click();
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

        // Wait for error message to appear
        emailFormatError.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
        assertTrue(emailFormatError.isVisible());
    }

    public void triggerAndAssertEmailDomainError() {
        emailInput.fill("user@invalid-domain");
        clickContinueUserDetails();

        // Wait for error message to appear
        emailDomainError.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
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

    public String createProviderAdminUserWithNonMultiFirmAccess(String firmCode) {

        clickCreateUser();

        final String email = fillInUserDetails(false);

        selectMultiFirmAccess(false);
        searchAndSelectFirmByCode(firmCode);
        clickContinueFirmSelectPage();

        clickConfirmNewUserButton();
        clickGoBackToManageUsers();

        return email;
    }

    public void showFilters() {
        if ("false".equals(toggleFiltersButton.getAttribute("aria-expanded"))) {
            toggleFiltersButton.click();
        }

        assertThat(toggleFiltersButton).hasAttribute("aria-expanded", "true");
        assertThat(toggleFiltersButton).containsText("Hide filters");
        assertThat(filterPanel).isVisible();
    }

    private void checkFilter(Locator filter) {
        showFilters();

        if (!filter.isChecked()) {
            filter.check();
        }

        assertThat(filter).isChecked();
    }

    private void uncheckFilter(Locator filter) {
        showFilters();

        if (filter.isChecked()) {
            filter.uncheck();
        }

        assertThat(filter).not().isChecked();
    }

    public void applyFilters() {
        assertThat(applyFiltersButton).isVisible();
        applyFiltersButton.click();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void filterByThirdPartyUsers() {
        selectThirdPartyUserFilter();
        applyFilters();

        assertThat(page.locator("#showMultiFirmUsers")).isChecked();
    }

    public Locator userRowLocator(String email) {
        return page.locator("tr", new Page.LocatorOptions().setHasText(email));
    }

    public void clickUserLink(String email) {
        userRowLocator(email).locator("a").first().click();
    }


    public Locator statusTag(String status) {
        // Finds any GOV.UK status tag on the page that contains the supplied status text.
        // This keeps the selector in one place instead of repeating ".govuk-tag" in tests.
        return page.locator(".govuk-tag")
                .filter(new Locator.FilterOptions().setHasText(status));
    }

    public void assertStatusVisible(String status) {
        // Verifies that the expected status tag is displayed for the user.
        // The status text is passed in so this can be reused for any current or future status.
        assertTrue(
                statusTag(status).isVisible(),
                "Expected status tag to be visible: " + status
        );
    }

    public void assertStatusNotVisible(String status) {
        // Verifies that a specific status tag is not displayed.
        // Useful for negative checks where a user should not have a certain status.
        assertFalse(
                statusTag(status).isVisible(),
                "Expected status tag NOT to be visible: " + status
        );
    }

    public void refreshUntilStatusVisible(String email, String expectedStatus) {
        // Refreshes the manage users page and re-runs the user search until the expected status is visible.
        // This is useful where the status update happens asynchronously and the page needs reloading.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            page.reload();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            searchAndVerifyUser(email);

            if (statusTag(expectedStatus).isVisible()) {
                return;
            }
        }

        assertStatusVisible(expectedStatus);
    }

    public void clickConvertToMultiFirm() {
        assertThat(convertToMultiFirmLink).isVisible();
        assertThat(convertToMultiFirmLink).hasText("Convert to multi-firm");

        convertToMultiFirmLink.click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void assertConvertToMultiFirmPageVisible() {
        Locator heading = page.locator("h1.govuk-fieldset__heading")
                .filter(
                        new Locator.FilterOptions()
                                .setHasText("Allow multi-firm access")
                );

        assertThat(heading).isVisible();
        assertThat(convertToMultiFirmYesRadio).isVisible();
        assertThat(convertToMultiFirmNoRadio).isVisible();
    }

    public void assertConvertToMultiFirmDefaultsToNo() {
        assertThat(convertToMultiFirmNoRadio).isChecked();
        assertThat(convertToMultiFirmYesRadio).not().isChecked();
    }

    public void selectConvertToMultiFirm(boolean convertToMultiFirm) {
        if (convertToMultiFirm) {
            convertToMultiFirmYesRadio.check();
        } else {
            convertToMultiFirmNoRadio.check();
        }
    }

    public void assertMultiFirmAccess(String expectedValue) {
        assertRow("Multi-firm access", expectedValue);
    }

    public void assertMultiFirmConversionSuccessful() {
        assertThat(multiFirmConversionSuccessAlert).isVisible();
        assertMultiFirmAccess("Yes");
        assertThat(multiFirmUserNotification).isVisible();
        assertThat(viewAllFirmsLink).isVisible();
    }

    public void assertUserNotConvertedToMultiFirm() {
        page.waitForURL(
                "**/admin/users/manage/**",
                new Page.WaitForURLOptions().setTimeout(5000)
        );

        Locator multiFirmRow = convertToMultiFirmLink.locator(
                "xpath=ancestor::div[contains(@class, 'govuk-summary-list__row')]"
        );

        assertThat(multiFirmRow).isVisible();
        assertThat(
                multiFirmRow.locator(".govuk-summary-list__value")
        ).hasText("No");

        assertThat(convertToMultiFirmLink).isVisible();
    }

    public void verifyDelegateAccessProfilePageVisible() {
        page.waitForURL(
                "**/admin/multi-firm/user/add/profile",
                new Page.WaitForURLOptions().setTimeout(5000)
        );

        assertThat(delegateAccessHeading).isVisible();
        assertThat(delegateAccessEmailInput).isVisible();
        assertThat(delegateAccessContinueButton).isVisible();
    }

    public void enterDelegateAccessEmail(String email) {
        assertThat(delegateAccessEmailInput).isVisible();
        delegateAccessEmailInput.fill(email);
    }

    public void clickDelegateAccessContinue() {
        assertThat(delegateAccessContinueButton).isVisible();
        delegateAccessContinueButton.click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void verifyDelegateAccessButtonVisible() {
        assertThat(delegateAccessButton).isVisible();
    }

    public void verifyDelegateAccessFirmSelectionPageVisible() {
        page.waitForURL(
                "**/admin/multi-firm/user/add/profile/select/internalUserFirm",
                new Page.WaitForURLOptions().setTimeout(5000)
        );

        assertThat(delegateAccessFirmSelectionHeading).isVisible();
        assertThat(firmSearchInput).isVisible();
        assertThat(continueButtonFirmSelection).isVisible();
    }

    public void clickDelegateAccess() {
        assertThat(delegateAccessButton).isVisible();
        delegateAccessButton.click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public String createMultiFirmProviderUserWithoutFirm() {
        clickCreateUser();

        final String email = fillInUserDetails(false);

        selectMultiFirmAccess(true);
        clickConfirmNewUserButton();
        clickGoBackToManageUsers();

        return email;
    }

    public void delegateFirmAccess(
            String email,
            String firmCode,
            List<String> services,
            List<String> roles,
            List<String> officeAccountNumbers
    ) {
        verifyDelegateAccessButtonVisible();
        clickDelegateAccess();

        verifyDelegateAccessProfilePageVisible();
        enterDelegateAccessEmail(email);
        clickDelegateAccessContinue();

        verifyDelegateAccessFirmSelectionPageVisible();
        searchAndSelectFirmByCode(firmCode);
        clickContinueFirmSelectPage();

        checkSelectedServices(services);
        clickContinueUserDetails();

        checkSelectedRoles(roles);
        clickContinueUserDetails();

        checkSelectedOffices(officeAccountNumbers);
        clickContinueUserDetails();

        clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public String createMultiFirmUserAndDelegateAccess(
            String firmCode,
            List<String> services,
            List<String> roles,
            List<String> officeAccountNumbers
    ) {
        String email = createMultiFirmProviderUserWithoutFirm();

        searchAndVerifyUserNotExists(email);

        delegateFirmAccess(
                email,
                firmCode,
                services,
                roles,
                officeAccountNumbers
        );

        return email;
    }

    public void verifyRevokeAccessLinkVisible() {
        assertThat(revokeAccessLink).isVisible();
    }

    public void clickRevokeAccess() {
        assertThat(revokeAccessLink).isVisible();

        revokeAccessLink.click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void verifyRevokeAccessConfirmationPageVisible() {
        assertThat(revokeAccessHeading).isVisible();
        assertThat(revokeAccessYesRadio).isVisible();
        assertThat(revokeAccessNoRadio).isVisible();
        assertThat(revokeAccessConfirmButton).isVisible();
    }

    public void selectRevokeAccessYes() {
        assertThat(revokeAccessYesRadio).isVisible();
        revokeAccessYesRadio.check();
        assertThat(revokeAccessYesRadio).isChecked();
    }

    public void selectRevokeAccessNo() {
        assertThat(revokeAccessNoRadio).isVisible();
        revokeAccessNoRadio.check();
        assertThat(revokeAccessNoRadio).isChecked();
    }

    public void confirmRevokeAccess() {
        assertThat(revokeAccessConfirmButton).isVisible();
        revokeAccessConfirmButton.click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void verifyAccessRevokedSuccessfully() {
        assertThat(revokeAccessSuccessBanner).isVisible();

        assertThat(revokeAccessSuccessMessage)
                .containsText("no longer has access to");
    }

    public void selectThirdPartyUserFilter() {
        Locator multiFirmCheckbox = page.locator("#showMultiFirmUsers");

        if (!multiFirmCheckbox.isVisible()) {
            Locator showFiltersButton = page.getByRole(
                    AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Show filters")
            );

            assertThat(showFiltersButton).isVisible();
            showFiltersButton.click();

            assertThat(multiFirmCheckbox).isVisible();
        }

        if (!multiFirmCheckbox.isChecked()) {
            multiFirmCheckbox.check();
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        }

        assertThat(page.locator("#showMultiFirmUsers")).isChecked();
    }

    public void verifyOnlyThirdPartyUsersDisplayed() {
        Locator userRows = page.locator("tbody.govuk-table__body tr.govuk-table__row");

        assertTrue(
                userRows.count() > 0,
                "At least one third-party user should be displayed"
        );

        for (int index = 0; index < userRows.count(); index++) {
            assertThat(userRows.nth(index))
                    .containsText("External - 3rd Party");
        }
    }

    public void navigateToManageUsersWithThirdPartyFilter() {
        page.navigate("/admin/users?showMultiFirmUsers=true");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertThat(thirdPartyFilterCheckbox).isChecked();
    }

    public void verifySelectedUserOffices(List<String> offices) {
        for (String office : offices) {
            String officeAccountNumber = office;

            if (office.contains("Office account number:")) {
                officeAccountNumber = office
                        .substring(
                                office.indexOf("Office account number:")
                                        + "Office account number:".length()
                        )
                        .replace(")", "")
                        .trim();
            }

            Locator officeDetails = page.locator("#offices")
                    .filter(
                            new Locator.FilterOptions()
                                    .setHasText(officeAccountNumber)
                    );

            assertThat(officeDetails).containsText(officeAccountNumber);
        }
    }

    public void verifyOfficesNotPresent(List<String> offices) {
        for (String office : offices) {
            String officeAccountNumber = office;

            if (office.contains("Office account number:")) {
                officeAccountNumber = office
                        .substring(
                                office.indexOf("Office account number:")
                                        + "Office account number:".length()
                        )
                        .replace(")", "")
                        .trim();
            }

            Locator officeDetails = page.locator("#offices")
                    .getByText(
                            officeAccountNumber,
                            new Locator.GetByTextOptions().setExact(false)
                    );

            assertThat(officeDetails).not().isVisible();
        }
    }

    public String createMultiFirmFirmUserManager(
            String firmCode,
            List<String> officeAccountNumbers
    ) {
        String email = createMultiFirmProviderUserWithoutFirm();

        searchAndVerifyUserNotExists(email);

        verifyDelegateAccessButtonVisible();
        clickDelegateAccess();

        verifyDelegateAccessProfilePageVisible();
        enterDelegateAccessEmail(email);
        clickDelegateAccessContinue();

        verifyDelegateAccessFirmSelectionPageVisible();
        searchAndSelectFirmByCode(firmCode);
        clickContinueFirmSelectPage();

        // Selecting Manage your users grants the manager access.
        checkSelectedServices(List.of(
                "Manage your users",
                "Test LAA App Four"
        ));
        clickContinueUserDetails();

        // Only Test LAA App Four presents a role-selection screen.
        checkSelectedRoles(List.of(
                "Test LAA App Four Role One Access"
        ));
        clickContinueUserDetails();

        // Offices screen
        checkSelectedOffices(officeAccountNumbers);
        clickContinueUserDetails();

        // Check your answers
        clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        return email;
    }

    public void verifyAwaitingFirmAccessMessage() {
        assertThat(awaitingFirmAccessMessage).isVisible();

        assertThat(awaitingFirmAccessMessage)
                .containsText(
                        "Your firm admin is working on giving you access to the services you need.");

        assertThat(awaitingFirmAccessMessage)
                .containsText(
                        "There’s nothing else you need to do.");

        assertThat(awaitingFirmAccessMessage)
                .containsText(
                        "If you still don’t have access after 2 working days");
    }

}


