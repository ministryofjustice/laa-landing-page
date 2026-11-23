package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.ResponseBody;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.config.CurrentUserPermissionsConfig;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserPermissions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
class CurrentUserPermissionsJdbcIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persistsPermissionsWithTheSessionAndReloadsOnlyForNewSessions() throws Exception {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new ResourceDatabasePopulator(new ClassPathResource(
                "org/springframework/session/jdbc/schema-postgresql.sql")).execute(dataSource);
        var jdbc = new JdbcTemplate(dataSource);
        var repository = new JdbcIndexedSessionRepository(jdbc, new TransactionTemplate(new JdbcTransactionManager(dataSource)));
        var sessions = new SessionRepositoryFilter<>(repository);
        sessions.setHttpSessionIdResolver(HeaderHttpSessionIdResolver.xAuthToken());

        var authorities = AuthorityUtils.createAuthorityList("USER");
        var principal = new DefaultOAuth2User(authorities, Map.of("sub", "actor", "oid", "actor-oid"), "sub");
        var authentication = new OAuth2AuthenticationToken(principal, authorities, "azure");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var authorizedClients = mock(OAuth2AuthorizedClientRepository.class);
        var authorizedClient = mock(OAuth2AuthorizedClient.class);
        var now = Instant.now();
        when(authorizedClient.getAccessToken()).thenReturn(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "login-token", now, now.plusSeconds(3600)));
        when(authorizedClients.loadAuthorizedClient(eq("azure"), eq(authentication), any())).thenReturn(authorizedClient);
        var client = mock(UserDataApiClient.class);
        when(client.currentUserPermissions(eq("login-token"), eq("actor-oid"), anyString())).thenReturn(new CurrentUserPermissions(List.of("VIEW_EXTERNAL_USER")),
                new CurrentUserPermissions(List.of("EDIT_EXTERNAL_USER")));
        var config = new CurrentUserPermissionsConfig(
                new CurrentUserPermissionsService(authorizedClients, client), true);
        var mvc = MockMvcBuilders.standaloneSetup(new Pages())
                .addFilters(sessions)
                .addMappedInterceptors(new String[]{"/home"}, config)
                .build();

        String firstSession = mvc.perform(get("/start")).andReturn().getResponse().getHeader("X-Auth-Token");
        mvc.perform(get("/home").header("X-Auth-Token", firstSession)).andExpect(status().isOk())
                .andExpect(model().attribute("userDataPermissions", notNullValue()));
        assertThat(snapshotRows(jdbc, firstSession)).isEqualTo(1);

        mvc.perform(get("/home").header("X-Auth-Token", firstSession)).andExpect(status().isOk());
        verify(client).currentUserPermissions(eq("login-token"), eq("actor-oid"), anyString());

        jdbc.update("UPDATE SPRING_SESSION SET EXPIRY_TIME = 0 WHERE SESSION_ID = ?", firstSession);
        repository.cleanUpExpiredSessions();
        assertThat(snapshotRows(jdbc, firstSession)).isZero();
        assertThat(repository.findById(firstSession)).isNull();

        String nextSession = mvc.perform(get("/start")).andReturn().getResponse().getHeader("X-Auth-Token");
        mvc.perform(get("/home").header("X-Auth-Token", nextSession)).andExpect(status().isOk());
        verify(client, times(2)).currentUserPermissions(eq("login-token"), eq("actor-oid"), anyString());
        assertThat(snapshotRows(jdbc, nextSession)).isEqualTo(1);

        repository.deleteById(nextSession);
        assertThat(snapshotRows(jdbc, nextSession)).isZero();
    }

    private Integer snapshotRows(JdbcTemplate jdbc, String id) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM SPRING_SESSION_ATTRIBUTES a
                JOIN SPRING_SESSION s ON a.SESSION_PRIMARY_ID = s.PRIMARY_ID
                WHERE s.SESSION_ID = ? AND a.ATTRIBUTE_NAME = ?
                """, Integer.class, id, CurrentUserPermissionsService.class.getName() + ".state");
    }

    @Controller
    static class Pages {
        @GetMapping("/start")
        @ResponseBody
        public String start(HttpSession session) {
            return "OK";
        }

        @GetMapping("/home")
        public String home() {
            return "forward:/";
        }
    }
}
