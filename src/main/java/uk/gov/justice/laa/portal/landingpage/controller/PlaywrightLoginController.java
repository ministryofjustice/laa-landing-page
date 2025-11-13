package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Profile("playwright")
@ConditionalOnProperty(name = "playwright.auth.endpoint.enabled", havingValue = "true")
public class PlaywrightLoginController {

    private final UserService userService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final HttpSessionOAuth2AuthorizedClientRepository authorizedClientRepository;

    @GetMapping("/playwright/login")
    public RedirectView playwrightLogin(String email, HttpServletRequest request, HttpServletResponse response) {
        authenticateTestUser(email, request, response);
        return new RedirectView("/home");
    }

    public void authenticateTestUser(String email, HttpServletRequest request, HttpServletResponse response) {

        EntraUserDto user = userService.getEntraUserByEmail(email).orElseThrow();


        String[] userPermissions = Permission.ADMIN_PERMISSIONS;

        Set<SimpleGrantedAuthority> authorities = Arrays.stream(userPermissions)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        OidcIdToken idToken = new OidcIdToken(
                "dummy-token", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("sub", user.getFirstName(), "name", user.getFirstName(), "oid", user.getEntraOid())
        );
        OidcUser oidcUser = new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                idToken
        );


        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                oidcUser,
                authorities,
                "azure"
        );

        // Persist to SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        OAuth2AuthorizedClient authorizedClient = new OAuth2AuthorizedClient(
                clientRegistrationRepository.findByRegistrationId("azure"),
                oidcUser.getName(),
                new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "dummy-token",
                        Instant.now(), Instant.now().plusSeconds(3600))
        );

        authorizedClientRepository.saveAuthorizedClient(
                authorizedClient, authentication, request, response);
    }

}
