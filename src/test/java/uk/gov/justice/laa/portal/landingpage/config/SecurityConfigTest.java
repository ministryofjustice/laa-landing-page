package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.landingpage.config.jwt.DevJwtDecoderConfig;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AuthzOidcUserDetailsService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
@Import({SecurityConfig.class, DevJwtDecoderConfig.class, SecurityConfigTest.OAuth2ClientTestConfig.class})
@ActiveProfiles("test")
class SecurityConfigTest {

    @TestConfiguration
    static class OAuth2ClientTestConfig {
        @Bean
        public ClientRegistrationRepository clientRegistrationRepository() {
            return new InMemoryClientRegistrationRepository(Collections.emptyList());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthzOidcUserDetailsService authzOidcUserDetailsService;

    @Test
    void passwordEncoderBeanCreation() {
        SecurityConfig securityConfig = new SecurityConfig(authzOidcUserDetailsService);
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void jwtAuthenticationConverterBeanCreation() {
        SecurityConfig securityConfig = new SecurityConfig(authzOidcUserDetailsService);
        JwtAuthenticationConverter converter = securityConfig.jwtAuthenticationConverter();

        assertThat(converter).isNotNull();
    }

    @Test
    void publicEndpointsAreAccessibleWithoutAuthentication() throws Exception {
        // Test public endpoints
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/migrate"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void securedEndpointsRedirectToLogin() throws Exception {
        mockMvc.perform(get("/secure"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/authorization/azure**"));
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        // Without admin role
        mockMvc.perform(get("/admin/dashboard").with(jwt()))
                .andExpect(status().isForbidden());

        // With admin role
        mockMvc.perform(get("/admin/dashboard")
                        .with(jwt().authorities(Arrays.stream(UserType.ADMIN_TYPES)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andExpect(status().isOk());
    }

    @Test
    void postRequestsRequireCsrfToken() throws Exception {
        // Without CSRF token
        mockMvc.perform(post("/secure").with(jwt()))
                .andExpect(status().isForbidden());

        // With CSRF token
        mockMvc.perform(post("/secure").with(jwt()).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void claimsEnrichEndpointBypassesCsrfProtection() throws Exception {
        // Claims enrich endpoint should allow requests without CSRF
        mockMvc.perform(post("/api/v1/claims/enrich")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        // Claims enrich entraid endpoint should also bypass CSRF
        mockMvc.perform(post("/api/v1/claims/enrich/entraid")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRedirectsToHomePage() throws Exception {
        mockMvc.perform(post("/logout").with(jwt()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @RestController
    static class TestController {
        @GetMapping({"/", "/login", "/migrate", "/register", "/css/style.css", "/actuator/health"})
        public String publicEndpoint() {
            return "public";
        }

        @GetMapping("/secure")
        public String secureEndpoint() {
            return "secure";
        }

        @PostMapping("/secure")
        public String securePostEndpoint() {
            return "secure-post";
        }

        @GetMapping("/admin/dashboard")
        public String adminEndpoint() {
            return "admin";
        }

        @PostMapping({"/api/v1/claims/enrich", "/api/v1/claims/enrich/entraid"})
        public String claimsEnrichEndpoint() {
            return "claims-enrich";
        }
    }
}