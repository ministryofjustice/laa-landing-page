package uk.gov.justice.laa.portal.landingpage.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomAuthenticationSuccessHandler")
class CustomAuthenticationSuccessHandlerTest {

    @Mock
    private LoginService loginService;

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    UserProfileRepository userProfileRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oauth2User;

    private CustomAuthenticationSuccessHandler handler;
    private UUID testUserId;
    private String testOid;

    @BeforeEach
    void setUp() {
        handler = new CustomAuthenticationSuccessHandler(loginService, entraUserRepository, userProfileRepository);
        testUserId = UUID.randomUUID();
        testOid = UUID.randomUUID().toString();
    }

    @Test
    @DisplayName("Should record successful login for enabled user")
    void shouldRecordSuccessfulLoginForEnabledUser() throws IOException {
        // Arrange
        EntraUser enabledUser = EntraUser.builder().id(testUserId).entraOid(testOid).enabled(true).build();

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.of(enabledUser));

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, times(1)).recordSuccessfulLogin(authentication);
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should NOT record login for disabled user")
    void shouldNotRecordLoginForDisabledUser() throws IOException {
        // Arrange
        EntraUser disabledUser = EntraUser.builder().id(testUserId).entraOid(testOid).enabled(false).build();

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.of(disabledUser));

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, never()).recordSuccessfulLogin(any());
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should NOT record login when EntraUser not found")
    void shouldNotRecordLoginWhenEntraUserNotFound() throws IOException {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.empty());

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, never()).recordSuccessfulLogin(any());
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should NOT record login when OID is null")
    void shouldNotRecordLoginWhenOidIsNull() throws IOException {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(null);

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, never()).recordSuccessfulLogin(any());
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should NOT record login when OAuth2User is not principal")
    void shouldNotRecordLoginWhenNotOauth2User() throws IOException {
        // Arrange
        when(authentication.getPrincipal()).thenReturn("not-an-oauth2-user");

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, never()).recordSuccessfulLogin(any());
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should redirect to /home even when eligibility check fails")
    void shouldRedirectToHomeEvenWhenEligibilityCheckFails() throws IOException {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.empty());

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should handle exception during eligibility check gracefully")
    void shouldHandleExceptionDuringEligibilityCheckGracefully() throws IOException {
        // Arrange
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, never()).recordSuccessfulLogin(any());
        verify(response, times(1)).sendRedirect("/home");
    }

    // ========== FIRM ELIGIBILITY TESTS ==========

    @Test
    @DisplayName("Should record login for EXTERNAL user with enabled firm")
    void shouldRecordLoginForExternalUserWithEnabledFirm() throws IOException {
        // Arrange
        EntraUser enabledUser = EntraUser.builder().id(testUserId).entraOid(testOid).enabled(true).build();

        Firm enabledFirm = Firm.builder().enabled(true).build();

        UserProfile externalProfile = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(enabledFirm)
                .build();

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.of(enabledUser));
        when(userProfileRepository.findAllByEntraUser(enabledUser)).thenReturn(List.of(externalProfile));

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, times(1)).recordSuccessfulLogin(authentication);
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should NOT record login for EXTERNAL user with all disabled firms")
    void shouldNotRecordLoginForExternalUserWithAllDisabledFirms() throws IOException {
        // Arrange
        EntraUser enabledUser = EntraUser.builder().id(testUserId).entraOid(testOid).enabled(true).build();

        Firm disabledFirm = Firm.builder().enabled(false).build();

        UserProfile externalProfile = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(disabledFirm)
                .build();

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.of(enabledUser));
        when(userProfileRepository.findAllByEntraUser(enabledUser)).thenReturn(List.of(externalProfile));

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, never()).recordSuccessfulLogin(any());
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should record login for EXTERNAL user with at least one enabled firm")
    void shouldRecordLoginForExternalUserWithAtLeastOneEnabledFirm() throws IOException {
        // Arrange
        EntraUser enabledUser = EntraUser.builder().id(testUserId).entraOid(testOid).enabled(true).build();

        Firm disabledFirm = Firm.builder().enabled(false).build();
        Firm enabledFirm = Firm.builder().enabled(true).build();

        UserProfile disabledFirmProfile = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(disabledFirm)
                .build();

        UserProfile enabledFirmProfile = UserProfile.builder()
                .userType(UserType.EXTERNAL)
                .firm(enabledFirm)
                .build();

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.of(enabledUser));
        when(userProfileRepository.findAllByEntraUser(enabledUser))
            .thenReturn(List.of(disabledFirmProfile, enabledFirmProfile));

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, times(1)).recordSuccessfulLogin(authentication);
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should record login for INTERNAL user (no firm check)")
    void shouldRecordLoginForInternalUserNoFirmCheck() throws IOException {
        // Arrange
        EntraUser enabledUser = EntraUser.builder().id(testUserId).entraOid(testOid).enabled(true).build();

        UserProfile internalProfile = UserProfile.builder()
                .userType(UserType.INTERNAL)
                .firm(null)
                .build();

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.of(enabledUser));
        when(userProfileRepository.findAllByEntraUser(enabledUser)).thenReturn(List.of(internalProfile));

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, times(1)).recordSuccessfulLogin(authentication);
        verify(response, times(1)).sendRedirect("/home");
    }

    @Test
    @DisplayName("Should record login for user with no profiles")
    void shouldRecordLoginForUserWithNoProfiles() throws IOException {
        // Arrange
        EntraUser enabledUser = EntraUser.builder().id(testUserId).entraOid(testOid).enabled(true).build();

        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttribute("oid")).thenReturn(testOid);
        when(entraUserRepository.findByEntraOid(testOid)).thenReturn(Optional.of(enabledUser));
        when(userProfileRepository.findAllByEntraUser(enabledUser)).thenReturn(List.of());

        // Act
        handler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(loginService, times(1)).recordSuccessfulLogin(authentication);
        verify(response, times(1)).sendRedirect("/home");
    }
}
