package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;


public class AuditPage {

    private static final Logger log = LoggerFactory.getLogger(AuditPage.class);

    private final Page page;
    private final String url;

    // Locators
    private final Locator pageHeader;

    private final Locator firmSearchInput;
    private final Locator firmSearchHiddenId;
    private final Locator firmSearchListbox;
    private final Locator firmOptionRows;

    private final Locator nameOrEmailSearchInput;
    private final Locator searchButton;
    private final Locator toggleFiltersButton;
    private final Locator applyFiltersButton;

    private final Locator silasRoleFilter;
    private final Locator appAccessFilter;
    private final Locator userTypeFilter;
    private final Locator viewAllDeletedUsersButton;


    private final Locator nameSortButton;
    private final Locator emailSortButton;
    private final Locator typeSortButton;
    private final Locator firmSortButton;
    private final Locator statusSortButton;
    private final Locator multiFirmSortButton;
    private final Locator profilesSortButton;

    private final Locator tableRows;
    private final Locator paginationNumbers;
    private final Locator paginationCurrentPage;
    private final Locator paginationNextPage;

    private final Locator resultsSummary;
    private final Locator tableHeaders;

    // CSV Export
    private final Locator exportCsvButton;
    private final Locator csvErrorBanner;
    private final Locator viewDeletedUsersLink;
    private final Locator auditTable;

    //deleted user screen

    private final Locator deletedUsersPageHeading;
    private final Locator deletedUsersSearchInput;
    private final Locator deletedUsersSearchButton;
    private final Locator deletedUsersTable;
    private final Locator deletedUsersTableRows;
    private final Locator backToUserAuditTableLink;

    private final Locator emailSortLink;
    private final Locator deletedBySortLink;
    private final Locator deletionDateSortLink;
    private final Locator deleteReasonSortLink;

    private final Locator nameColumnHeader;
    private final Locator emailColumnHeader;
    private final Locator deletedByColumnHeader;
    private final Locator deletionDateColumnHeader;
    private final Locator deleteReasonColumnHeader;

    private final Locator deletedUsersNextPageLink;
    private final Locator deletedUsersCurrentPage;
    private final Locator deletedUsersPageInformation;

    public AuditPage(Page page, int port) {
        this.page = page;
        this.url = "http://localhost:" + port + "/admin/users/audit";

        log.info("Navigating to Audit Users page: {}", url);
        page.navigate(url);


        // Page Header
        this.pageHeader = page.locator("h1.govuk-heading-l");

        // Firm Search Autocomplete
        this.firmSearchInput = page.locator("#firmSearch");
        this.firmSearchHiddenId = page.locator("#selectedFirmId");
        this.firmSearchListbox = page.locator("#firmSearch__listbox");
        this.firmOptionRows = page.locator("#firmSearch__listbox .autocomplete__option");

        // Name/Email Search
        this.nameOrEmailSearchInput = page.locator("#search");
        this.searchButton = page.locator("button.govuk-button[type='submit']:has-text('Search')");
        this.toggleFiltersButton = page.locator("#toggle-filters-btn");
        this.applyFiltersButton = page.locator("#filter-panel button[type='submit']:has-text('Apply filters')");

        // Filters
        this.silasRoleFilter = page.locator("#silasRole");
        this.appAccessFilter = page.locator("#selectedAppId");
        this.userTypeFilter = page.locator("#selectedUserType");

        // Sort Buttons
        this.nameSortButton = page.locator("button.sort-button[data-sort='name']");
        this.emailSortButton = page.locator("button.sort-button[data-sort='email']");
        this.typeSortButton = page.locator("button.sort-button[data-sort='userType']");
        this.firmSortButton = page.locator("button.sort-button[data-sort='firm']");
        this.statusSortButton = page.locator("button.sort-button[data-sort='accountStatus']");
        this.multiFirmSortButton = page.locator("button.sort-button[data-sort='isMultiFirmUser']");
        this.profilesSortButton = page.locator("button.sort-button[data-sort='profileCount']");

        // Table Rows
        this.tableRows = page.locator("tbody.govuk-table__body tr.govuk-table__row");
        this.tableHeaders = page.locator("thead.govuk-table__head th.govuk-table__header");


        // Pagination
        this.paginationNumbers = page.locator(".govuk-pagination__list .govuk-pagination__item a");
        this.paginationCurrentPage = page.locator("li.govuk-pagination__item--current a");
        this.paginationNextPage = page.locator(".govuk-pagination__next a");

        // Results Summary
        this.resultsSummary = page.locator(".moj-pagination__results");



        // CSV Export
        this.exportCsvButton = page.locator("#exportCsvButton");
        this.csvErrorBanner = page.locator("#csv-export-error-summary");
        this.viewDeletedUsersLink = page.locator("a:has-text('View all deleted users')");
        this.auditTable = page.locator("#audit-table");
        this.viewAllDeletedUsersButton = page.locator("a[href='/admin/users/audit/deleted']");


        //Deleted user screen

        this.deletedUsersPageHeading =
                page.locator("h1.govuk-heading-xl");

        this.deletedUsersSearchInput =
                page.locator("#search");

        this.deletedUsersSearchButton =
                page.locator("form[action='/admin/users/audit/deleted'] button[type='submit']");

        this.deletedUsersTable =
                page.locator("table[aria-label='Deleted users table']");

        this.deletedUsersTableRows =
                deletedUsersTable.locator("tbody tr");

        this.backToUserAuditTableLink =
                page.locator("a[href='/admin/users/audit']");

        this.emailSortLink =
                page.locator("a[href*='sort=userEmail']");

        this.deletedBySortLink =
                page.locator("a[href*='sort=statusChangedBy']");

        this.deletionDateSortLink =
                page.locator("a[href*='sort=statusChangedDate']");

        this.deleteReasonSortLink =
                page.locator("a[href*='sort=deleteReason']");

        this.nameColumnHeader =
                deletedUsersTable.locator("thead th").nth(0);

        this.emailColumnHeader =
                deletedUsersTable.locator("thead th").nth(1);

        this.deletedByColumnHeader =
                deletedUsersTable.locator("thead th").nth(2);

        this.deletionDateColumnHeader =
                deletedUsersTable.locator("thead th").nth(3);

        this.deleteReasonColumnHeader =
                deletedUsersTable.locator("thead th").nth(4);

        this.deletedUsersNextPageLink =
                page.locator(".govuk-pagination__next a");

        this.deletedUsersCurrentPage =
                page.locator(".govuk-pagination__item--current");

        this.deletedUsersPageInformation =
                page.locator("p.govuk-body.govuk-\\!-margin-top-4");


    }

    public void assertUserIsPresent(String email) {
        log.info("Verifying user exists with email: {}", email);

        nameOrEmailSearchInput.clear();
        nameOrEmailSearchInput.fill(email);
        searchButton.click();

        page.waitForLoadState(LoadState.NETWORKIDLE);

        Locator match = page.locator(
                "tbody.govuk-table__body tr.govuk-table__row td:nth-of-type(2):text('" + email + "')"
        );

        Assertions.assertTrue(
                match.count() > 0,
                "Expected user with email '" + email + "' to appear in the results, but no match was found."
        );
    }

    public void populateFirmField(String firmName) {
        openFiltersPanel();
        firmSearchInput.fill("");
        firmSearchInput.fill(firmName);
    }

    public void searchByFirmCode(String firmCode) {
        log.info("Searching by firm code: {}", firmCode);
        nameOrEmailSearchInput.fill(firmCode);
    }

    public void filterBySilasRole(String role) {
        log.info("Filtering by SiLAS role: {}", role);
        openFiltersPanel();
        silasRoleFilter.selectOption(role);
        applyFilters();
    }

    public void filterByAppAccess(String appId) {
        log.info("Filtering by app access ID: {}", appId);
        openFiltersPanel();
        appAccessFilter.selectOption(appId);
        applyFilters();
    }

    public void filterByUserType(String userType) {
        log.info("Filtering by user type: {}", userType);
        openFiltersPanel();

        Locator userTypeCheckbox = page.locator("#userType-" + userType);
        userTypeCheckbox.check();
        assertThat(userTypeFilter).hasValue(userType);
        applyFilters();
    }

    private void openFiltersPanel() {
        if (!"true".equals(toggleFiltersButton.getAttribute("aria-expanded"))) {
            toggleFiltersButton.click();
        }
        assertThat(silasRoleFilter).isVisible();
    }

    private void applyFilters() {
        applyFiltersButton.click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public List<String> getTableHeaderTexts() {
        return tableHeaders.allInnerTexts()
                .stream()
                .map(String::trim)
                .toList();
    }

    public void assertEachRowHasColumnCount(int expectedColumns) {
        int rowCount = tableRows.count();

        for (int i = 0; i < rowCount; i++) {
            Locator cells = tableRows.nth(i).locator("td.govuk-table__cell");

            Assertions.assertEquals(
                    expectedColumns,
                    cells.count(),
                    "Row " + (i + 1) + " does not have " + expectedColumns + " columns"
            );
        }
    }

    public void assertOnAuditPage() {
        Assertions.assertEquals(
                "User Access Audit Table",
                pageHeader.innerText().trim(),
                "Expected to be on the Audit Users page"
        );
    }

    public void assertAccessForbidden() {
        Locator forbiddenHeader = page.locator(
                "h1.govuk-heading-l:has-text('Access forbidden')"
        );

        Assertions.assertTrue(
                forbiddenHeader.isVisible(),
                "Expected 'Access forbidden' page to be displayed"
        );

        Assertions.assertTrue(
                page.locator("text=You don't have permission to access this page.").isVisible(),
                "Expected access denied message to be displayed"
        );
    }

    public void assertExportCsvButtonVisible() {
        log.info("Asserting Export CSV button is visible");
        Assertions.assertTrue(exportCsvButton.isVisible(), "Expected Export CSV button to be visible");
    }

    public void assertExportCsvButtonNotDisabled() {
        log.info("Asserting Export CSV button is not disabled");
        Assertions.assertFalse(
                exportCsvButton.isDisabled(),
                "Export CSV button should not be disabled"
        );
        Assertions.assertFalse(
                exportCsvButton.getAttribute("class").contains("govuk-button--disabled"),
                "Export CSV button should not have the disabled class"
        );
    }

    public void clickExportCsv() {
        log.info("Clicking Export CSV button");
        exportCsvButton.click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    public void assertCsvErrorBannerVisible() {
        log.info("Asserting CSV error banner is visible");
        Assertions.assertTrue(
                csvErrorBanner.isVisible(),
                "Expected CSV export error summary banner to be visible"
        );
    }

    public void assertCsvErrorBannerHidden() {
        log.info("Asserting CSV error banner is hidden");
        Assertions.assertFalse(
                csvErrorBanner.isVisible(),
                "Expected CSV export error summary banner to be hidden"
        );
    }

    public void assertCsvErrorBannerContainsText(String expectedText) {
        log.info("Asserting CSV error banner contains text: {}", expectedText);
        Assertions.assertTrue(
                csvErrorBanner.innerText().contains(expectedText),
                "Expected CSV error banner to contain: " + expectedText
        );
    }

    public void assertViewDeletedUsersLinkVisible() {
        log.info("Asserting 'View all deleted users' link is visible");
        Assertions.assertTrue(viewDeletedUsersLink.isVisible(), "Expected 'View all deleted users' link to be visible");
    }

    public double getExportCsvButtonY() {
        Assertions.assertTrue(exportCsvButton.isVisible(), "Export CSV button must be visible before comparing position");
        var box = exportCsvButton.boundingBox();
        Assertions.assertNotNull(box, "Export CSV button bounding box must not be null");
        return box.y;
    }

    public double getAuditTableY() {
        Assertions.assertTrue(auditTable.isVisible(), "Audit table must be visible before comparing position");
        var box = auditTable.boundingBox();
        Assertions.assertNotNull(box, "Audit table bounding box must not be null");
        return box.y;
    }

    public void assertDeletedUsersPageDisplayed() {
        log.info("Verifying Deleted Users page is displayed");

        assertThat(deletedUsersPageHeading).isVisible();
        assertThat(deletedUsersPageHeading).hasText("Deleted Users");
    }

    public void searchForDeletedUser(String email) {
        log.info("Searching deleted users using email: {}", email);

        deletedUsersSearchInput.fill(email);
        deletedUsersSearchButton.click();
    }

    public void assertDeletedUserDisplayed(String email) {
        log.info("Verifying deleted user is displayed with email: {}", email);

        Locator deletedUserRow = getDeletedUserRowByEmail(email);

        assertThat(deletedUserRow).isVisible();
        assertThat(deletedUserRow.locator("td").nth(1)).hasText(email);
    }

    private Locator getDeletedUserRowByEmail(String email) {
        return deletedUsersTable
                .locator("tbody tr")
                .filter(new Locator.FilterOptions().setHasText(email));
    }

    public void clickViewAllDeletedUsers() {
        viewAllDeletedUsersButton.click();
    }


}
