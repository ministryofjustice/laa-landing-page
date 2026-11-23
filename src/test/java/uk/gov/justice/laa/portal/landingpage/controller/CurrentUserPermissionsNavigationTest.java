package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import uk.gov.justice.laa.portal.landingpage.client.RoleAssignmentContract;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserType;
import uk.gov.justice.laa.datauserapi.contracts.dto.AppRoleView;
import uk.gov.justice.laa.datauserapi.contracts.response.UserProfileDetailView;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserPermissions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserPermissionsNavigationTest {

    @Test
    void manageUsersNavigationRequiresMatchingLocalAndApiPermissionsWhenEnabled() {
        var context = new Context();
        context.setVariable("isAdminUser", true);
        context.setVariable("canViewExternalUsers", true);
        context.setVariable("canViewInternalUsers", false);

        var engine = templateEngine();
        assertThat(render(engine, context)).contains("Manage your users");

        context.setVariable("userDataPermissions", new CurrentUserPermissions(List.of()));
        assertThat(render(engine, context)).doesNotContain("Manage your users");

        context.setVariable("userDataPermissions", new CurrentUserPermissions(List.of("VIEW_INTERNAL_USER")));
        assertThat(render(engine, context)).doesNotContain("Manage your users");

        context.setVariable("userDataPermissions", new CurrentUserPermissions(List.of("VIEW_EXTERNAL_USER")));
        assertThat(render(engine, context)).contains("Manage your users");

        context.setVariable("isAdminUser", false);
        assertThat(render(engine, context)).doesNotContain("Manage your users");
    }

    @Test
    void apiBackedProfileRendersFirmAndAssignedRolesWithoutBypassingManageAccessHint() {
        var servletContext = new MockServletContext();
        var webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        var exchange = webApplication.buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
        var context = new WebContext(exchange);
        UUID id = UUID.randomUUID();
        var role = new AppRoleView(null, null, "Billing", null, "Billing Approver", null,
                false, null, false, null, null, List.of());
        var profile = new UserProfileDetailView(id, true, UserType.EXTERNAL, "bob@example.test", "Bob",
                "Legal Services", "12345", false, null, null, true, null, false, true, List.of(role), List.of());
        context.setVariables(Map.of("profile", profile, "user", Map.of("id", id), "externalUser", true,
                "isAccessGranted", false, "canGrantUserAccess", true, "userDataPermissions", new CurrentUserPermissions(List.of())));

        assertThat(templateEngine().process("manage-user-api", Set.of("#main-content"), context))
                .contains("Bob", "Legal Services", "Billing Approver")
                .doesNotContain("Manage access");
    }

    @Test
    void manageAccessRequiresAssignmentPermissionForExternalUsersWhenEnabled() {
        var servletContext = new MockServletContext();
        var webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        var exchange = webApplication.buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
        var context = new WebContext(exchange);
        var engine = templateEngine();
        context.setVariables(Map.of("user", Map.of("id", "target"), "externalUser", true,
                "isAccessGranted", false, "canGrantUserAccess", true));

        assertThat(renderManageAccess(engine, context)).contains("Manage access");

        context.setVariable("userDataPermissions", new CurrentUserPermissions(List.of("EDIT_EXTERNAL_USER", "REMOVE_EXTERNAL_USER_ROLES")));
        assertThat(renderManageAccess(engine, context)).doesNotContain("Manage access");

        context.setVariable("externalUser", false);
        assertThat(renderManageAccess(engine, context)).contains("Manage access");
        context.setVariable("externalUser", true);

        context.setVariable("userDataPermissions", new CurrentUserPermissions(List.of("ASSIGN_EXTERNAL_USER_ROLES")));
        assertThat(renderManageAccess(engine, context)).contains("Manage access", "/admin/users/manage/target/grant-access");

        context.setVariable("canGrantUserAccess", false);
        assertThat(renderManageAccess(engine, context)).doesNotContain("Manage access");
        context.setVariable("canGrantUserAccess", true);
        context.setVariable("isAccessGranted", true);
        assertThat(renderManageAccess(engine, context)).doesNotContain("Manage access");
    }

    @Test
    void servicesChangeActionAllowsRoleAssignmentOrRemovalPermission() {
        var servletContext = new MockServletContext();
        var webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        var exchange = webApplication.buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
        var context = new WebContext(exchange);
        context.setVariables(Map.of("user", Map.of("id", "target"), "externalUser", true,
                "isAccessGranted", true, "userHasRoles", true, "canEditUserRoleAssignments", true,
                "userDataPermissions", new CurrentUserPermissions(List.of("REMOVE_EXTERNAL_USER_ROLES"))));

        assertThat(renderServiceChangeAction(templateEngine(), context))
                .contains("Change", "/admin/users/edit/target/apps");

        context.setVariable("userDataPermissions", new CurrentUserPermissions(List.of("EDIT_EXTERNAL_USER")));
        assertThat(renderServiceChangeAction(templateEngine(), context)).doesNotContain("Change");

        context.setVariable("userDataPermissions", new CurrentUserPermissions(List.of("ASSIGN_EXTERNAL_USER_ROLES")));
        assertThat(renderServiceChangeAction(templateEngine(), context)).contains("Change");
    }

    @Test
    void apiRoleAssignmentErrorSummaryIsInitializedAndLinkedToItsHeading() {
        var servletContext = new MockServletContext();
        var webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        var exchange = webApplication.buildExchange(new MockHttpServletRequest(servletContext), new MockHttpServletResponse());
        var context = new WebContext(exchange);
        UUID id = UUID.randomUUID();
        var profile = new UserProfileDetailView(id, true, UserType.EXTERNAL, "bob@example.test", "Bob",
                "Legal Services", "12345", false, null, null, true, null, false, true, List.of(), List.of());
        context.setVariables(Map.of("profile", profile,
                "assignableRoles", List.of(new RoleAssignmentContract.Role(UUID.randomUUID(), "Billing Approver")),
                "errorMessage", "Select at least one role."));

        String rendered = templateEngine().process("assign-user-roles-api", Set.of("#main-content"), context);

        assertThat(rendered)
                .contains("data-module=\"govuk-error-summary\"")
                .contains("aria-labelledby=\"assign-role-error-summary-title\"")
                .contains("id=\"assign-role-error-summary-title\"")
                .contains("aria-describedby=\"assign-role-error-summary-description\"")
                .contains("id=\"assign-role-error-summary-description\"")
                .contains("href=\"#role-options\"", "Select at least one role.");
    }

    private SpringTemplateEngine templateEngine() {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private String render(SpringTemplateEngine engine, Context context) {
        return engine.process("home", Set.of("manageUsersNavigation"), context);
    }

    private String renderManageAccess(SpringTemplateEngine engine, WebContext context) {
        return engine.process("manage-user", Set.of("manageAccessAction"), context);
    }

    private String renderServiceChangeAction(SpringTemplateEngine engine, WebContext context) {
        return engine.process("manage-user", Set.of("serviceChangeAction"), context);
    }
}
