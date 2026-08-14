package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthzOidcUserDetailsService extends OidcUserService {

    private final UserService userService;

    private final OidcUserService delegate;

    @Value("${app.enable.user.data.api.call}")
    private boolean oboEnabled;

    public AuthzOidcUserDetailsService(UserService userService) {
        this.userService = userService;
        delegate = new OidcUserService();
    }


    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        if (oboEnabled) {
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

            OidcUserInfo userInfo = new OidcUserInfo(idToken.getClaims());
            return new DefaultOidcUser(grantedAuthorities, idToken, userInfo, nameAttributeKey);
        } else {
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
}
