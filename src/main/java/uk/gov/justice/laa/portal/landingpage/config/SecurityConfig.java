package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AuthzOidcUserDetailsService;

@Configuration
@Profile("!test")
@EnableWebSecurity
public class SecurityConfig {

    private final AuthzOidcUserDetailsService authzOidcUserDetailsService;

    public SecurityConfig(AuthzOidcUserDetailsService authzOidcUserDetailsService) {
        this.authzOidcUserDetailsService = authzOidcUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/admin/**", "/pda/**").hasAnyAuthority(UserType.ADMIN_TYPES)
                .requestMatchers("/", "/login", "/migrate", "/register", "/css/**", "/js/**", "/assets/**", "/actuator/**")
                .permitAll()
                .anyRequest().authenticated()
        ).oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(oauth2n -> oauth2n.oidcUserService(authzOidcUserDetailsService))
                .loginPage("/oauth2/authorization/azure")
                .defaultSuccessUrl("/home", true)
                .permitAll()
        ).logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/")
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID") // clear the session cookie
                .invalidateHttpSession(true)
                .permitAll()
        ).csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/v1/claims/enrich", "/api/v1/claims/enrich/entraid")
        );
        return http.build();
    }
}
