package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.context.annotation.Lazy;
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

    private final OidcUserService delegate;

    public AuthzOidcUserDetailsService(UserService userService) {
        this.userService = userService;
        delegate = new OidcUserService();
    }


    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Call the delegate instead of super
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String userId = oidcUser.getAttribute("oid");
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

        return new DefaultOidcUser(grantedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo(), nameAttributeKey);
    }
}
