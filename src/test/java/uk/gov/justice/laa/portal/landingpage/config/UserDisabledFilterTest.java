package uk.gov.justice.laa.portal.landingpage.config;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

@ExtendWith(MockitoExtension.class)
class UserDisabledFilterTest {

    @Mock
    private EntraUserRepository userRepository;

    @Mock
    private LoginService loginService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    private UserDisabledFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserDisabledFilter(userRepository, loginService);
        SecurityContextHolder.setContext(securityContext);
    }

    private OAuth2AuthenticationToken buildOAuth2Token(String userId) {
        OAuth2User oauth2User = new DefaultOAuth2User(
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
            Collections.singletonMap("sub", userId),
            "sub"
        );
        return new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "azure");
    }

    @Nested
    class UnauthenticatedAccessTests {

        @Test
        void shouldAllowAccessWhenAuthIsNull() throws Exception {
            when(securityContext.getAuthentication()).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        void shouldAllowAccessWhenAuthIsNotOauth2() throws Exception {
            Authentication auth = org.mockito.Mockito.mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.isAuthenticated()).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        void shouldAllowAccessWhenAuthIsNotAuthenticated() throws Exception {
            Authentication notAuthenticated = org.mockito.Mockito.mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(notAuthenticated);
            when(notAuthenticated.isAuthenticated()).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Nested
    class DisabledUserTests {

        @Test
        void shouldBlockAccessForDisabledUser() throws Exception {
            UUID userId = UUID.randomUUID();
            OAuth2AuthenticationToken auth = buildOAuth2Token(userId.toString());
            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
            when(userRepository.existsByEntraOidAndEnabledFalse(userId.toString())).thenReturn(true);

            filter.doFilterInternal(request, response, filterChain);

            verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
            verify(filterChain, never()).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void shouldAllowAccessForEnabledUser() throws Exception {
            UUID userId = UUID.randomUUID();
            OAuth2AuthenticationToken auth = buildOAuth2Token(userId.toString());
            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);
            when(userRepository.existsByEntraOidAndEnabledFalse(userId.toString())).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldAllowAccessWhenCurrentUserDtoIsNull() throws Exception {
            OAuth2AuthenticationToken auth = buildOAuth2Token("some-user");
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loginService.getCurrentUser(auth)).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Nested
    class ShouldNotFilterTests {

        @Test
        void shouldNotFilterCssRequests() {
            when(request.getServletPath()).thenReturn("/css/style.css");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        void shouldNotFilterJsRequests() {
            when(request.getServletPath()).thenReturn("/js/app.js");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        void shouldNotFilterAssetsRequests() {
            when(request.getServletPath()).thenReturn("/assets/images/logo.png");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        void shouldNotFilterFaviconRequest() {
            when(request.getServletPath()).thenReturn("/favicon.ico");
            assertThat(filter.shouldNotFilter(request)).isTrue();
        }

        @Test
        void shouldFilterNormalRequests() {
            when(request.getServletPath()).thenReturn("/home");
            assertThat(filter.shouldNotFilter(request)).isFalse();
        }
    }
}
