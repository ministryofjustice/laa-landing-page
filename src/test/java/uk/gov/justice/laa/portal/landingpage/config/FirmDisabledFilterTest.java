package uk.gov.justice.laa.portal.landingpage.config;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

/**
 * Tests for FirmDisabledFilter - verifying access control for users with disabled firms.
 */
@ExtendWith(MockitoExtension.class)
class FirmDisabledFilterTest {

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

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

    private FirmDisabledFilter filter;

    @BeforeEach
    void setUp() {
        filter = new FirmDisabledFilter(entraUserRepository, userProfileRepository, loginService);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    class UnauthenticatedAccessTests {

        @Test
        void shouldAllowAccessWhenNotAuthenticated() throws Exception {
            // Given
            when(securityContext.getAuthentication()).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        void shouldAllowAccessWhenAuthenticationNotOAuth2() throws Exception {
            // Given
            Authentication auth = org.mockito.Mockito.mock(Authentication.class);
            when(securityContext.getAuthentication()).thenReturn(auth);
            when(auth.isAuthenticated()).thenReturn(true);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Nested
    class ExternalUserWithDisabledFirmTests {

        @Test
        void shouldBlockAccessForExternalUserWithDisabledFirm() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", userId.toString()),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

            EntraUser entraUser = EntraUser.builder()
                .entraOid(userId.toString())
                .build();
            when(entraUserRepository.findByEntraOid(userId.toString()))
                .thenReturn(Optional.of(entraUser));

            Firm disabledFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("FIRM123")
                .name("Disabled Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(false) // Disabled
                .build();

            UserProfile externalProfile = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(disabledFirm)
                .entraUser(entraUser)
                .build();

            when(userProfileRepository.findAllByEntraUser(entraUser))
                .thenReturn(List.of(externalProfile));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(response).sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Access denied - your firm is temporarily disabled due to contract status"
            );
            verify(filterChain, never()).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        void shouldAllowAccessForExternalUserWithEnabledFirm() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", userId.toString()),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

            EntraUser entraUser = EntraUser.builder()
                .entraOid(userId.toString())
                .build();
            when(entraUserRepository.findByEntraOid(userId.toString()))
                .thenReturn(Optional.of(entraUser));

            Firm enabledFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("FIRM123")
                .name("Enabled Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true) // Enabled
                .build();

            UserProfile externalProfile = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(enabledFirm)
                .entraUser(entraUser)
                .build();

            when(userProfileRepository.findAllByEntraUser(entraUser))
                .thenReturn(List.of(externalProfile));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Nested
    class InternalUserTests {

        @Test
        void shouldAllowAccessForInternalUserEvenWithDisabledFirm() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", userId.toString()),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

            EntraUser entraUser = EntraUser.builder()
                .entraOid(userId.toString())
                .build();
            when(entraUserRepository.findByEntraOid(userId.toString()))
                .thenReturn(Optional.of(entraUser));

            Firm disabledFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("FIRM123")
                .name("Disabled Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(false)
                .build();

            UserProfile internalProfile = UserProfile.builder()
                .userType(UserType.INTERNAL) // Internal user
                .firm(disabledFirm)
                .entraUser(entraUser)
                .build();

            when(userProfileRepository.findAllByEntraUser(entraUser))
                .thenReturn(List.of(internalProfile));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then - internal users are not affected by disabled firms
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        void shouldAllowAccessForInternalUserWithoutFirm() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", userId.toString()),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

            EntraUser entraUser = EntraUser.builder()
                .entraOid(userId.toString())
                .build();
            when(entraUserRepository.findByEntraOid(userId.toString()))
                .thenReturn(Optional.of(entraUser));

            UserProfile internalProfile = UserProfile.builder()
                .userType(UserType.INTERNAL)
                .firm(null) // No firm
                .entraUser(entraUser)
                .build();

            when(userProfileRepository.findAllByEntraUser(entraUser))
                .thenReturn(List.of(internalProfile));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Nested
    class ExternalUserWithoutFirmTests {

        @Test
        void shouldAllowAccessForExternalUserWithoutFirm() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", userId.toString()),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

            EntraUser entraUser = EntraUser.builder()
                .entraOid(userId.toString())
                .build();
            when(entraUserRepository.findByEntraOid(userId.toString()))
                .thenReturn(Optional.of(entraUser));

            UserProfile externalProfile = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(null) // No firm
                .entraUser(entraUser)
                .build();

            when(userProfileRepository.findAllByEntraUser(entraUser))
                .thenReturn(List.of(externalProfile));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    @Nested
    class MultipleProfilesTests {

        @Test
        void shouldBlockAccessIfAnyExternalProfileHasDisabledFirm() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", userId.toString()),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

            EntraUser entraUser = EntraUser.builder()
                .entraOid(userId.toString())
                .build();
            when(entraUserRepository.findByEntraOid(userId.toString()))
                .thenReturn(Optional.of(entraUser));

            Firm enabledFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("ENABLED")
                .name("Enabled Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .build();

            Firm disabledFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("DISABLED")
                .name("Disabled Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(false)
                .build();

            UserProfile profile1 = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(enabledFirm)
                .entraUser(entraUser)
                .build();

            UserProfile profile2 = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(disabledFirm) // Has disabled firm
                .entraUser(entraUser)
                .build();

            when(userProfileRepository.findAllByEntraUser(entraUser))
                .thenReturn(List.of(profile1, profile2));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then - should block if ANY external profile has disabled firm
            verify(response).sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Access denied - your firm is temporarily disabled due to contract status"
            );
            verify(filterChain, never()).doFilter(request, response);
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldAllowAccessWhenCurrentUserDtoIsNull() throws Exception {
            // Given
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", "user-id"),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);
            when(loginService.getCurrentUser(auth)).thenReturn(null); // Returns null

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        void shouldAllowAccessWhenEntraUserNotFound() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", userId.toString()),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

            when(entraUserRepository.findByEntraOid(userId.toString()))
                .thenReturn(Optional.empty()); // User not found

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        @Test
        void shouldAllowAccessWhenNoProfiles() throws Exception {
            // Given
            UUID userId = UUID.randomUUID();
            OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                Collections.singletonMap("sub", userId.toString()),
                "sub"
            );
            OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "azure"
            );

            when(securityContext.getAuthentication()).thenReturn(auth);

            CurrentUserDto currentUser = new CurrentUserDto();
            currentUser.setUserId(userId);
            when(loginService.getCurrentUser(auth)).thenReturn(currentUser);

            EntraUser entraUser = EntraUser.builder()
                .entraOid(userId.toString())
                .build();
            when(entraUserRepository.findByEntraOid(userId.toString()))
                .thenReturn(Optional.of(entraUser));

            when(userProfileRepository.findAllByEntraUser(entraUser))
                .thenReturn(Collections.emptyList()); // No profiles

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain).doFilter(request, response);
            verify(response, never()).sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
