package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.justice.laa.portal.landingpage.service.AuthzOidcUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTest.DummyController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @Mock
    private AuthzOidcUserDetailsService authzOidcUserDetailsService;

    @Test
    void passwordEncoderBeanCreation() {
        assertThat(new SecurityConfig(authzOidcUserDetailsService).passwordEncoder())
                .isInstanceOf(PasswordEncoder.class);
    }

    @Test
    void protectedEndpointRedirectsToOauthLogin() throws Exception {
        mvc.perform(get("/secure"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/oauth2/**"));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/secure"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postWithCsrfTokenContinuesToAuthFlow() throws Exception {
        mvc.perform(post("/secure").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void logoutRedirectsToRoot() throws Exception {
        mvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    // Minimal controller to make sure that every URL in tests are able to resolve
    @RestController
    static class DummyController {

        @GetMapping({"/", "/login", "/css/{file:.+}", "/secure"})
        public String get() {
            return "ok";
        }

        @PostMapping("/secure")
        public String post() {
            return "ok";
        }
    }
}