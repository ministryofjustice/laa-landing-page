package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import java.util.List;
import java.util.stream.Stream;

import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.AuditPage;

public class AuditPageTest extends BaseFrontEndTest {

    /**
     * Logs in as the specified user and navigates to the Audit page.
     */
    private AuditPage loginAndGetAuditPage(TestUser user) {
        loginAs(user.email);
        page.navigate(String.format("http://localhost:%d/admin/users/audit", port));
        return new AuditPage(page, port);
    }

    @Test
    @DisplayName("Filter audit table by Internal users only")
    void filterByUserType_internal() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.filterByUserType("INTERNAL");
        auditPage.assertUserIsPresent("playwright-internalusermanager@playwrighttest.com");
    }

    @Test
    @DisplayName("Audit table displays all expected columns in correct order")
    void auditTable_hasExpectedColumnsInOrder() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        List<String> headers = auditPage.getTableHeaderTexts();

        Assertions.assertEquals(
                List.of(
                        "Name",
                        "Email",
                        "Type",
                        "Firm",
                        "SILAS Account Status",
                        "Multi-firm",
                        "Profiles"
                ),
                headers,
                "Audit table headers do not match expected contract"
        );
    }

    @Test
    @DisplayName("Audit table rows always contain exactly 7 columns")
    void auditTable_eachRowHasSevenColumns() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.assertEachRowHasColumnCount(7);
    }

    @ParameterizedTest(name = "Filter by role: {0}")
    @MethodSource("roleFilterUsers")
    @DisplayName("Audit table filters correctly by SILAS role")
    void filterByRole_usingTestUserEnum(TestUser user) {

        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.filterBySilasRole(user.silasRoleLabel);

        auditPage.assertUserIsPresent(user.email);
    }

    private static Stream<TestUser> roleFilterUsers() {
        return Stream.of(
                TestUser.GLOBAL_ADMIN,
                TestUser.INTERNAL_USER_VIEWER,
                TestUser.INTERNAL_USER_MANAGER,
                TestUser.EXTERNAL_USER_VIEWER,
                TestUser.EXTERNAL_USER_MANAGER,
                TestUser.EXTERNAL_USER_ADMIN,
                TestUser.INFORMATION_AND_ASSURANCE
        );
    }

    @Test
    @DisplayName("Global Admin can access the Audit Users page")
    void globalAdmin_canAccessAuditPage() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.assertOnAuditPage();
    }

    @Test
    @DisplayName("Security Response can access the Audit Users page")
    void informationAndAssurance_canAccessAuditPage() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.INFORMATION_AND_ASSURANCE);

        auditPage.assertOnAuditPage();
    }


    @ParameterizedTest(name = "User {0} is forbidden from accessing Audit page")
    @MethodSource("nonPermittedUsers")
    @DisplayName("Non-permitted users see Access Forbidden page")
    void nonPermittedUsers_seeAccessForbiddenPage(TestUser user) {

        AuditPage auditPage = loginAndGetAuditPage(user);

        auditPage.assertAccessForbidden();

        Assertions.assertEquals(
                0,
                page.locator("h1.govuk-heading-l:has-text('User Access Audit Table')").count(),
                "Audit page should not be visible to user " + user.name()
        );
    }


    private static Stream<TestUser> nonPermittedUsers() {
        return Stream.of(
                TestUser.NO_ROLES
        );
    }

    // -----------------------------------------------------------------------
    // CSV Export tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Export CSV button is visible and enabled above the audit table")
    void exportCsvButton_isVisibleAndEnabled() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.assertExportCsvButtonVisible();
        auditPage.assertExportCsvButtonNotDisabled();
    }

    @Test
    @DisplayName("Export CSV button is above the audit table")
    void exportCsvButton_isAboveAuditTable() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        double buttonY = auditPage.getExportCsvButtonY();
        double tableY = auditPage.getAuditTableY();
        Assertions.assertTrue(
                buttonY < tableY,
                "Export CSV button should appear above the audit table"
        );
    }

    @Test
    @DisplayName("CSV error banner is not shown on initial page load")
    void csvErrorBanner_isHiddenOnLoad() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.assertCsvErrorBannerHidden();
    }

    @Test
    @DisplayName("Clicking Export CSV without a firm filter shows the error banner")
    void exportCsv_withoutFirm_showsErrorBanner() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.clickExportCsv();
        auditPage.assertCsvErrorBannerVisible();
    }

    @Test
    @DisplayName("Error banner shows exact required message when no firm is selected")
    void exportCsv_withoutFirm_errorBannerHasCorrectText() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.clickExportCsv();
        auditPage.assertCsvErrorBannerContainsText("Filter results to a single firm to enable CSV export.");
    }

    @Test
    @DisplayName("Selecting a firm in the autocomplete without submitting still shows the CSV error banner")
    void exportCsv_withFirmSelectedButNotApplied_showsErrorBanner() {
        // data-firm-selected is rendered server-side; filling the autocomplete client-side
        // does not reload the page, so the button still considers no firm selected.
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.searchAndSelectFirm("90001");
        auditPage.clickExportCsv();
        auditPage.assertCsvErrorBannerVisible();
    }

    @Test
    @DisplayName("'View all deleted users' link is visible above the audit table")
    void viewDeletedUsersLink_isVisible() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.assertViewDeletedUsersLinkVisible();
    }
}
