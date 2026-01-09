package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.UserProfilePage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OfficeAssignmentTest extends BaseFrontEndTest {

    @Test
    @Order(1)
    void doNotShowOfficeTabForInternalUser() {
        UserProfilePage userProfilePage =
                loginAndGetUserProfilePage(TestUser.GLOBAL_ADMIN, "playwright-internaluserviewer@playwrighttest.com");

        assertFalse(userProfilePage.getOfficesTab().isVisible());
    }

    @Test
    @Order(2)
    void showOfficeTabForExternalUser() {
        UserProfilePage userProfilePage =
                loginAndGetUserProfilePage(TestUser.GLOBAL_ADMIN, "playwright-firmusermanager@playwrighttest.com");

        assertTrue(userProfilePage.getOfficesTab().isVisible());
    }

    @Test
    @Order(3)
    void assignNoOfficeForExternalUser() {
        UserProfilePage userProfilePage =
                loginAndGetUserProfilePage(TestUser.GLOBAL_ADMIN, "playwright-firmusermanager@playwrighttest.com");
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(userProfilePage.getOfficesTab().isVisible());
        userProfilePage.getOfficesTab().click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertEquals("Change", userProfilePage.getChangeOfficeAssignmentLink().innerText());
        userProfilePage.getChangeOfficeAssignmentLink().click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(userProfilePage.getAllOfficesCheckBox().isChecked());

        Locator others = page.locator("input[type='checkbox']:not(#offices-all)");
        for (int i = 0; i < others.count(); i++) {
            assertFalse(others.nth(i).isChecked());
        }

        userProfilePage.getNoOfficesCheckBox().click();


        assertFalse(userProfilePage.getAllOfficesCheckBox().isChecked());
        userProfilePage.clickContinueUserDetails();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(page.locator(".govuk-summary-list__row:has-text(\"No Office Access\") .govuk-summary-list__value").isVisible());

        userProfilePage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(page.locator(".govuk-panel--confirmation:has-text(\"User detail updated\") .govuk-panel__title").isVisible());
    }

    @Test
    @Order(4)
    void assignAllOfficesForExternalUser() {
        UserProfilePage userProfilePage =
                loginAndGetUserProfilePage(TestUser.GLOBAL_ADMIN, "playwright-firmusermanager@playwrighttest.com");

        assertTrue(userProfilePage.getOfficesTab().isVisible());
        userProfilePage.getOfficesTab().click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(userProfilePage.getChangeOfficeAssignmentLink().isVisible());
        assertEquals("Change", userProfilePage.getChangeOfficeAssignmentLink().innerText());
        userProfilePage.getChangeOfficeAssignmentLink().click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(userProfilePage.getNoOfficesCheckBox().isChecked());

        Locator others = page.locator("input[type='checkbox']:not(#no-offices)");
        for (int i = 0; i < others.count(); i++) {
            assertFalse(others.nth(i).isChecked());
        }

        page.locator("main#main-content")
                .getByRole(AriaRole.CHECKBOX)
                .nth(1)
                .check();

        assertFalse(userProfilePage.getNoOfficesCheckBox().isChecked());

        userProfilePage.clickContinueUserDetails();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(page.locator(".govuk-summary-list__row:has-text(\"Automation Office 1, City1, 12345 (THREE)\") .govuk-summary-list__value").isVisible());

        userProfilePage.clickConfirmButton();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertTrue(page.locator(".govuk-panel--confirmation:has-text(\"User detail updated\") .govuk-panel__title").isVisible());
    }

    @Test
    @Order(5)
    void externalUserViewerCanNotChangeOffice(){
        UserProfilePage userProfilePage =
                loginAndGetUserProfilePage(TestUser.EXTERNAL_USER_VIEWER, "playwright-firmusermanager@playwrighttest.com");

        assertTrue(userProfilePage.getOfficesTab().isVisible());
        userProfilePage.getOfficesTab().click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        assertFalse(userProfilePage.getChangeOfficeAssignmentLink().isVisible());
    }

}
