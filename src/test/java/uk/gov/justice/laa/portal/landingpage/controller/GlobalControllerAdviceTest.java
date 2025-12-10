package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.view.RedirectView;

import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

@ExtendWith(MockitoExtension.class)
class GlobalControllerAdviceTest {

    @Mock
    private LoginService loginService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private GlobalControllerAdvice controller;

    @Test
    void getActiveFirm_notLoggedIn() {
        when(loginService.getCurrentEntraUser(any())).thenReturn(null);
        assertThat(controller.getActiveFirm(authentication, null)).isNull();
    }

    @Test
    void skipControllerAdvice_forClaimEnrichment() {
        when(loginService.getCurrentEntraUser(any())).thenReturn(null);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/claims");
        assertThat(controller.getActiveFirm(authentication, null)).isNull();
    }

    @Test
    void getActiveFirm_noProfileSet() {
        EntraUser entraUser = EntraUser.builder().build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication, null);
        assertThat(firmDto.getName())
                .isEqualTo("You currently don’t have access to any profiles. Please contact the admin to be added.");
        assertThat(firmDto.isCanChange()).isFalse();
    }

    @Test
    void getActiveFirm_single_firm_external() {
        Firm firm = Firm.builder().name("Firm").code("Code").build();
        UserProfile userProfile = UserProfile.builder().userType(UserType.EXTERNAL).firm(firm).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(false).userProfiles(Set.of(userProfile)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication, null);
        assertThat(firmDto.getName()).isEqualTo("Firm");
        assertThat(firmDto.isCanChange()).isFalse();
    }

    @Test
    void getActiveFirm_single_firm_internal() {
        UserProfile userProfile = UserProfile.builder().userType(UserType.INTERNAL).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(false).userProfiles(Set.of(userProfile)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication, null);
        assertThat(firmDto).isNull();
    }

    @Test
    void getActiveFirm_multi_firm_not_active_profile() {
        Firm firm = Firm.builder().name("Firm").code("Code").build();
        UserProfile userProfile = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(false).firm(firm)
                .build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(true).userProfiles(Set.of(userProfile)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication, null);
        assertThat(firmDto.getName()).isEqualTo(
                "You currently don't have access to any Provider Firms. Please contact the provider firm's admin to be added.");
        assertThat(firmDto.isCanChange()).isFalse();
    }

    @Test
    void getActiveFirm_multi_firm_single_profile() {
        Firm firm = Firm.builder().name("Firm").code("Code").build();
        UserProfile userProfile = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(true).firm(firm)
                .build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(true).userProfiles(Set.of(userProfile)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication, null);
        assertThat(firmDto.getName()).isEqualTo("Firm");
        assertThat(firmDto.isCanChange()).isFalse();
    }

    @Test
    void getActiveFirm_multi_firm_multi_profiles() {
        Firm firm1 = Firm.builder().name("Firm1").code("Code1").build();
        Firm firm2 = Firm.builder().name("Firm2").code("Code2").build();
        UserProfile userProfile1 = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(false).firm(firm1)
                .build();
        UserProfile userProfile2 = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(true).firm(firm2)
                .build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(true).userProfiles(Set.of(userProfile1, userProfile2))
                .build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication, null);
        assertThat(firmDto.getName()).isEqualTo("Firm2");
        assertThat(firmDto.isCanChange()).isTrue();
    }

    @Test
    void handleClientAuthorizationRequired_redirectsToAuthorization() {
        // Given
        ClientAuthorizationRequiredException exception = new ClientAuthorizationRequiredException("azure");

        // When
        RedirectView redirectView = controller.handleClientAuthorizationRequired(exception);

        // Then
        assertThat(redirectView).isNotNull();
        assertThat(redirectView.getUrl()).isEqualTo("/oauth2/authorization/azure");
    }

    @Test
    void testGetCurrentUserProfile() {
        // Arrange
        CurrentUserDto expectedDto = new CurrentUserDto();
        when(loginService.getCurrentUser(authentication)).thenReturn(expectedDto);

        // Act
        CurrentUserDto result = controller.getCurrentUserProfile(authentication, null);

        // Assert
        assertThat(result).isEqualTo(expectedDto);
        verify(loginService).getCurrentUser(authentication);
    }

    @Test
    public void testIsInternal_ReturnsTrue_WhenUserTypeIsInternal() {
        UserProfile userProfile = UserProfile.builder().userType(UserType.INTERNAL).activeProfile(false).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);

        boolean result = controller.isInternal(authentication, null);

        assertThat(result).isTrue();
    }

    @Test
    public void testIsInternal_ReturnsFalse_WhenUserTypeIsNotInternal() {
        UserProfile userProfile = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(false).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);

        boolean result = controller.isInternal(authentication, null);

        assertThat(result).isFalse();
    }

    @Test
    public void testIsExternal_ReturnsTrue_WhenUserTypeIsExternal() {
        UserProfile userProfile = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(false).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);

        boolean result = controller.isExternal(authentication, null);

        assertThat(result).isTrue();
    }

    @Test
    public void testIsExternal_ReturnsFalse_WhenUserTypeIsNotExternal() {
        UserProfile userProfile = UserProfile.builder().userType(UserType.INTERNAL).activeProfile(false).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(userProfile);

        boolean result = controller.isExternal(authentication, null);

        assertThat(result).isFalse();
    }

}
