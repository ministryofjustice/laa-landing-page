package uk.gov.justice.laa.portal.landingpage.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AdminPage {

    private static final Logger log = LoggerFactory.getLogger(AdminPage.class);

    private final Page page;
    private final String url;

    // Header
    private final Locator pageHeader;

    // Tabs
    private final Locator adminServicesTab;
    private final Locator legalAidServicesTab;
    private final Locator rolesAndPermissionsTab;

    // Panels
    private final Locator adminServicesPanel;
    private final Locator legalAidServicesPanel;
    private final Locator rolesAndPermissionsPanel;

    // --- Admin Services tab content ---
    private final Locator adminServicesTable;
    private final Locator adminServicesHeaders;
    private final Locator adminServicesRows;
    private final Locator reorderAdminServicesButton;

    // --- Legal Aid Services tab content ---
    private final Locator legalAidServicesTable;
    private final Locator legalAidServicesHeaders;
    private final Locator legalAidServicesRows;
    private final Locator reorderLegalAidServicesButton;

    // --- Roles and Permissions tab content ---
    private final Locator createNewRoleButton;
    private final Locator deleteRoleButton;

    private final Locator appFilterSelect;
    private final Locator rolesTable;
    private final Locator rolesHeaders;
    private final Locator rolesRows;
    private final Locator reorderRolesButton;

    private final Locator accessForbiddenHeading;
    private final Locator accessForbiddenMessage;
    private final Locator goToHomepageButton;
    private final Locator signOutAndTryAgainButton;

    public AdminPage(Page page, int port) {
        this.page = page;
        this.url = "http://localhost:" + port + "/admin/silas-administration";

        log.info("Navigating to SiLAS Administration page: {}", url);
        page.navigate(url);
        page.waitForLoadState(LoadState.NETWORKIDLE);

        // Header
        this.pageHeader = page.locator("h1.govuk-heading-xl");

        // Tabs
        this.adminServicesTab = page.locator("#tab_admin-apps");
        this.legalAidServicesTab = page.locator("#tab_apps");
        this.rolesAndPermissionsTab = page.locator("#tab_roles");

        // Panels
        this.adminServicesPanel = page.locator("#admin-apps");
        this.legalAidServicesPanel = page.locator("#apps");
        this.rolesAndPermissionsPanel = page.locator("#roles");

        // Admin Services
        this.adminServicesTable = adminServicesPanel.locator("table.govuk-table");
        this.adminServicesHeaders = adminServicesTable.locator("thead th.govuk-table__header");
        this.adminServicesRows = adminServicesTable.locator("tbody.govuk-table__body tr.govuk-table__row");
        this.reorderAdminServicesButton = adminServicesPanel.locator("button.govuk-button--secondary:has-text('Reorder admin services')");

        // Legal Aid Services
        this.legalAidServicesTable = legalAidServicesPanel.locator("table.govuk-table");
        this.legalAidServicesHeaders = legalAidServicesTable.locator("thead th.govuk-table__header");
        this.legalAidServicesRows = legalAidServicesTable.locator("tbody.govuk-table__body tr.govuk-table__row");
        this.reorderLegalAidServicesButton = legalAidServicesPanel.locator("button.govuk-button:has-text('Reorder legal aid services')");

        // Roles & Permissions
        this.createNewRoleButton = rolesAndPermissionsPanel.locator("button.govuk-button:has-text('Create a new role')");
        this.deleteRoleButton = rolesAndPermissionsPanel.locator("button.govuk-button--warning:has-text('Delete a role')");

        this.appFilterSelect = rolesAndPermissionsPanel.locator("#appFilter");

        this.rolesTable = rolesAndPermissionsPanel.locator("table.govuk-table");
        this.rolesHeaders = rolesTable.locator("thead th.govuk-table__header");
        this.rolesRows = rolesTable.locator("tbody.govuk-table__body tr.govuk-table__row");
        this.reorderRolesButton = rolesAndPermissionsPanel.locator("button.govuk-button:has-text('Reorder application roles')");

        //Forbidden Access Page
        this.accessForbiddenHeading = page.locator("h1.govuk-heading-l:has-text('Access forbidden')");
        this.accessForbiddenMessage = page.locator("p.govuk-body:has-text(\"You don't have permission to access this page.\")");
        this.goToHomepageButton = page.locator("a.govuk-button:has-text('Go to homepage')");
        this.signOutAndTryAgainButton = page.locator("a.govuk-button.govuk-button--secondary:has-text('Sign out and try again')");
    }

    // -------------------------
    // Page-level assertions
    // -------------------------

    public AdminPage assertOnPage() {
        assertThat(pageHeader).isVisible();
        assertThat(pageHeader).hasText("SiLAS Administration");
        return this;
    }

    public AdminPage assertAccessForbiddenPage() {
        assertThat(accessForbiddenHeading).isVisible();
        assertThat(accessForbiddenMessage).isVisible();
        assertThat(goToHomepageButton).isVisible();
        assertThat(signOutAndTryAgainButton).isVisible();

        // Ensure admin page contract isn't visible at the same time
        assertThat(pageHeader).not().isVisible();

        return this;
    }

    // -------------------------
    // Tab navigation
    // -------------------------

    public AdminPage goToAdminServicesTab() {
        adminServicesTab.click();
        assertThat(adminServicesPanel).isVisible();
        return this;
    }

    public AdminPage goToLegalAidServicesTab() {
        legalAidServicesTab.click();
        assertThat(legalAidServicesPanel).isVisible();
        return this;
    }

    public AdminPage goToRolesAndPermissionsTab() {
        rolesAndPermissionsTab.click();
        assertThat(rolesAndPermissionsPanel).isVisible();
        return this;
    }

    // -------------------------
    // Admin Services
    // -------------------------

    public AdminPage assertAdminServicesTableColumns() {
        goToAdminServicesTab();
        assertThat(adminServicesTable).isVisible();

        assertHeadersContainExactly(
                adminServicesHeaders,
                List.of("Service name", "Description", "Code", "Order", "")
        );
        return this;
    }

    public int getAdminServicesRowCount() {
        goToAdminServicesTab();
        return adminServicesRows.count();
    }

    public AdminPage assertReorderAdminServicesButtonVisible() {
        goToAdminServicesTab();
        assertThat(reorderAdminServicesButton).isVisible();
        return this;
    }

    // -------------------------
    // Legal Aid Services
    // -------------------------

    public AdminPage assertLegalAidServicesTableColumns() {
        goToLegalAidServicesTab();
        assertThat(legalAidServicesTable).isVisible();

        assertHeadersContainExactly(
                legalAidServicesHeaders,
                List.of("Service name", "Description", "Code", "Order", "")
        );
        return this;
    }

    public int getLegalAidServicesRowCount() {
        goToLegalAidServicesTab();
        return legalAidServicesRows.count();
    }

    public AdminPage assertReorderLegalAidServicesButtonVisible() {
        goToLegalAidServicesTab();
        assertThat(reorderLegalAidServicesButton).isVisible();
        return this;
    }

    // -------------------------
    // Roles & Permissions
    // -------------------------

    public AdminPage assertRolesTableColumns() {
        goToRolesAndPermissionsTab();
        assertThat(rolesTable).isVisible();

        assertHeadersContainExactly(
                rolesHeaders,
                List.of("Role name", "Description", "Code", "Order", "Grouping", "")
        );
        return this;
    }

    public AdminPage assertRolesActionButtonsVisible() {
        goToRolesAndPermissionsTab();
        assertThat(createNewRoleButton).isVisible();
        assertThat(deleteRoleButton).isVisible();
        return this;
    }

    public AdminPage filterRolesByApplication(String applicationVisibleText) {
        goToRolesAndPermissionsTab();

        assertThat(appFilterSelect).isVisible();
        appFilterSelect.selectOption(applicationVisibleText);

        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(rolesAndPermissionsPanel).isVisible();
        return this;
    }

    public int getRolesRowCount() {
        goToRolesAndPermissionsTab();
        return rolesRows.count();
    }

    public AdminPage assertReorderRolesButtonVisible() {
        goToRolesAndPermissionsTab();
        assertThat(reorderRolesButton).isVisible();
        return this;
    }

    // -------------------------
    // Helpers
    // -------------------------

    private void assertHeadersContainExactly(Locator headerLocator, List<String> expectedHeaders) {
        List<String> actualHeaders = headerLocator.allInnerTexts()
                .stream()
                .map(s -> s == null ? "" : s.trim())
                .toList();

        if (actualHeaders.size() != expectedHeaders.size()) {
            throw new AssertionError("Header count mismatch. Expected " + expectedHeaders.size()
                    + " but was " + actualHeaders.size() + ". Actual: " + actualHeaders);
        }

        for (int i = 0; i < expectedHeaders.size(); i++) {
            String expected = expectedHeaders.get(i);
            String actual = actualHeaders.get(i);

            if (!expected.equals(actual)) {
                throw new AssertionError("Header mismatch at index " + i
                        + ". Expected '" + expected + "' but was '" + actual + "'. Full actual: " + actualHeaders);
            }
        }
    }
}
