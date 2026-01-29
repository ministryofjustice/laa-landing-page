package uk.gov.justice.laa.portal.landingpage.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
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
    private final Environment environment;

    public SecurityConfig(AuthzOidcUserDetailsService authzOidcUserDetailsService, CustomLogoutHandler logoutHandler, Environment environment) {
        this.authzOidcUserDetailsService = authzOidcUserDetailsService;
        this.logoutHandler = logoutHandler;
        this.environment = environment;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }

    /**
     * Custom OAuth2 Authorization Request Resolver that adds prompt=login parameter
     * to force users to always enter their email address during authentication
     */
    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        
        authorizationRequestResolver.setAuthorizationRequestCustomizer(customizer -> {
            Map<String, Object> additionalParameters = new HashMap<>();
            additionalParameters.put("prompt", "select_account");
            customizer.additionalParameters(additionalParameters);
        });
        
        return authorizationRequestResolver;
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
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http, 
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.authorizeHttpRequests((authorize) -> authorize
                .requestMatchers("/admin/users/**", "/pda/**")
                .hasAnyAuthority(Permission.ADMIN_PERMISSIONS)
                .requestMatchers("/admin/user/**")
                .hasAnyAuthority(Permission.ADMIN_PERMISSIONS)
                .requestMatchers("/admin/multi-firm/user/**")
                .hasAnyAuthority(Permission.DELEGATE_FIRM_ACCESS_PERMISSIONS)
                .requestMatchers("/", "/login", "/logout-success", "/cookies", "/css/**", "/js/**", "/assets/**"
                ).permitAll()
                .requestMatchers("/actuator/**", "/playwright/login")
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
                .authorizationEndpoint(authorization -> authorization
                        .authorizationRequestResolver(authorizationRequestResolver(clientRegistrationRepository))
                )
                .loginPage("/oauth2/authorization/azure")
                .defaultSuccessUrl("/home", true)
                .permitAll()
        ).logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .addLogoutHandler(logoutHandler)
                .logoutSuccessUrl("/logout-success")
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll()
        ).sessionManagement(session -> session
                .invalidSessionUrl("/?message=session-expired")
                .maximumSessions(1)
                .expiredUrl("/?message=session-expired")
        ).csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        ).headers(headers -> headers
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(63072000)
                        .requestMatcher(AnyRequestMatcher.INSTANCE)
                )
                .contentSecurityPolicy(contentSecurityPolicyConfig -> contentSecurityPolicyConfig
                        .policyDirectives("default-src * self blob: data: gap:; style-src * self 'unsafe-inline' blob: data: gap:;"
                                + " script-src * 'self' 'unsafe-eval' 'unsafe-inline' blob: data: gap:; object-src * 'self' blob: data: gap:;"
                                + " img-src * self 'unsafe-inline' blob: data: gap:; connect-src self * 'unsafe-inline' blob: data: gap:;"
                                + " frame-src * self blob: data: gap:;"))
        );
        return http.build();
    }

    @Bean
    // CHECKSTYLE.OFF: AbbreviationAsWordInName|MethodName
    public OAuth2AuthorizedClientService oAuth2AuthorizedClientService(JdbcOperations jdbcOperations,
                                                                       ClientRegistrationRepository clientRegistrationRepository) {
        // CHECKSTYLE.ON: AbbreviationAsWordInName|MethodName
        String enabled = environment.getProperty("SPRING_SESSION_JDBC_ENABLED", "false");
        OAuth2AuthorizedClientService service;
        if ("true".equalsIgnoreCase(enabled)) {
            service = new JdbcOAuth2AuthorizedClientService(jdbcOperations, clientRegistrationRepository);
        } else {
            service = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        }
        logoutHandler.setOAuth2AuthorizedClientService(service);
        return service;
    }

    @Bean
    public PostgreSqlJdbcHttpSessionCustomizer jdbcHttpSessionCustomizer() {
        return new PostgreSqlJdbcHttpSessionCustomizer();
    }

    @Bean
    public HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    public class PostgreSqlJdbcHttpSessionCustomizer
            implements SessionRepositoryCustomizer<JdbcIndexedSessionRepository> {

        private static final String CREATE_SESSION_ATTRIBUTE_QUERY = """
            INSERT INTO %TABLE_NAME%_ATTRIBUTES (SESSION_PRIMARY_ID, ATTRIBUTE_NAME, ATTRIBUTE_BYTES)
            VALUES (?, ?, ?)
            ON CONFLICT (SESSION_PRIMARY_ID, ATTRIBUTE_NAME)
            DO UPDATE SET ATTRIBUTE_BYTES = EXCLUDED.ATTRIBUTE_BYTES
            """;

        @Override
        public void customize(JdbcIndexedSessionRepository sessionRepository) {
            sessionRepository.setCreateSessionAttributeQuery(CREATE_SESSION_ATTRIBUTE_QUERY);
        }

    }
}
