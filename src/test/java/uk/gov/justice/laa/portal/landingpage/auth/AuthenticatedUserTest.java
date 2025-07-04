package uk.gov.justice.laa.portal.landingpage.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class AuthenticatedUserTest {

    @Test
    public void testGetCurrentUserReturnsCurrentUserWhenAuthenticated() {
        OAuth2User principal = mock(OAuth2User.class);
        final String name = "Test User";
        final String id = UUID.randomUUID().toString();
        when(principal.getAttribute("name")).thenReturn(name);
        when(principal.getAttribute("oid")).thenReturn(id);
        Authentication authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            AuthenticatedUser authenticatedUser = new AuthenticatedUser();
            Optional<CurrentUserDto> optionalCurrentUser = authenticatedUser.getCurrentUser();
            Assertions.assertTrue(optionalCurrentUser.isPresent());
            CurrentUserDto currentUserDto = optionalCurrentUser.get();
            Assertions.assertEquals(name, currentUserDto.getName());
            Assertions.assertEquals(id, currentUserDto.getUserId().toString());
        }
    }

    @Test
    public void testGetCurrentEntraUserReturnsEntraUserWhenAuthenticatedAndUserExists() {
        OAuth2User principal = mock(OAuth2User.class);
        final String name = "Test User";
        final String id = UUID.randomUUID().toString();
        when(principal.getAttribute("name")).thenReturn(name);
        when(principal.getAttribute("oid")).thenReturn(id);
        Authentication authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        UserService userService = mock(UserService.class);
        when(userService.getUserByEntraId(any())).thenReturn(EntraUser.builder().build());

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            AuthenticatedUser authenticatedUser = new AuthenticatedUser();
            EntraUser entraUser = authenticatedUser.getCurrentEntraUser(userService);
            Assertions.assertNotNull(entraUser);
        }
    }

    @Test
    public void testGetCurrentEntraUserThrowsAssertionErrorWhenAuthenticatedButUserDoesNotExist() {
        OAuth2User principal = mock(OAuth2User.class);
        final String name = "Test User";
        final String id = UUID.randomUUID().toString();
        when(principal.getAttribute("name")).thenReturn(name);
        when(principal.getAttribute("oid")).thenReturn(id);
        Authentication authentication = mock(OAuth2AuthenticationToken.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        UserService userService = mock(UserService.class);
        when(userService.getUserByEntraId(any())).thenReturn(null);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            AuthenticatedUser authenticatedUser = new AuthenticatedUser();
            assertThrows(AssertionError.class, () -> authenticatedUser.getCurrentEntraUser(userService));
        }
    }

    @Test
    public void testGetCurrentUserReturnsNothingWhenNotAuthenticated() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);

        try (MockedStatic<SecurityContextHolder> mockedStatic = mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            AuthenticatedUser authenticatedUser = new AuthenticatedUser();
            Optional<CurrentUserDto> optionalCurrentUser = authenticatedUser.getCurrentUser();
            Assertions.assertTrue(optionalCurrentUser.isEmpty());
        }
    }

}
