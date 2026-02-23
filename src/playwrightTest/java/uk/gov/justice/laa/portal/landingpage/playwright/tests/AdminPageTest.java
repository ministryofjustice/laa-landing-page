package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.AdminPage;

import java.util.stream.Stream;

public class AdminPageTest extends BaseFrontEndTest {

    private static Stream<TestUser> nonPermittedUsers() {
        return Stream.of(
                TestUser.GLOBAL_ADMIN,
                TestUser.INTERNAL_USER_VIEWER,
                TestUser.INTERNAL_USER_MANAGER,
                TestUser.EXTERNAL_USER_VIEWER,
                TestUser.EXTERNAL_USER_MANAGER,
                TestUser.EXTERNAL_USER_ADMIN,
                TestUser.INFORMATION_AND_ASSURANCE,
                TestUser.NO_ROLES
        );
    }

    private AdminPage loginAndGetAdminPage(TestUser user) {
        loginAs(user.email);
        page.navigate(String.format("http://localhost:%d/admin/silas-administration", port));
        return new AdminPage(page, port);
    }

    @Test
    @DisplayName("SiLAS Administration page loads and shows expected heading")
    void adminPage_loadsAndShowsHeading() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.SILAS_ADMINISTRATION);
        adminPage.assertOnPage();
    }

    @Test
    @DisplayName("Admin Services tab shows expected columns and at least 1 row")
    void adminServicesTab_hasExpectedColumnsAndRows() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.SILAS_ADMINISTRATION);

        adminPage.assertAdminServicesTableColumns()
                .assertReorderAdminServicesButtonVisible();

        Assertions.assertTrue(
                adminPage.getAdminServicesRowCount() > 0,
                "Expected Admin Services table to contain at least 1 row"
        );
    }

    @Test
    @DisplayName("Legal Aid Services tab shows expected columns and at least 1 row")
    void legalAidServicesTab_hasExpectedColumnsAndRows() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.SILAS_ADMINISTRATION);

        adminPage.assertLegalAidServicesTableColumns()
                .assertReorderLegalAidServicesButtonVisible();

        Assertions.assertTrue(
                adminPage.getLegalAidServicesRowCount() > 0,
                "Expected Legal Aid Services table to contain at least 1 row"
        );
    }

    @Test
    @DisplayName("Edit Legal Aid Service End to End")
    void legalAidServicesTab_edit_app_end_to_end() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.SILAS_ADMINISTRATION);

        adminPage.assertLegalAidServicesTableColumns()
                .assertReorderLegalAidServicesButtonVisible();

        Assertions.assertTrue(
                adminPage.getLegalAidServicesRowCount() > 0,
                "Expected Legal Aid Services table to contain at least 1 row"
        );

        clickEditAppLinkForFirstLegalAidService();

        page.locator("#app-description").fill("Test LAA App 2");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue")).click();

        verifyNewAppDescriptionInTable("Test LAA App Two", "Test LAA App 2");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirm")).click();
        assertConfirmationMessage();

    }

    @Test
    @DisplayName("Reorder Legal Aid Service End to End")
    void legalAidServicesTab_reorder_apps_end_to_end() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.SILAS_ADMINISTRATION);

        adminPage.assertLegalAidServicesTableColumns()
                .assertReorderLegalAidServicesButtonVisible();

        Assertions.assertTrue(
                adminPage.getLegalAidServicesRowCount() > 0,
                "Expected Legal Aid Services table to contain at least 1 row"
        );

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reorder legal aid services")).click();

        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        page.locator("#apps0\\.ordinal").fill("4");
        page.locator("#apps1\\.ordinal").fill("3");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue")).click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        verifyAppOrderConfirmationTable("3", "4");

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirm")).click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        PlaywrightAssertions.assertThat(
                page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Apps order updated"))
        ).isVisible();

    }

    private void assertConfirmationMessage() {
        String expectedUrlFragment = "/admin/silas-administration/app/99999999-9999-9999-9999-999999999999/check-answers";
        Assertions.assertTrue(
                page.url().contains(expectedUrlFragment),
                "Expected URL to contain '" + expectedUrlFragment + "' after clicking Edit link for Legal Aid Service"
        );
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        PlaywrightAssertions.assertThat(
                page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("App details updated"))
        ).isVisible();
    }

    private void verifyNewAppDescriptionInTable(String expectedCurrDesc, String expectedNewDesc) {
        // Assert that the URL contains the expected path and query parameter for the selected app
        String expectedUrlFragment = "/admin/silas-administration/app/99999999-9999-9999-9999-999999999999/check-answers";
        Assertions.assertTrue(
                page.url().contains(expectedUrlFragment),
                "Expected URL to contain '" + expectedUrlFragment + "' after clicking Edit link for Legal Aid Service"
        );
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Locator for the <dd> value next to "Current App Description"
        Locator currentDesc = page.locator(
                "div.govuk-summary-list__row:has(dt.govuk-summary-list__key:has-text('Current App Description')) "
                        + "dd.govuk-summary-list__value");

        // Locator for the <dd> value next to "New App Description"
        Locator newDesc = page.locator(
                "div.govuk-summary-list__row:has(dt.govuk-summary-list__key:has-text('New App Description')) "
                        + "dd.govuk-summary-list__value");

        // Get the text values (trim whitespace)
        String currentText = currentDesc.textContent().trim();
        String newText = newDesc.textContent().trim();

        // Assert equality (will fail with your current sample values)
        Assertions.assertEquals(expectedCurrDesc, currentText,
                String.format("Descriptions do not match. Current='%s', New='%s'", expectedCurrDesc, currentText));
        Assertions.assertEquals(expectedNewDesc, newText,
                String.format("Descriptions do not match. Current='%s', New='%s'", expectedNewDesc, newText));
    }

    private void verifyAppOrderConfirmationTable(String expectedOrdinal1, String expectedOrdinal2) {
        // Assert that the URL contains the expected path and query parameter for the selected app
        String expectedUrlFragment = "/admin/silas-administration/apps/reorder";
        Assertions.assertTrue(
                page.url().contains(expectedUrlFragment),
                "Expected URL to contain '" + expectedUrlFragment + "' after clicking Edit link for Legal Aid Service"
        );
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        // Locator for the <dd> value next to "Current App Description"
        Locator currentDesc = page.locator(
                "div.govuk-summary-list__row:has(dt.govuk-summary-list__key:has-text('Test LAA App Two')) "
                        + "dd.govuk-summary-list__value");

        // Locator for the <dd> value next to "New App Description"
        Locator newDesc = page.locator(
                "div.govuk-summary-list__row:has(dt.govuk-summary-list__key:has-text('Test LAA App One')) "
                        + "dd.govuk-summary-list__value");

        // Get the text values (trim whitespace)
        String currentText = currentDesc.textContent().trim();
        String newText = newDesc.textContent().trim();

        // Assert equality (will fail with your current sample values)
        Assertions.assertEquals(expectedOrdinal1, currentText,
                String.format("Descriptions do not match. Current='%s', New='%s'", expectedOrdinal1, currentText));
        Assertions.assertEquals(expectedOrdinal2, newText,
                String.format("Descriptions do not match. Current='%s', New='%s'", expectedOrdinal2, newText));
    }

    @Test
    @DisplayName("Roles and Permissions tab shows expected columns and action buttons")
    void rolesAndPermissionsTab_hasExpectedColumnsAndButtons() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.SILAS_ADMINISTRATION);

        adminPage.assertRolesTableColumns()
                .assertRolesActionButtonsVisible()
                .assertReorderRolesButtonVisible();

        Assertions.assertTrue(
                adminPage.getRolesRowCount() > 0,
                "Expected Roles table to contain at least 1 row"
        );
    }

    @Test
    @DisplayName("Roles and Permissions filter dropdown is present and can filter by a known application")
    void rolesFilter_canFilterByApplication() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.SILAS_ADMINISTRATION);

        int before = adminPage.getRolesRowCount();

        adminPage.filterRolesByApplication("Manage Your Users");

        int after = adminPage.getRolesRowCount();

        Assertions.assertTrue(before > 0, "Expected roles table to contain at least 1 row before filtering");
        Assertions.assertTrue(after > 0, "Expected roles table to contain at least 1 row after filtering");
    }

    @ParameterizedTest(name = "User {0} cannot access SiLAS Administration page")
    @MethodSource("nonPermittedUsers")
    @DisplayName("Non-permitted users cannot see SiLAS Administration page content")
    void nonPermittedUsers_cannotSeeAdminPageContent(TestUser user) {

        AdminPage adminPage = loginAndGetAdminPage(user);

        adminPage.assertAccessForbiddenPage();
        Assertions.assertEquals(
                0,
                page.locator("h1.govuk-heading-xl:has-text('SiLAS Administration')").count(),
                "SiLAS Administration heading should not be visible to user " + user.name()
        );
    }

    private void clickEditAppLinkForFirstLegalAidService() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Change")).first().click();

        // Assert that the URL contains the expected path and query parameter for the selected app
        String expectedUrlFragment = "/admin/silas-administration/app/99999999-9999-9999-9999-999999999999";
        Assertions.assertTrue(
                page.url().contains(expectedUrlFragment),
                "Expected URL to contain '" + expectedUrlFragment + "' after clicking Edit link for Legal Aid Service"
        );
    }
}
