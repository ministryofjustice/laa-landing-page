package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CustomLogoutHandlerTest {

    @InjectMocks
    private CustomLogoutHandler logoutHandler;
    @Mock
    private OAuth2AuthorizedClientService clientService;
    @Mock
    private LoginService loginService;

    @Test
    public void getClient() {
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");
        logoutHandler.getClient(realAuthToken);
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
    }

    @Test
    public void logout() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2User realPrincipal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "Alice", "preferred_username", "alice@laa.gov.uk"),
                "name");
        OAuth2AuthenticationToken realAuthToken = new OAuth2AuthenticationToken(realPrincipal, realPrincipal.getAuthorities(), "azure");

        logoutHandler.logout(request, response, realAuthToken);
        verify(clientService).loadAuthorizedClient(eq("azure"), eq("Alice"));
        verify(loginService).logout(any(), any());
    }
}
