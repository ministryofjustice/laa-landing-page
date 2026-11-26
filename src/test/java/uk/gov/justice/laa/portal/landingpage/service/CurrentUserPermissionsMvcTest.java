package uk.gov.justice.laa.portal.landingpage.service;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.DoNothingUserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.CurrentUserPermissionsConfig;
import uk.gov.justice.laa.portal.landingpage.config.CustomAuthenticationSuccessHandler;
import uk.gov.justice.laa.portal.landingpage.config.OAuth2ExceptionResolver;
import uk.gov.justice.laa.portal.landingpage.controller.FirmDirectoryController;
import uk.gov.justice.laa.portal.landingpage.controller.UserController;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserPermissions;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProvider;
import uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProviderResolver;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class CurrentUserPermissionsMvcTest {

    private static final String PERMISSIONS = "userDataPermissions";
    private static final UUID PROFILE_ID = UUID.randomUUID();
    private static final CurrentUserPermissions GRANTED =
            new CurrentUserPermissions(List.of("EDIT_EXTERNAL_USER", "VIEW_EXTERNAL_USER", "VIEW_FIRM_DIRECTORY"));
    private static final CurrentUserPermissions DENIED = new CurrentUserPermissions(List.of());

    @Mock
    private OAuth2AuthorizedClientRepository authorizedClients;
    @Mock
    private UserDataApiClient client;
    @Mock
    private LoginService loginService;
    @Mock
    private FirmDirectoryProviderResolver firmDirectoryResolver;
    @Mock
    private FirmDirectoryProvider firmDirectory;

    private MockMvc mvc;
    private ProtectedPage protectedPage;
    private OAuth2AuthenticationToken authentication;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        protectedPage = new ProtectedPage();
        var firmController = new FirmDirectoryController(mock(AccessControlService.class), loginService, firmDirectoryResolver);
        ReflectionTestUtils.setField(firmController, "firmDirectoryEnabled", true);
        var exceptionResolver = new ExceptionHandlerExceptionResolver();
        exceptionResolver.afterPropertiesSet();
        var permissionsConfig = new CurrentUserPermissionsConfig(
                new CurrentUserPermissionsService(authorizedClients, client), true);
        mvc = MockMvcBuilders.standaloneSetup(protectedPage, firmController)
                .addMappedInterceptors(new String[]{"/home", "/admin/users"}, permissionsConfig)
                .setHandlerExceptionResolvers(new OAuth2ExceptionResolver(), exceptionResolver)
                .setViewResolvers(new InternalResourceViewResolver("/", ".html"))
                .build();
        authentication = authenticate("subject-one");
        session = new MockHttpSession();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loadsBeforeRenderingAndReusesSessionSnapshot() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenAnswer(invocation -> {
            assertThat(protectedPage.renders).isZero();
            return GRANTED;
        });

        assertHomePermissions(session, GRANTED);
        assertHomePermissions(session, GRANTED);
        verify(client).currentUserPermissions(eq("login-token"), eq("user-oid"), anyString());
        assertThat(protectedPage.renders).isEqualTo(2);
    }

    @Test
    void forwardsOrGeneratesCorrelationIdForPermissionsRequests() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenReturn(GRANTED);

        mvc.perform(get("/home").session(session).header("X-Correlation-ID", "request-id"))
                .andExpect(status().isOk());
        mvc.perform(get("/home").session(new MockHttpSession())).andExpect(status().isOk());
        mvc.perform(get("/home").session(new MockHttpSession()).header("X-Correlation-ID", " "))
                .andExpect(status().isOk());

        var ids = ArgumentCaptor.forClass(String.class);
        verify(client, times(3)).currentUserPermissions(eq("login-token"), eq("user-oid"), ids.capture());
        assertThat(ids.getAllValues().get(0)).isEqualTo("request-id");
        assertThat(UUID.fromString(ids.getAllValues().get(1))).isNotNull();
        assertThat(UUID.fromString(ids.getAllValues().get(2))).isNotNull();
    }

    @Test
    void anotherSessionForSameUserDoesNotReusePermissions() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenReturn(GRANTED, DENIED);

        assertHomePermissions(session, GRANTED);
        assertHomePermissions(new MockHttpSession(), DENIED);
        assertHomePermissions(session, GRANTED);
        verify(client, times(2)).currentUserPermissions(eq("login-token"), eq("user-oid"), anyString());
    }

    @Test
    void identityChangeCannotUsePreviousPermissionsEvenWhenLoadingFails() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenReturn(GRANTED)
                .thenThrow(new UserDataApiClientException("unavailable", 503)).thenReturn(DENIED);
        assertHomePermissions(session, GRANTED);

        stubToken(authenticate("different-subject"));
        assertHomeDenied(session);

        authenticate("subject-one");
        assertHomePermissions(session, DENIED);
    }

    @Test
    void successfulReloginRefreshesTheSnapshot() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenReturn(GRANTED, DENIED);
        assertHomePermissions(session, GRANTED);

        loginAgain();

        assertHomePermissions(session, DENIED);
    }

    @Test
    void invalidatedSessionCannotReusePermissions() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenReturn(GRANTED, DENIED);
        assertHomePermissions(session, GRANTED);

        // The configured logout handler invalidates the same HTTP session that expiry destroys.
        session.invalidate();

        assertThat(session.isInvalid()).isTrue();
        assertHomePermissions(new MockHttpSession(), DENIED);
    }

    @Test
    void unauthorizedRedirectsToLoginOnlyOnce() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenThrow(new UserDataApiClientException("unauthorized", 401));

        assertHomeDenied(session);
        mvc.perform(get("/admin/users").session(session)).andExpect(redirectedUrl("/oauth2/authorization/azure"));
        session.changeSessionId();
        loginAgain();
        mvc.perform(get("/admin/users").session(session)).andExpect(status().isForbidden())
                .andExpect(view().name("errors/error-403"))
                .andExpect(model().attributeDoesNotExist(PERMISSIONS));
        verify(client, times(3)).currentUserPermissions(eq("login-token"), eq("user-oid"), anyString());
        assertThat(protectedPage.renders).isEqualTo(1);
    }

    @Test
    void forbiddenUsesExistingUxWithoutRetrying() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenThrow(new UserDataApiClientException("forbidden", 403));

        assertHomeDenied(session);
        assertHomeDenied(session);
        mvc.perform(get("/admin/users").session(session)).andExpect(redirectedUrl("/not-authorised"));
        verify(client).currentUserPermissions(eq("login-token"), eq("user-oid"), anyString());
        assertThat(protectedPage.renders).isEqualTo(2);
    }

    @Test
    void networkFailureFailsClosedAndUserCanRetry() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString()))
                .thenThrow(new ResourceAccessException("network"))
                .thenThrow(new ResourceAccessException("network"))
                .thenReturn(GRANTED);

        assertHomeDenied(session);
        mvc.perform(get("/admin/users").session(session)).andExpect(status().isServiceUnavailable())
                .andExpect(view().name("errors/error-generic"))
                .andExpect(model().attributeDoesNotExist(PERMISSIONS));
        assertThat(protectedPage.renders).isEqualTo(1);
        assertHomePermissions(session, GRANTED);
    }

    @Test
    void unexpectedPermissionFailureDegradesHomeButStillFailsManagePages() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString()))
                .thenThrow(new IllegalStateException("unexpected failure"))
                .thenThrow(new IllegalStateException("unexpected failure"))
                .thenReturn(GRANTED);

        assertHomeDenied(session);
        assertThatThrownBy(() -> mvc.perform(get("/admin/users").session(session)))
                .hasRootCauseInstanceOf(IllegalStateException.class);
        assertHomePermissions(session, GRANTED);
        verify(client, times(3)).currentUserPermissions(eq("login-token"), eq("user-oid"), anyString());
    }

    @Test
    void nullPermissionsDegradeHomeWithoutCachingDenial() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString()))
                .thenReturn(null, GRANTED);

        assertHomeDenied(session);
        assertHomePermissions(session, GRANTED);
        verify(client, times(2)).currentUserPermissions(eq("login-token"), eq("user-oid"), anyString());
    }

    @Test
    void disabledDataApiFailsClosedWithoutMakingAnApiCall() throws Exception {
        stubToken(authentication);
        var config = new CurrentUserPermissionsConfig(
                new CurrentUserPermissionsService(authorizedClients, new DoNothingUserDataApiClient()), true);
        var disabledMvc = MockMvcBuilders.standaloneSetup(protectedPage)
                .addMappedInterceptors(new String[]{"/home", "/admin/users"}, config)
                .setViewResolvers(new InternalResourceViewResolver("/", ".html"))
                .build();

        var result = disabledMvc.perform(get("/home").session(session)).andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andReturn();
        assertNoViewPermissions((CurrentUserPermissions) result.getModelAndView().getModel().get(PERMISSIONS));
        disabledMvc.perform(get("/admin/users").session(session)).andExpect(status().isServiceUnavailable())
                .andExpect(view().name("errors/error-generic"));
        assertThat(protectedPage.renders).isEqualTo(1);
        verifyNoInteractions(client);
    }

    @Test
    void homeHeadRequestWithContextPathAlsoDegradesGracefully() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString()))
                .thenThrow(new UserDataApiClientException("unavailable", 503));

        var result = mvc.perform(head("/portal/home").contextPath("/portal").session(session))
                .andExpect(status().isOk()).andExpect(view().name("home")).andReturn();
        assertNoViewPermissions((CurrentUserPermissions) result.getModelAndView().getModel().get(PERMISSIONS));
    }

    @Test
    void missingTokenOnHomeDoesNotConsumeManagePageReauthenticationAttempt() throws Exception {
        assertHomeDenied(session);
        mvc.perform(get("/admin/users").session(session)).andExpect(redirectedUrl("/oauth2/authorization/azure"));
        verifyNoInteractions(client);
    }

    @Test
    void backendForbiddenStillUsesAccessDeniedUxDespiteCachedPermission() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenReturn(GRANTED);
        assertHomePermissions(session, GRANTED);
        when(firmDirectoryResolver.resolve()).thenReturn(firmDirectory);
        when(firmDirectory.getFirm("25966")).thenThrow(new UserDataApiClientException("denied by backend", 403));

        mvc.perform(get("/admin/firmDirectory/25966").session(session))
                .andExpect(redirectedUrl("/not-authorised"));
        verify(firmDirectory).getFirm("25966");
        verify(client).currentUserPermissions(eq("login-token"), eq("user-oid"), anyString());
    }

    @Test
    void profileReadIsDeniedBeforeQueryWhenCachedPermissionsLackViewAccess() throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenReturn(new CurrentUserPermissions(List.of("EDIT_EXTERNAL_USER")));
        var queries = mock(UserProfileQueryService.class);
        try (var context = profileSecurityContext(queries)) {
            profileMvc(context).perform(get("/admin/users/manage/{id}", PROFILE_ID).session(session))
                    .andExpect(redirectedUrl("/not-authorised"));
            verifyNoInteractions(queries);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"VIEW_EXTERNAL_USER", "VIEW_INTERNAL_USER"})
    void eitherCachedViewPermissionAllowsQueryButBackendCanStillDeny(String permission) throws Exception {
        stubToken(authentication);
        when(client.currentUserPermissions(eq("login-token"), eq("user-oid"), anyString())).thenReturn(new CurrentUserPermissions(List.of(permission)));
        var queries = mock(UserProfileQueryService.class);
        when(queries.getProfile(PROFILE_ID)).thenThrow(new UserDataApiClientException("denied by backend", 403));
        try (var context = profileSecurityContext(queries)) {
            var profileMvc = profileMvc(context);
            for (int i = 0; i < 2; i++) {
                profileMvc.perform(get("/admin/users/manage/{id}", PROFILE_ID).session(session))
                        .andExpect(redirectedUrl("/not-authorised"));
            }
            verify(queries, times(2)).getProfile(PROFILE_ID);
            verify(client).currentUserPermissions(eq("login-token"), eq("user-oid"), anyString());
        }
    }

    @Test
    void profileReadFailsClosedWithoutCachedPermissions() throws Exception {
        var queries = mock(UserProfileQueryService.class);
        try (var context = profileSecurityContext(queries)) {
            MockMvcBuilders.standaloneSetup(context.getBean(UserController.class)).build()
                    .perform(get("/admin/users/manage/{id}", PROFILE_ID).session(session))
                    .andExpect(redirectedUrl("/not-authorised"));
            verifyNoInteractions(queries, client);
        }
    }

    private AnnotationConfigApplicationContext profileSecurityContext(UserProfileQueryService queries) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("profile-security-test", Map.of(
                "feature.flag.manage-users.data-api", "true",
                "feature.flag.bulk.disable.user", "false",
                "feature.flag.disable.user", "false",
                "feature.flag.edit.user.details", "false")));
        context.register(ProfileMethodSecurity.class);
        context.registerBean("accessControlService", AccessControlService.class,
                () -> new AccessControlService(null, loginService, null, null, null, null, null));
        context.registerBean(UserController.class, () -> new UserController(loginService, null, null, null, null,
                null,                 null, null, null, null, null, null, null, null, null, null, queries, null, null, null));
        context.refresh();
        return context;
    }

    private MockMvc profileMvc(AnnotationConfigApplicationContext context) {
        return MockMvcBuilders.standaloneSetup(context.getBean(UserController.class))
                .addMappedInterceptors(new String[]{"/admin/users/manage/*"}, new CurrentUserPermissionsConfig(
                        new CurrentUserPermissionsService(authorizedClients, client), true))
                .build();
    }

    @Configuration
    @EnableMethodSecurity
    static class ProfileMethodSecurity {
    }

    private void assertHomePermissions(MockHttpSession httpSession, CurrentUserPermissions expected) throws Exception {
        mvc.perform(get("/home").session(httpSession)).andExpect(status().isOk())
                .andExpect(model().attribute(PERMISSIONS, expected));
    }

    private void assertHomeDenied(MockHttpSession httpSession) throws Exception {
        var result = mvc.perform(get("/home").session(httpSession)).andExpect(status().isOk())
                .andExpect(view().name("home")).andExpect(model().attributeExists(PERMISSIONS)).andReturn();
        assertNoViewPermissions((CurrentUserPermissions) result.getModelAndView().getModel().get(PERMISSIONS));
    }

    private void assertNoViewPermissions(CurrentUserPermissions permissions) {
        assertThat(permissions.allows(Permission.VIEW_EXTERNAL_USER)).isFalse();
        assertThat(permissions.allows(Permission.VIEW_INTERNAL_USER)).isFalse();
    }

    private void loginAgain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        HttpServletResponse response = new MockHttpServletResponse();
        new CustomAuthenticationSuccessHandler(loginService).onAuthenticationSuccess(request, response, authentication);
    }

    private OAuth2AuthenticationToken authenticate(String subject) {
        var authorities = AuthorityUtils.createAuthorityList("USER");
        var principal = new DefaultOAuth2User(authorities,
                Map.of("name", "same-name", "sub", subject, "oid", "user-oid"), "name");
        var auth = new OAuth2AuthenticationToken(principal, authorities, "azure");
        SecurityContextHolder.getContext().setAuthentication(auth);
        return auth;
    }

    private void stubToken(OAuth2AuthenticationToken auth) {
        OAuth2AuthorizedClient authorizedClient = mock(OAuth2AuthorizedClient.class);
        Instant now = Instant.now();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "login-token",
                now, now.plusSeconds(3600));
        when(authorizedClient.getAccessToken()).thenReturn(accessToken);
        when(authorizedClients.loadAuthorizedClient(eq("azure"), eq(auth), any())).thenReturn(authorizedClient);
    }

    @Controller
    static class ProtectedPage {
        private int renders;

        @GetMapping("/home")
        public String home() {
            renders++;
            return "home";
        }

        @GetMapping("/admin/users")
        public String users() {
            return "users";
        }
    }
}
