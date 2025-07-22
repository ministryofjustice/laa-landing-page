package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.justice.laa.portal.landingpage.config.jwt.DevJwtDecoderConfig;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AuthzOidcUserDetailsService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

        // With admin role - should be allowed
        mockMvc.perform(get("/admin/dashboard")
                        .with(jwt().authorities(Arrays.stream(UserType.ADMIN_TYPES)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("admin"));

        // With external roles - users create should be blocked
        mockMvc.perform(get("/admin/user/create")
                        .with(jwt().authorities(UserType.EXTERNAL_TYPES.stream()
                                .map(UserType::name)
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());

        // With internal role - should be allowed to create users
        mockMvc.perform(get("/admin/user/create")
                        .with(jwt().authorities(Arrays.stream(new String[]{UserType.INTERNAL.name()})
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("create-user"));

        // With external admin roles - manage users allowed
        mockMvc.perform(get("/admin/users/manage")
                        .with(jwt().authorities(Arrays.stream(new String[]{UserType.EXTERNAL_SINGLE_FIRM_ADMIN.name()})
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("manage-user"));

        // With internal roles - manage users allowed
        mockMvc.perform(get("/admin/users/manage")
                        .with(jwt().authorities(Arrays.stream(new String[]{UserType.INTERNAL.name()})
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("manage-user"));

        // With external std roles - do not allow manage users
        mockMvc.perform(get("/admin/users/manage")
                        .with(jwt().authorities(Arrays.stream(new String[]{UserType.EXTERNAL_SINGLE_FIRM.name()})
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList()))))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isForbidden());

        // With admin role - should be allowed
        mockMvc.perform(get("/admin/dashboard")
                        .with(jwt().authorities(Arrays.stream(UserType.ADMIN_TYPES)
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
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().string("public"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));

        mockMvc.perform(get("/migrate"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));

        mockMvc.perform(get("/register"))
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
                .andExpect(status().isForbidden());
        
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
}