package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AuthzOidcUserDetailsService;

/**
 * Security configuration for the application
 * Configures security filter chains, authentication, and authorization
 */
@Configuration
@Profile("!integration-test")
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

    /**
     * JWT Authentication Converter for extracting authorities from JWT tokens
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    /**
     * Security filter chain for API endpoints
     * Configures stateless JWT authentication for claims enrichment endpoints
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/v1/claims/enrich")
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            // Disable form login and HTTP Basic to prevent redirects
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
        return http.build();
    }

    /**
     * Main security filter chain for web application
     * Configures OAuth2 login, CSRF protection, and role-based access control
     * Restrict /actuator/** endpoints to private IPv4 CIDR ranges (no public access)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/admin/**", "/pda/**").hasAnyAuthority(UserType.ADMIN_TYPES)
                .requestMatchers("/", "/login", "/migrate", "/register", "/css/**", "/js/**", "/assets/**", "/actuator/**")
                .permitAll()
                .requestMatchers("/actuator/**")
                .access((auth, context) -> {
                    boolean allowed =
                            new IpAddressMatcher("127.0.0.1").matches(request) ||
                            new IpAddressMatcher("::1").matches(request) ||
                            new IpAddressMatcher("10.0.0.0/8").matches(context.getRequest()) ||
                            new IpAddressMatcher("172.16.0.0/12").matches(context.getRequest()) ||
                            new IpAddressMatcher("192.168.0.0/16").matches(context.getRequest());
                    return new AuthorizationDecision(allowed);
                })
                .anyRequest().authenticated()
        ).oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                        .oidcUserService(authzOidcUserDetailsService)
                )
                .loginPage("/oauth2/authorization/azure")
                .defaultSuccessUrl("/home", true)
                .permitAll()
        ).logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/")
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
        ).csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        );
        return http.build();
    }
}
