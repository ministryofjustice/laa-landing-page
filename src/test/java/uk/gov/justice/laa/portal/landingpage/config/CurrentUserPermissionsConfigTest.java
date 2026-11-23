package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.landingpage.client.DoNothingUserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.LiveUserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.service.CurrentUserPermissionsService;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CurrentUserPermissionsConfigTest {

    @Test
    void startsWithoutPermissionsConfigurationWhenBothFlagsAreOff() {
        try (var context = context(Map.of(
                "feature.flag.current-user-permissions", "false",
                "feature.flag.manage-users.data-api", "false",
                "app.enable.user.data.api.call", "false",
                "SPRING_SESSION_JDBC_ENABLED", "false"))) {
            context.refresh();
            assertThat(context.getBeansOfType(CurrentUserPermissionsConfig.class)).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"feature.flag.current-user-permissions", "feature.flag.manage-users.data-api"})
    void startsEnabledFeatureWhenApiCallsAreDisabled(String flag) {
        try (var context = context(Map.of(flag, "true",
                "app.enable.user.data.api.call", "false", "SPRING_SESSION_JDBC_ENABLED", "true"))) {
            context.refresh();
            assertThat(context.getBeansOfType(CurrentUserPermissionsConfig.class)).hasSize(1);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"feature.flag.current-user-permissions", "feature.flag.manage-users.data-api"})
    void startsEnabledFeatureWhenApiCallsAndJdbcSessionsAreEnabled(String flag) {
        try (var context = context(Map.of(flag, "true",
                "app.enable.user.data.api.call", "true", "SPRING_SESSION_JDBC_ENABLED", "true"))) {
            context.refresh();
            assertThat(context.getBeansOfType(CurrentUserPermissionsConfig.class)).hasSize(1);
        }
    }

    @Test
    void rejectsEnabledFeatureWithoutJdbcSessions() {
        try (var context = context(Map.of("feature.flag.current-user-permissions", "true",
                "app.enable.user.data.api.call", "true", "SPRING_SESSION_JDBC_ENABLED", "false"))) {
            assertThatThrownBy(context::refresh).hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("Current-user permissions require SPRING_SESSION_JDBC_ENABLED=true");
        }
    }

    @Test
    void disabledApiSelectsNoOpClientAndDoesNotExchangeTokens() {
        var tokens = mock(OboTokenService.class);
        var restClient = mock(RestClient.class);
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("app.enable.user.data.api.call", "false")));
            context.registerBean(OboTokenService.class, () -> tokens);
            context.registerBean(RestClient.class, () -> restClient);
            context.register(LiveUserDataApiClient.class, DoNothingUserDataApiClient.class);
            context.refresh();

            var client = context.getBean(UserDataApiClient.class);
            assertThat(client).isInstanceOf(DoNothingUserDataApiClient.class);
            assertThatThrownBy(() -> client.currentUserPermissions("login", "oid", "cid"))
                    .isInstanceOf(UserDataApiClientException.class)
                    .satisfies(error -> assertThat(((UserDataApiClientException) error).getStatusCode()).isEqualTo(503));
            verifyNoInteractions(tokens, restClient);
        }
    }

    private AnnotationConfigApplicationContext context(Map<String, Object> properties) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.registerBean(CurrentUserPermissionsService.class, () -> mock(CurrentUserPermissionsService.class));
        context.register(CurrentUserPermissionsConfig.class);
        return context;
    }
}
