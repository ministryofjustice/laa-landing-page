package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
        assertThat(controller.getActiveFirm(authentication)).isNull();
    }

    @Test
    void getActiveFirm_noProfileSet() {
        EntraUser entraUser = EntraUser.builder().build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication);
        assertThat(firmDto.getName()).isEqualTo("You currently don’t have access to any profiles. Please contact the admin to be added.");
        assertThat(firmDto.isCanChange()).isFalse();
    }

    @Test
    void getActiveFirm_single_firm_external() {
        Firm firm = Firm.builder().name("Firm").code("Code").build();
        UserProfile userProfile = UserProfile.builder().userType(UserType.EXTERNAL).firm(firm).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(false).userProfiles(Set.of(userProfile)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication);
        assertThat(firmDto.getName()).isEqualTo("Firm (Code)");
        assertThat(firmDto.isCanChange()).isFalse();
    }

    @Test
    void getActiveFirm_single_firm_internal() {
        UserProfile userProfile = UserProfile.builder().userType(UserType.INTERNAL).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(false).userProfiles(Set.of(userProfile)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication);
        assertThat(firmDto).isNull();
    }

    @Test
    void getActiveFirm_multi_firm_not_active_profile() {
        Firm firm = Firm.builder().name("Firm").code("Code").build();
        UserProfile userProfile = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(false).firm(firm).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(true).userProfiles(Set.of(userProfile)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication);
        assertThat(firmDto.getName()).isEqualTo("You currently don’t have access to any Provider Firms. Please contact the provider firm’s admin to be added.");
        assertThat(firmDto.isCanChange()).isFalse();
    }

    @Test
    void getActiveFirm_multi_firm_single_profile() {
        Firm firm = Firm.builder().name("Firm").code("Code").build();
        UserProfile userProfile = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(true).firm(firm).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(true).userProfiles(Set.of(userProfile)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication);
        assertThat(firmDto.getName()).isEqualTo("Firm (Code)");
        assertThat(firmDto.isCanChange()).isFalse();
    }

    @Test
    void getActiveFirm_multi_firm_multi_profiles() {
        Firm firm1 = Firm.builder().name("Firm1").code("Code1").build();
        Firm firm2 = Firm.builder().name("Firm2").code("Code2").build();
        UserProfile userProfile1 = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(false).firm(firm1).build();
        UserProfile userProfile2 = UserProfile.builder().userType(UserType.EXTERNAL).activeProfile(true).firm(firm2).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(true).userProfiles(Set.of(userProfile1, userProfile2)).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        FirmDto firmDto = controller.getActiveFirm(authentication);
        assertThat(firmDto.getName()).isEqualTo("Firm2 (Code2)");
        assertThat(firmDto.isCanChange()).isTrue();
    }

}