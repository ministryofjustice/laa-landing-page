package uk.gov.justice.laa.portal.landingpage.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        List<SimpleGrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
        OidcUserInfo userInfo = new OidcUserInfo(Map.of("preferred_username", username));
        OidcIdToken oidcIdToken = new OidcIdToken("test", Instant.now(), Instant.now().plusSeconds(300), Map.of("sub", "test"));
        OidcUser oidcUser = new DefaultOidcUser(authorities, oidcIdToken, userInfo);

        BiFunction<OidcUserRequest, OidcUserInfo, OidcUser> userMapper = (OidcUserRequest ouRequest, OidcUserInfo ouInfo) -> oidcUser;
        ReflectionTestUtils.setField(authzOidcUserDetailsService, "oidcUserMapper", userMapper);

        OidcUserRequest oidcUserRequest = Mockito.mock(OidcUserRequest.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(userService.getUserAuthorities(username)).thenReturn(roles);
        // Act
        OidcUser result = authzOidcUserDetailsService.loadUser(oidcUserRequest);
        // Assert
        assertNotNull(result);
        Assertions.assertThat(result.getAuthorities()).isNotNull();
        Assertions.assertThat(result.getAuthorities()).isNotEmpty();
        Assertions.assertThat(result.getAuthorities()).hasSize(2);
        List<String> resultAuthorities = result.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        Assertions.assertThat(resultAuthorities).hasSameElementsAs(roles);

    }
}