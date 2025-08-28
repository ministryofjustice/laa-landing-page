package uk.gov.justice.laa.portal.landingpage.config;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.service.AuthzOidcUserDetailsService;
import uk.gov.justice.laa.portal.landingpage.service.CustomLogoutHandler;

/**
 * Security configuration for the application
 * Configures security filter chains, authentication, and authorization
 */
@Slf4j
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
    public OncePerRequestFilter jwtRequestLoggingFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                    throws ServletException, IOException {

                if (request.getRequestURI().equals("/api/v1/claims/enrich")) {
                    log.info("Request URI: {}", request.getRequestURI());
                    log.info("Request Method: {}", request.getMethod());

                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null) {
                        if (authHeader.startsWith("Bearer ")) {
                            String token = authHeader.substring(7);
                            log.info("Authorization header present: Bearer [token length: {}]", token.length());
                            log.info("JWT Token (first 50 chars): {}...", token.substring(0, Math.min(50, token.length())));
                        } else {
                            log.warn("Authorization header present but not in Bearer format: {}", authHeader.substring(0, Math.min(20, authHeader.length())));
                        }
                    } else {
                        log.error("No Authorization header found in request");
                    }

                    log.info("Content-Type: {}", request.getContentType());
                    log.info("User-Agent: {}", request.getHeader("User-Agent"));
                }


                ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
                ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

                filterChain.doFilter(requestWrapper, responseWrapper);

                String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                log.info("claim enrichment entra request body: {}", requestBody);

                responseWrapper.copyBodyToResponse();

                if (request.getRequestURI().equals("/api/v1/claims/enrich")) {
                    log.info("Response Status to enrichment endpoint: {}", response.getStatus());
                }
            }
        };
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
        log.info("Configuring JWT Authentication Converter");

        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);

        log.info("JWT Authentication Converter configured successfully");
        return jwtAuthenticationConverter;
    }

    /**
     * Security filter chain for API endpoints
     * Configures stateless JWT authentication for claims enrichment endpoints
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        log.info("security filter chain for /api/v1/claims/enrich");
        http.securityMatcher("/api/v1/claims/enrich")
            .authorizeHttpRequests(authorize -> {
                log.info("Configuring authorization for API endpoints");
                authorize.anyRequest().authenticated();
            })
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .oauth2ResourceServer(oauth2 -> {
                log.info("Configuring OAuth2 resource server with JWT");
                oauth2.jwt(jwt -> {
                    log.info("Setting JWT authentication converter");
                    jwt.jwtAuthenticationConverter(jwtAuthenticationConverter());
                });
            })
            // Add request logging filter before authentication
            .addFilterBefore(jwtRequestLoggingFilter(), BasicAuthenticationFilter.class)
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
            .requestMatchers("/", "/login", "/css/**", "/js/**", "/assets/**"
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
}
