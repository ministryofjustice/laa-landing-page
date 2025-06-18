package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthzOidcUserDetailsService extends OidcUserService {

    private final UserService userService;

    public AuthzOidcUserDetailsService(UserService userService) {
        this.userService = userService;
    }


    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String userName = oidcUser.getPreferredUsername();

        List<String> userAuthorities = userService.getUserAuthorities(userName);
        List<SimpleGrantedAuthority> grantedAuthorities =
                userAuthorities.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

        return new DefaultOidcUser(grantedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());

    }
}
