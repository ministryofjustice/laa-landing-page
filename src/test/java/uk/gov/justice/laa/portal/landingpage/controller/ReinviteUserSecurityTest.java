package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.justice.laa.portal.landingpage.auth.AuthenticatedUser;
import uk.gov.justice.laa.portal.landingpage.config.UiLabelsProperties;
import uk.gov.justice.laa.portal.landingpage.dto.AuditUserDetailDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.AuditExportService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserPollingService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;
import uk.gov.justice.laa.portal.landingpage.service.UserReactivationRequestService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.utils.StatusTagClassUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReinviteUserSecurityTest {
    private final UUID entraOid = UUID.randomUUID();
    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;
    private AccessControlService accessControlService;
    private UserService userService;
    private LoginService loginService;
    private EventService eventService;
    private TechServicesClient techServicesClient;

    @BeforeEach
    void setUp() {
        accessControlService = mock(AccessControlService.class);
        userService = mock(UserService.class);
        loginService = mock(LoginService.class);
        eventService = mock(EventService.class);
        techServicesClient = mock(TechServicesClient.class);

        TestSecurityConfiguration.accessControlServiceMock = accessControlService;
        TestSecurityConfiguration.userServiceMock = userService;
        TestSecurityConfiguration.loginServiceMock = loginService;
        TestSecurityConfiguration.eventServiceMock = eventService;
        TestSecurityConfiguration.techServicesClientMock = techServicesClient;
        context = new AnnotationConfigWebApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("reinvite-test", Map.of("feature.flag.disable.user", "false")));
        context.setServletContext(new MockServletContext());
        context.register(TestSecurityConfiguration.class);
        context.refresh();
        context.getBeanFactory().registerSingleton("accessControlService", accessControlService);
        WebApplicationContext webContext = context;
        mockMvc = MockMvcBuilders.webAppContextSetup(webContext).apply(springSecurity()).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void reinviteGetRoutesRequirePermissionOnEntraOidBeforeLoadingUser() throws Exception {
        when(accessControlService.canReinviteExternalUser(entraOid.toString())).thenReturn(false);
        String[] paths = {
            "/admin/users/audit/%s/reinvite-user",
            "/admin/users/audit/%s/reinvite-user/check-answers",
            "/admin/users/audit/%s/reinvite-user/success"
        };

        for (String path : paths) {
            mockMvc.perform(get(path.formatted(entraOid)).with(user("admin")))
                    .andExpect(status().isForbidden());
        }

        verify(accessControlService, times(paths.length)).canReinviteExternalUser(entraOid.toString());
        verifyNoInteractions(userService, loginService, eventService, techServicesClient);
    }

    @Test
    void confirmPostRequiresCsrfTokenBeforeInvokingController() throws Exception {
        mockMvc.perform(post("/admin/users/audit/{id}/reinvite-user/confirm", entraOid).with(user("admin")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accessControlService, userService, loginService, eventService, techServicesClient);
    }

    @Test
    void confirmPostRequiresReinvitePermissionBeforeLoadingUserOrCallingClient() throws Exception {
        when(accessControlService.canReinviteExternalUser(entraOid.toString())).thenReturn(false);

        mockMvc.perform(post("/admin/users/audit/{id}/reinvite-user/confirm", entraOid)
                        .with(user("admin")).with(csrf()))
                .andExpect(status().isForbidden());

        verify(accessControlService).canReinviteExternalUser(entraOid.toString());
        verifyNoInteractions(userService, loginService, eventService, techServicesClient);
    }

    @Test
    void confirmPostChecksPermissionUsingEntraOidAndOnlyThenCallsClient() throws Exception {
        UUID internalUserId = UUID.randomUUID();
        UUID initiatingUserOid = UUID.randomUUID();
        AuditUserDetailDto target = AuditUserDetailDto.builder()
                .userId(internalUserId.toString())
                .entraOid(entraOid.toString())
                .fullName("External User")
                .build();
        when(accessControlService.canReinviteExternalUser(entraOid.toString())).thenReturn(true);
        when(userService.findUserByUserEntraId(entraOid.toString()))
                .thenReturn(EntraUserDto.builder().id(internalUserId.toString()).build());
        when(userService.getAuditUserDetailByEntraId(internalUserId)).thenReturn(target);
        when(loginService.getCurrentEntraUser(any(Authentication.class)))
                .thenReturn(EntraUser.builder().entraOid(initiatingUserOid.toString()).build());
        when(techServicesClient.reinviteUser(entraOid)).thenReturn(TechServicesApiResponse.success(null));

        mockMvc.perform(post("/admin/users/audit/{id}/reinvite-user/confirm", entraOid)
                        .with(user("admin")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/audit/" + entraOid + "/reinvite-user/success"));

        verify(accessControlService).canReinviteExternalUser(entraOid.toString());
        verify(accessControlService, never()).canReinviteExternalUser(internalUserId.toString());
        verify(userService).findUserByUserEntraId(entraOid.toString());
        verify(techServicesClient).reinviteUser(entraOid);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfiguration {
        private static AccessControlService accessControlServiceMock;
        private static UserService userServiceMock;
        private static LoginService loginServiceMock;
        private static EventService eventServiceMock;
        private static TechServicesClient techServicesClientMock;

        @Bean
        AuditController auditController() {
            return new AuditController(userServiceMock, loginServiceMock, eventServiceMock, accessControlServiceMock,
                    mock(AuditExportService.class), mock(FirmService.class), mock(AuthenticatedUser.class),
                    techServicesClientMock, mock(UserAccountStatusService.class),
                    mock(ExternalUserPollingService.class), mock(EntraUserRepository.class),
                    mock(UserReactivationRequestService.class), mock(UiLabelsProperties.class),
                    new StatusTagClassUtil());
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .httpBasic(basic -> { })
                    .build();
        }
    }
}
