package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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

        // Pagination
        this.paginationNumbers = page.locator(".govuk-pagination__list .govuk-pagination__item a");
        this.paginationCurrentPage = page.locator("li.govuk-pagination__item--current a");
        this.paginationNextPage = page.locator(".govuk-pagination__next a");

        // Results Summary
        this.resultsSummary = page.locator(".moj-pagination__results");

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

}

