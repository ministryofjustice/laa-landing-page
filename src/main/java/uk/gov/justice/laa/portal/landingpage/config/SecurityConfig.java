package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.service.AuthzOidcUserDetailsService;
import uk.gov.justice.laa.portal.landingpage.service.CustomLogoutHandler;

/**
 * Security configuration for the application
 * Configures security filter chains, authentication, and authorization
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final AuthzOidcUserDetailsService authzOidcUserDetailsService;
    private final CustomLogoutHandler logoutHandler;

    public SecurityConfig(AuthzOidcUserDetailsService authzOidcUserDetailsService, CustomLogoutHandler logoutHandler) {
        this.authzOidcUserDetailsService = authzOidcUserDetailsService;
        this.logoutHandler = logoutHandler;
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
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            // Disable form login and HTTP Basic to prevent redirects
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
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
            .requestMatchers("/admin/users/**", "/pda/**")
                .hasAnyAuthority(Permission.ADMIN_PERMISSIONS)
            .requestMatchers("/admin/user/**")
                .hasAnyAuthority(Permission.ADMIN_PERMISSIONS)
            .requestMatchers("/", "/login", "/migrate", "/register", "/css/**", "/js/**", "/assets/**"
            ).permitAll()
            .requestMatchers("/actuator/**")
                .access((auth, context) -> {
                    boolean allowed =
                            new IpAddressMatcher("127.0.0.1").matches(context.getRequest())
                            || new IpAddressMatcher("::1").matches(context.getRequest())
                            || new IpAddressMatcher("10.0.0.0/8").matches(context.getRequest())
                            || new IpAddressMatcher("172.16.0.0/12").matches(context.getRequest())
                            || new IpAddressMatcher("192.168.0.0/16").matches(context.getRequest());
                    return new AuthorizationDecision(allowed);
                })
            .anyRequest()
                .authenticated()
        ).oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .oidcUserService(authzOidcUserDetailsService)
            )
            .loginPage("/oauth2/authorization/azure")
            .defaultSuccessUrl("/home", true)
            .permitAll()
        ).logout(logout -> logout
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
            .addLogoutHandler(logoutHandler)
            .logoutSuccessUrl("/?message=logout")
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
