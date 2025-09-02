package uk.gov.justice.laa.portal.landingpage.config;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import uk.gov.justice.laa.portal.landingpage.config.jwt.DevJwtDecoderConfig;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AuthzOidcUserDetailsService;
import uk.gov.justice.laa.portal.landingpage.service.CustomLogoutHandler;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@WebMvcTest(controllers = TestController.class)
@Import({SecurityConfig.class, DevJwtDecoderConfig.class, SecurityConfigTest.OauthClientTestConfig.class})
@ActiveProfiles("test")
class SecurityConfigTest {

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        // Without admin role - should be redirected
        mockMvc.perform(get("/admin/dashboard"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection());

        // With create external user permission - should be allowed to create users
        mockMvc.perform(get("/admin/user/create")
                        .with(jwt().authorities(Arrays.stream(new String[]{Permission.CREATE_EXTERNAL_USER.name()})
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("create-user"));

        // With external admin roles - manage users allowed
        mockMvc.perform(get("/admin/users/manage")
                        .with(jwt().authorities(Arrays.stream(new String[]{Permission.EDIT_EXTERNAL_USER.name()})
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("manage-user"));

        // With internal roles - manage users allowed
        mockMvc.perform(get("/admin/users/manage")
                        .with(jwt().authorities(Arrays.stream(new String[]{Permission.EDIT_INTERNAL_USER.name()})
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("manage-user"));

        // With admin role - should be allowed
        mockMvc.perform(get("/admin/dashboard")
                        .with(jwt().authorities(Arrays.stream(Permission.ADMIN_PERMISSIONS)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));
    }

    @Autowired
    private WebApplicationContext context;
    
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthzOidcUserDetailsService authzOidcUserDetailsService;
    @MockitoBean
    private CustomLogoutHandler logoutHandler;

    @Test
    void passwordEncoderBeanCreation() {
        SecurityConfig securityConfig = new SecurityConfig(authzOidcUserDetailsService, logoutHandler);
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void jwtAuthenticationConverterBeanCreation() {
        SecurityConfig securityConfig = new SecurityConfig(authzOidcUserDetailsService, logoutHandler);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        assertThat(converter).isNotNull();
    }

    @Test
    void publicEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        // Test public endpoints
        mockMvc.perform(get("/"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("public"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));

        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void securedEndpointsRedirectToLogin() throws Exception {
        mockMvc.perform(get("/secure"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection());
    }

    @TestConfiguration
    static class OauthClientTestConfig {
        @Bean
        public ClientRegistrationRepository clientRegistrationRepository() {
            return new CustomRepository();
        }

        public static class CustomRepository implements ClientRegistrationRepository {
            public CustomRepository() {
                // Empty repository for testing
            }

            @Override
            public ClientRegistration findByRegistrationId(String registrationId) {
                return null;
            }
        }
    }
    
    @Test
    @WithMockUser
    void postRequestsRequireCsrfToken() throws Exception {
        // Create a separate MockMvc instance specifically for CSRF testing
        MockMvc csrfMockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        
        csrfMockMvc.perform(post("/secure")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?message=session-expired"));
        
        csrfMockMvc.perform(post("/secure")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("secure-post"));
    }

    @Test
    void claimsEnrichEndpointBypassesCsrfProtection() throws Exception {
        mockMvc.perform(post("/api/v1/claims/enrich")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("claims-enrich"));
    }
    
    @Test
    void claimsEnrichEndpointRequiresJwtAuthentication() throws Exception {
        // Without JWT - should be unauthorized (401)
        mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRedirectsToLogoutSuccessPagePost() throws Exception {
        mockMvc.perform(post("/logout").with(jwt()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/logout-success"));
    }

    @Test
    void logoutRedirectsToLogoutSuccessPageGet() throws Exception {
        mockMvc.perform(get("/logout").with(jwt()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/logout-success"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "192.168.0.2", "10.0.0.1", "172.16.0.1", "::1", "192.168.0.1"})
    void publicEndpointsAreAccessibleWhiteList(String input) throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .with(request -> {
                            request.setRemoteAddr(input);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }

    @Test
    void publicEndpointsAreAccessibleRemote() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .with(request -> {
                            request.setRemoteAddr("1.1.1.1");
                            return request;
                        }))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void securityHeaders() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/")).andReturn();
        String sts = mvcResult.getResponse().getHeader("Strict-Transport-Security");
        String csp = mvcResult.getResponse().getHeader("Content-Security-Policy");
        assertThat(sts).isEqualTo("max-age=63072000 ; includeSubDomains ; preload");
        assertThat(csp).isEqualTo("default-src * self blob: data: gap:; style-src * self 'unsafe-inline' blob: data: gap:;"
                + " script-src * 'self' 'unsafe-eval' 'unsafe-inline' blob: data: gap:; object-src * 'self' blob: data: gap:;"
                + " img-src * self 'unsafe-inline' blob: data: gap:; connect-src self * 'unsafe-inline' blob: data: gap:;"
                + " frame-src * self blob: data: gap:;");
    }

    @Test
    void whenSessionExpires_thenRedirectsToHomeWithSessionExpiredMessage() throws Exception {

        // Create a session
        MvcResult result = mockMvc.perform(get("/secure").with(oauth2Login()))
                .andExpect(status().isOk())
                .andReturn();

        // Simulate session expiration by removing the session attribute
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        session.invalidate();

        // Try to access a secured page with expired session
        mockMvc.perform(get("/secure").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/oauth2/authorization/azure"));
    }
}