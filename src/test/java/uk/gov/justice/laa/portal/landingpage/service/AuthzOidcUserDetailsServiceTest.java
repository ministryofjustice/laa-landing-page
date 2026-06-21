package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthzOidcUserDetailsServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthzOidcUserDetailsService authzOidcUserDetailsService;

    @Test
    void loadUser() {
        // Arrange
        String username = "username";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");

        // Construct standard token properties matching what OidcUserService expects
        OidcUserInfo userInfo = new OidcUserInfo(Map.of("preferred_username", username, "oid", username));
        OidcIdToken oidcIdToken = new OidcIdToken(
                "test-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("sub", "test-sub", "oid", username)
        );

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("azure")
                .clientId("test-client")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost")
                .authorizationUri("http://localhost/authorize")
                .tokenUri("http://localhost/token")
                .userNameAttributeName("sub")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token-value-abc",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Collections.emptySet()
        );

        OidcUserRequest oidcUserRequest = new OidcUserRequest(clientRegistration, accessToken, oidcIdToken);

        // Instantiate normally (No Spy needed!)
        AuthzOidcUserDetailsService serviceUnderTest = new AuthzOidcUserDetailsService(userService);

        // Stub your internal database call
        when(userService.getUserAuthorities(username)).thenReturn(roles);

        // Act
        OidcUser result = serviceUnderTest.loadUser(oidcUserRequest);

        // Assert
        assertNotNull(result);
        assertThat(result.getAuthorities()).isNotNull().hasSize(2);

        List<String> resultAuthorities = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        assertThat(resultAuthorities).containsExactlyInAnyOrderElementsOf(roles);
    }
}
