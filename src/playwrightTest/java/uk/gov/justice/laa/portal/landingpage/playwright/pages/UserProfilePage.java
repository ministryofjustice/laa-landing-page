package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class UserProfilePage {

    private final Page page;
    private final int port;
    private final Locator officesTab;
    private final Locator changeOfficeAssignmentLink;
    private final Locator allOfficesCheckBox;
    private final Locator noOfficesCheckBox;
    private final Locator continueButton;
    private final Locator confirmButton;

    public UserProfilePage(Page page, int port) {
        this.page = page;
        this.port = port;
        this.officesTab = page.locator("#tab_offices");
        this.changeOfficeAssignmentLink = page.locator("#offices ul.govuk-summary-card__actions a.govuk-link");
        this.allOfficesCheckBox = page.locator("#offices-all");
        this.noOfficesCheckBox = page.locator("#no-offices");
        this.continueButton = page.locator("button[type='submit']:has-text('Continue')");
        this.confirmButton = page.locator("button[type='submit']:has-text('Confirm')");
    }

    public Locator getOfficesTab() {
        return officesTab;
    }

    public Locator getAllOfficesCheckBox() {
        return allOfficesCheckBox;
    }

    public Locator getNoOfficesCheckBox() {
        return noOfficesCheckBox;
    }

    public Locator getChangeOfficeAssignmentLink() {
        return changeOfficeAssignmentLink;
    }

    public void clickContinueUserDetails() {
        continueButton.click();
    }

    public void clickConfirmButton() {
        confirmButton.click();
    }
}
