package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
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
        OidcIdToken idToken = userRequest.getIdToken();

        String userId = idToken.getClaimAsString("oid");
        if (userId == null) {
            throw new OAuth2AuthenticationException("Missing 'oid' claim in OIDC token.");
        }

        List<String> userAuthorities = userService.getUserAuthorities(userId);
        List<SimpleGrantedAuthority> grantedAuthorities = userAuthorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        if (nameAttributeKey == null) {
            nameAttributeKey = "sub";
        }

        return new DefaultOidcUser(grantedAuthorities, idToken, nameAttributeKey);
    }
}
