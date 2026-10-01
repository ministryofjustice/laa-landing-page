package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.AuditPage;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.ManageUsersPage;

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
        auditPage.populateFirmField("90001");
        auditPage.clickExportCsv();
        auditPage.assertCsvErrorBannerVisible();
    }

    @Test
    @DisplayName("'View all deleted users' link is visible above the audit table")
    void viewDeletedUsersLink_isVisible() {
        AuditPage auditPage = loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);
        auditPage.assertViewDeletedUsersLinkVisible();
    }

    @Test
    @DisplayName("Audit name search returns matching users across different firms")
    void auditNameSearchReturnsMatchingUsersAcrossDifferentFirms() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Alex FilterShared"
        );

        auditPage.clickSearch();

        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-internal@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Audit firm and name search only returns matching user from selected firm")
    void auditFirmAndNameSearchOnlyReturnsMatchingUserFromSelectedFirm() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirmWithoutSubmitting("90001");
        auditPage.enterNameOrEmailWithoutSubmitting("Alex FilterShared");
        auditPage.clickSearch();

        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-internal@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Audit SILAS role filter preserves firm and name search")
    void auditSilasRoleFilterPreservesFirmAndNameSearch() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirmWithoutSubmitting("90001");
        auditPage.enterNameOrEmailWithoutSubmitting("Jamie FilterShared");
        auditPage.clickSearch();

        // Baseline: Firm One Jamie is returned
        auditPage.verifyUserVisible(
                "playwright-filter-f1-admin@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-admin@playwrighttest.com"
        );

        // Apply existing SILAS Role filter
        auditPage.filterBySilasRole("Firm User Manager");

        // Existing search state must survive the role filter
        auditPage.verifyNameOrEmailValue("Jamie FilterShared");
        auditPage.verifySilasRoleSelected("Firm User Manager");

        auditPage.verifyUserVisible(
                "playwright-filter-f1-admin@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-admin@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Audit user type filter preserves firm, name and SILAS role filters")
    void auditUserTypeFilterPreservesExistingFilters() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirmWithoutSubmitting("90001");
        auditPage.enterNameOrEmailWithoutSubmitting("Morgan FilterShared");
        auditPage.clickSearch();

        auditPage.filterBySilasRole("Firm User Manager");

        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-both@playwrighttest.com"
        );

        // Apply user type as an additional filter
        auditPage.selectUserType("MULTI_FIRM");

        // Existing filters must remain selected
        auditPage.verifyNameOrEmailValue("Morgan FilterShared");
        auditPage.verifySilasRoleSelected("Firm User Manager");
        auditPage.verifyUserTypeSelected("MULTI_FIRM");

        // Firm restriction must also still be effective
        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-both@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Changing SILAS role preserves firm and name filters and updates results")
    void changingSilasRolePreservesFirmAndNameFilters() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirmWithoutSubmitting("90001");
        auditPage.enterNameOrEmailWithoutSubmitting("Alex FilterShared");
        auditPage.clickSearch();

        // Alex in Firm One has External User Viewer
        auditPage.filterBySilasRole("External User Viewer");

        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );

        // Change ONLY the SILAS role
        auditPage.filterBySilasRole("Firm User Manager");

        // Firm and name search must still be preserved
        auditPage.verifyNameOrEmailValue("Alex FilterShared");
        auditPage.verifySilasRoleSelected("Firm User Manager");

        // Alex must now be excluded because he does not have this role
        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Sorting audit table preserves active firm, name, role and user type filters")
    void sortingAuditTablePreservesActiveFilters() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirmWithoutSubmitting("90001");
        auditPage.enterNameOrEmailWithoutSubmitting("Morgan FilterShared");
        auditPage.clickSearch();

        auditPage.filterBySilasRole("Firm User Manager");
        auditPage.selectUserType("MULTI_FIRM");

        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        // Sort the filtered table
        auditPage.sortByColumn("Email");

        // All filter state must survive the sort
        auditPage.verifySelectedFirmId(
                "11111111-1111-1111-1111-111111111111"
        );

        auditPage.verifyNameOrEmailValue(
                "Morgan FilterShared"
        );

        auditPage.verifySilasRoleSelected(
                "Firm User Manager"
        );

        auditPage.verifyUserTypeSelected(
                "MULTI_FIRM"
        );

        // Results must still respect the filters
        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-both@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Changing audit firm preserves name, SILAS role and user type filters")
    void changingAuditFirmPreservesExistingFilters() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        // Firm One + Morgan
        auditPage.selectFirmWithoutSubmitting("90001");
        auditPage.enterNameOrEmailWithoutSubmitting("Morgan FilterShared");
        auditPage.clickSearch();

        auditPage.filterBySilasRole("Firm User Manager");
        auditPage.selectUserType("MULTI_FIRM");

        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-both@playwrighttest.com"
        );

        // Change ONLY the firm
        auditPage.selectFirmWithoutSubmitting("90002");

        // Existing filters must survive
        auditPage.verifySelectedFirmId(
                "22222222-2222-2222-2222-222222222222"
        );

        auditPage.verifyNameOrEmailValue(
                "Morgan FilterShared"
        );

        auditPage.verifySilasRoleSelected(
                "Firm User Manager"
        );

        auditPage.verifyUserTypeSelected(
                "MULTI_FIRM"
        );

        // Results should now flip to Firm Two
        auditPage.verifyUserVisible(
                "playwright-filter-f2-both@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Clearing audit firm filter removes firm restriction and preserves name search")
    void clearingAuditFirmFilterRemovesFirmRestrictionAndPreservesNameSearch() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirmWithoutSubmitting("90001");
        auditPage.enterNameOrEmailWithoutSubmitting("Alex FilterShared");
        auditPage.clickSearch();

        // Initially restricted to Firm One
        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-internal@playwrighttest.com"
        );

        // Clear only the firm
        auditPage.clearFirmFilterWithoutSubmitting();

        // Hidden firm UUID must clear as well
        auditPage.verifySelectedFirmIdIsEmpty();

        auditPage.clickSearch();

        // Name search should remain
        auditPage.verifyNameOrEmailValue("Alex FilterShared");

        // No firm restriction now
        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-internal@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Changing App Access filter preserves search and updates audit results")
    void changingAppAccessFilterPreservesSearchAndUpdatesResults() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        String email =
                "playwright-filter-f1-standard@playwrighttest.com";

        auditPage.enterNameOrEmailWithoutSubmitting(email);
        auditPage.clickSearch();

        auditPage.verifyUserVisible(email);

        // Alex has External User Viewer under Manage your users
        auditPage.selectAppAccess("Manage your users");

        auditPage.verifyNameOrEmailValue(email);
        auditPage.verifyAppAccessSelected("Manage your users");

        auditPage.verifyUserVisible(email);

        // Change only the application
        auditPage.selectAppAccess("Test LAA App One");

        auditPage.verifyNameOrEmailValue(email);
        auditPage.verifyAppAccessSelected("Test LAA App One");

        // Alex has no Test LAA App One access
        auditPage.verifyUserNotVisible(email);
    }

    @Test
    @DisplayName("Audit supports firm, name, SILAS role, app access and user type filters together")
    void auditSupportsAllPrimaryFiltersTogether() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        // Start with Firm One + Morgan
        auditPage.selectFirmWithoutSubmitting("90001");

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Morgan FilterShared"
        );

        auditPage.clickSearch();

        // Add role
        auditPage.filterBySilasRole(
                "Firm User Manager"
        );

        // Add app access
        auditPage.selectAppAccess(
                "Manage your users"
        );

        // Add user type
        auditPage.selectUserType(
                "MULTI_FIRM"
        );

        // Every active filter must still be retained
        auditPage.verifySelectedFirmId(
                "11111111-1111-1111-1111-111111111111"
        );

        auditPage.verifyNameOrEmailValue(
                "Morgan FilterShared"
        );

        auditPage.verifySilasRoleSelected(
                "Firm User Manager"
        );

        auditPage.verifyAppAccessSelected(
                "Manage your users"
        );

        auditPage.verifyUserTypeSelected(
                "MULTI_FIRM"
        );

        // Correct Firm One user remains
        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        // Same hostile fixture in Firm Two must not leak through
        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-both@playwrighttest.com"
        );

        // Similar Firm One users that do not satisfy the whole combination
        // must not appear either.
        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-admin@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-thirdparty@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Clearing App Access preserves all other active audit filters")
    void clearingAppAccessPreservesAllOtherActiveAuditFilters() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        // Build up the full filter state
        auditPage.selectFirmWithoutSubmitting("90001");

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Morgan FilterShared"
        );

        auditPage.clickSearch();

        auditPage.filterBySilasRole(
                "Firm User Manager"
        );

        auditPage.selectUserType(
                "MULTI_FIRM"
        );

        auditPage.selectAppAccess(
                "Manage your users"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        // Change app to one Morgan does not have
        auditPage.selectAppAccess(
                "Test LAA App One"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        // Clear ONLY App Access
        auditPage.selectAppAccess(
                "All apps"
        );

        // Everything else must still survive
        auditPage.verifySelectedFirmId(
                "11111111-1111-1111-1111-111111111111"
        );

        auditPage.verifyNameOrEmailValue(
                "Morgan FilterShared"
        );

        auditPage.verifySilasRoleSelected(
                "Firm User Manager"
        );

        auditPage.verifyUserTypeSelected(
                "MULTI_FIRM"
        );

        auditPage.verifyAppAccessSelected(
                "All apps"
        );

        // Morgan should return once App Access restriction is removed
        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-both@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Audit user type filter distinguishes internal and external users with same name")
    void auditUserTypeFilterDistinguishesUsersWithSameName() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Alex FilterShared"
        );

        auditPage.clickSearch();

        // Baseline - all three Alex fixtures
        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-internal@playwrighttest.com"
        );

        // Narrow to internal
        auditPage.selectUserType("INTERNAL");

        auditPage.verifyNameOrEmailValue("Alex FilterShared");
        auditPage.verifyUserTypeSelected("INTERNAL");

        auditPage.verifyUserVisible(
                "playwright-filter-internal@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Clearing Audit user type filter widens results and preserves name search")
    void clearingAuditUserTypeWidensResults() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Alex FilterShared"
        );

        auditPage.clickSearch();

        auditPage.selectUserType("INTERNAL");

        auditPage.verifyUserVisible(
                "playwright-filter-internal@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        // All user types
        auditPage.selectUserType("ALL");

        auditPage.verifyNameOrEmailValue("Alex FilterShared");
        auditPage.verifyUserTypeSelected("ALL");

        auditPage.verifyUserVisible(
                "playwright-filter-internal@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Clearing SILAS role preserves name search and widens audit results")
    void clearingSilasRolePreservesNameAndWidensResults() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Alex FilterShared"
        );

        auditPage.clickSearch();

        // Only internal Alex has this role
        auditPage.filterBySilasRole(
                "Internal User Viewer"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-internal@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );

        // Remove role restriction
        auditPage.clearSilasRole();

        auditPage.verifyNameOrEmailValue("Alex FilterShared");
        auditPage.verifySilasRoleCleared();

        auditPage.verifyUserVisible(
                "playwright-filter-internal@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f2-standard@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Selecting firm retains selected firm ID after Audit auto-submit")
    void selectingFirmRetainsSelectedFirmId() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirm("90001");

        auditPage.verifySelectedFirmId(
                "11111111-1111-1111-1111-111111111111"
        );
    }

    @Test
    @DisplayName("Changing name search preserves firm, role and App Access filters")
    void changingNameSearchPreservesExistingAuditFilters() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirm("90001");

        auditPage.filterBySilasRole(
                "External User Viewer"
        );

        auditPage.selectAppAccess(
                "Manage your users"
        );

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Alex FilterShared"
        );

        auditPage.clickSearch();

        auditPage.verifyUserVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );

        // Change ONLY the name search
        auditPage.enterNameOrEmailWithoutSubmitting(
                "Taylor FilterShared"
        );

        auditPage.clickSearch();

        auditPage.verifySelectedFirmId(
                "11111111-1111-1111-1111-111111111111"
        );

        auditPage.verifySilasRoleSelected(
                "External User Viewer"
        );

        auditPage.verifyAppAccessSelected(
                "Manage your users"
        );

        auditPage.verifyNameOrEmailValue(
                "Taylor FilterShared"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f1-thirdparty@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-thirdparty@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f1-standard@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Multi-firm audit user can be found under each associated firm")
    void multiFirmAuditUserCanBeFoundUnderEachAssociatedFirm() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Casey FilterShared"
        );

        auditPage.clickSearch();

        // Firm One
        auditPage.selectFirm("90001");

        auditPage.verifySelectedFirmId(
                "11111111-1111-1111-1111-111111111111"
        );

        auditPage.verifyNameOrEmailValue(
                "Casey FilterShared"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-crossfirm@playwrighttest.com"
        );

        // Switch to Firm Two
        auditPage.selectFirm("90002");

        auditPage.verifySelectedFirmId(
                "22222222-2222-2222-2222-222222222222"
        );

        auditPage.verifyNameOrEmailValue(
                "Casey FilterShared"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-crossfirm@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Selected firm display value survives audit dropdown filter changes")
    void selectedFirmDisplayValueSurvivesFilterChanges() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectFirm("90001");

        auditPage.enterNameOrEmailWithoutSubmitting(
                "Morgan FilterShared"
        );

        auditPage.clickSearch();

        auditPage.filterBySilasRole(
                "Firm User Manager"
        );

        auditPage.selectAppAccess(
                "Manage your users"
        );

        auditPage.selectUserType(
                "MULTI_FIRM"
        );

        // What the user sees must still be correct
        auditPage.verifyFirmSearchValue(
                "Automation Firm One"
        );

        // And the backend ID must still be correct
        auditPage.verifySelectedFirmId(
                "11111111-1111-1111-1111-111111111111"
        );

        auditPage.verifyNameOrEmailValue(
                "Morgan FilterShared"
        );

        auditPage.verifySilasRoleSelected(
                "Firm User Manager"
        );

        auditPage.verifyAppAccessSelected(
                "Manage your users"
        );

        auditPage.verifyUserTypeSelected(
                "MULTI_FIRM"
        );

        auditPage.verifyUserVisible(
                "playwright-filter-f1-both@playwrighttest.com"
        );

        auditPage.verifyUserNotVisible(
                "playwright-filter-f2-both@playwrighttest.com"
        );
    }

    @Test
    @DisplayName("Changing SILAS role from later audit page resets pagination to page one")
    void changingSilasRoleResetsPagination() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.goToPage(2);

        auditPage.verifyUrlContains(
                "page=2"
        );

        auditPage.filterBySilasRole(
                "Firm User Manager"
        );

        auditPage.verifyUrlContains(
                "page=1"
        );

        auditPage.verifySilasRoleSelected(
                "Firm User Manager"
        );
    }

    @Test
    @DisplayName("Changing App Access from later audit page resets pagination to page one")
    void changingAppAccessResetsPagination() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.goToPage(2);

        auditPage.verifyUrlContains(
                "page=2"
        );

        auditPage.selectAppAccess(
                "Manage your users"
        );

        auditPage.verifyUrlContains(
                "page=1"
        );

        auditPage.verifyAppAccessSelected(
                "Manage your users"
        );
    }

    @Test
    @DisplayName("Changing user type from later audit page resets pagination to page one")
    void changingUserTypeResetsPagination() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.goToPage(2);

        auditPage.verifyUrlContains(
                "page=2"
        );

        auditPage.selectUserType(
                "INTERNAL"
        );

        auditPage.verifyUrlContains(
                "page=1"
        );

        auditPage.verifyUserTypeSelected(
                "INTERNAL"
        );
    }

    @Test
    @DisplayName("Never Activated filter resets audit pagination to page one")
    void neverActivatedFilterResetsPagination() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.goToPage(2);

        auditPage.verifyUrlContains(
                "page=2"
        );

        auditPage.selectNeverActivated();

        auditPage.verifyNeverActivatedSelected();

        auditPage.verifyUrlContains(
                "page=1"
        );

        auditPage.verifyUrlContains(
                "neverActivated=true"
        );
    }

    @Test
    @DisplayName("Never Activated filter returns only users awaiting verification")
    void neverActivatedFilterReturnsOnlyUsersAwaitingVerification() {

        AuditPage auditPage =
                loginAndGetAuditPage(TestUser.GLOBAL_ADMIN);

        auditPage.selectNeverActivated();

        auditPage.verifyNeverActivatedSelected();

        auditPage.verifyAllReturnedUsersHaveSilasStatus(
                "Awaiting Verification"
        );
    }

}
