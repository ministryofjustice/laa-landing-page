package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.playwright.pages.AdminPage;

import java.util.stream.Stream;

@Disabled("Temporarily disabled")
public class AdminPageTest extends BaseFrontEndTest {

    private AdminPage loginAndGetAdminPage(TestUser user) {
        loginAs(user.email);
        page.navigate(String.format("http://localhost:%d/admin/silas-administration", port));
        return new AdminPage(page, port);
    }

    @Test
    @DisplayName("SiLAS Administration page loads and shows expected heading")
    void adminPage_loadsAndShowsHeading() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.GLOBAL_ADMIN);
        adminPage.assertOnPage();
    }

    @Test
    @DisplayName("Admin Services tab shows expected columns and at least 1 row")
    void adminServicesTab_hasExpectedColumnsAndRows() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.GLOBAL_ADMIN);

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
        AdminPage adminPage = loginAndGetAdminPage(TestUser.GLOBAL_ADMIN);

        adminPage.assertLegalAidServicesTableColumns()
                .assertReorderLegalAidServicesButtonVisible();

        Assertions.assertTrue(
                adminPage.getLegalAidServicesRowCount() > 0,
                "Expected Legal Aid Services table to contain at least 1 row"
        );
    }

    @Test
    @DisplayName("Roles and Permissions tab shows expected columns and action buttons")
    void rolesAndPermissionsTab_hasExpectedColumnsAndButtons() {
        AdminPage adminPage = loginAndGetAdminPage(TestUser.GLOBAL_ADMIN);

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
        AdminPage adminPage = loginAndGetAdminPage(TestUser.GLOBAL_ADMIN);

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

    private static Stream<TestUser> nonPermittedUsers() {
        return Stream.of(
                TestUser.INTERNAL_USER_VIEWER,
                TestUser.INTERNAL_USER_MANAGER,
                TestUser.EXTERNAL_USER_VIEWER,
                TestUser.EXTERNAL_USER_MANAGER,
                TestUser.EXTERNAL_USER_ADMIN,
                TestUser.INFORMATION_AND_ASSURANCE,
                TestUser.NO_ROLES
        );
    }
}
