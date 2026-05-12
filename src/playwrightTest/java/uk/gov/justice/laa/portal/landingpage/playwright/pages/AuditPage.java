package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;


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

    private final Locator silasRoleFilter;
    private final Locator appAccessFilter;
    private final Locator userTypeFilter;

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
        this.searchButton = page.locator("button.govuk-button.govuk-button--secondary");

        // Filters
        this.silasRoleFilter = page.locator("#silasRoleFilter");
        this.appAccessFilter = page.locator("#appAccessFilter");
        this.userTypeFilter = page.locator("#userTypeFilter");

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

    public void searchAndSelectFirm(String firmName) {
        log.info("Searching and selecting firm: {}", firmName);

        firmSearchInput.fill("");
        firmSearchInput.fill(firmName);

        firmSearchListbox.waitFor();
        firmOptionRows.first().waitFor();

        firmOptionRows.first().click();

        Assertions.assertFalse(
                firmSearchHiddenId.inputValue().isBlank(),
                "Expected selectedFirmId to be populated after selecting firm"
        );

        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public void filterBySilasRole(String role) {
        log.info("Filtering by SiLAS role: {}", role);
        silasRoleFilter.selectOption(role);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public void filterByAppAccess(String appId) {
        log.info("Filtering by app access ID: {}", appId);
        appAccessFilter.selectOption(appId);
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    public void filterByUserType(String userType) {
        log.info("Filtering by user type: {}", userType);
        userTypeFilter.selectOption(userType);
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
        var box = exportCsvButton.boundingBox();
        return box != null ? box.y : 0;
    }

    public double getAuditTableY() {
        var box = auditTable.boundingBox();
        return box != null ? box.y : 0;
    }

    public void clickExportCsvExpectingDownload() {
        log.info("Clicking Export CSV button (expecting download, not error banner)");
        // Use Playwright download interception to avoid actually saving the file
        page.waitForDownload(exportCsvButton::click);
    }


}
