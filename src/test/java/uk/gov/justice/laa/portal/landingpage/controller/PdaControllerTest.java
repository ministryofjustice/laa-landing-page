package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PdaControllerTest {

    @InjectMocks
    private PdaController controller;
    @Mock
    private LoginService loginService;
    @Mock
    private UserService userService;
    @Mock
    private OfficeService officeService;
    @Mock
    private FirmService firmService;
    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new PdaController(
                loginService,
                userService,
                officeService,
                firmService,
                new MapperConfig().modelMapper()
        );
    }

    @Test
    void getFirms_internal() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder()
                        .userProfiles(Set.of(UserProfile.builder()
                                .firm(Firm.builder().build())
                                .userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN).build())).build());
        Model model = new ConcurrentModel();
        String view = controller.getFirms(model, authentication);
        assertThat(view).isEqualTo("firms");
        assertThat((List) model.getAttribute("firms")).hasSize(1);
    }

    @Test
    void getFirms_external() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder().userProfiles(Set.of(UserProfile.builder().userType(UserType.INTERNAL).build()))
                        .build());
        when(firmService.getFirms()).thenReturn(List.of(FirmDto.builder().build()));
        Model model = new ConcurrentModel();
        String view = controller.getFirms(model, authentication);
        assertThat(view).isEqualTo("firms");
        assertThat((List) model.getAttribute("firms")).hasSize(1);
    }

    @Test
    void getFirm_internal() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder().userProfiles(Set.of(UserProfile.builder().userType(UserType.INTERNAL).build()))
                        .build());
        when(firmService.getFirm(any()))
                .thenReturn(FirmDto.builder().build());
        Model model = new ConcurrentModel();
        String view = controller.getFirm(UUID.randomUUID().toString(), model, authentication);
        assertThat(view).isEqualTo("firm");
        assertThat(model.getAttribute("firm")).isNotNull();
    }

    @Test
    void getFirm_external_office_belong_to_firm() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        UUID firmId = UUID.randomUUID();
        Firm firm = Firm.builder().id(firmId).build();
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder()
                        .userProfiles(Set.of(UserProfile.builder().userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN)
                                .firm(firm).build())).build());
        when(firmService.getFirm(any()))
                .thenReturn(FirmDto.builder().build());
        Model model = new ConcurrentModel();
        String view = controller.getFirm(firmId.toString(), model, authentication);
        assertThat(view).isEqualTo("firm");
        assertThat(model.getAttribute("firm")).isNotNull();
    }

    @Test
    void getFirm_external_not_belong_to_firm() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        UUID firmId1 = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID firmId2 = UUID.fromString("286c66b2-08b3-48c7-94a7-d66ad4b68779");
        Firm firm = Firm.builder().id(firmId1).build();
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder()
                        .userProfiles(Set.of(UserProfile.builder().userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN)
                                .firm(firm).build())).build());
        Model model = new ConcurrentModel();
        String view = controller.getFirm(firmId2.toString(), model, authentication);
        assertThat(view).isEqualTo("redirect:/pda/firms");
    }

    @Test
    void getOffices_internal() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder().userProfiles(Set.of(UserProfile.builder().userType(UserType.INTERNAL).build()))
                        .build());
        List<Office> list = List.of(Office.builder().build(), Office.builder().build());
        when(officeService.getOffices())
                .thenReturn(list);
        Model model = new ConcurrentModel();
        String view = controller.getOffices(model, authentication);
        assertThat(view).isEqualTo("offices");
        assertThat((List) model.getAttribute("offices")).hasSize(2);
    }

    @Test
    void getOffices_external() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder()
                        .userProfiles(Set.of(UserProfile.builder()
                                .firm(Firm.builder().build())
                                .userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN).build())).build());
        List<Office> list = List.of(Office.builder().build());
        when(officeService.getOfficesByFirms(any()))
                .thenReturn(list);
        Model model = new ConcurrentModel();
        String view = controller.getOffices(model, authentication);
        assertThat(view).isEqualTo("offices");
        assertThat((List) model.getAttribute("offices")).hasSize(1);
    }

    @Test
    void getOffice_internal() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder().userProfiles(Set.of(UserProfile.builder().userType(UserType.INTERNAL).build()))
                        .build());
        when(officeService.getOffice(any()))
                .thenReturn(Office.builder().id(UUID.randomUUID()).firm(Firm.builder().id(UUID.randomUUID())
                        .build()).build());
        Model model = new ConcurrentModel();
        String view = controller.getOffice(UUID.randomUUID().toString(), model, authentication);
        assertThat(view).isEqualTo("office");
        assertThat(model.getAttribute("office")).isNotNull();
    }

    @Test
    void getOffice_external_office_belong_to_firm() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        UUID firmId = UUID.randomUUID();
        Firm firm = Firm.builder().id(firmId).build();
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder()
                        .userProfiles(Set.of(UserProfile.builder().userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN)
                                .firm(firm).build())).build());
        when(officeService.getOffice(any()))
                .thenReturn(Office.builder().id(UUID.randomUUID()).firm(firm).build());
        Model model = new ConcurrentModel();
        String view = controller.getOffice(UUID.randomUUID().toString(), model, authentication);
        assertThat(view).isEqualTo("office");
        assertThat(model.getAttribute("office")).isNotNull();
    }

    @Test
    void getOffice_external_office_not_belong_to_firm() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        UUID firmId1 = UUID.fromString("00000000-0000-0000-0000-000000000000");
        UUID firmId2 = UUID.fromString("286c66b2-08b3-48c7-94a7-d66ad4b68779");
        Firm firm1 = Firm.builder().id(firmId1).build();
        Firm firm2 = Firm.builder().id(firmId2).build();
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any()))
                .thenReturn(EntraUser.builder()
                        .userProfiles(Set.of(UserProfile.builder().userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN)
                                .firm(firm1).build())).build());
        when(officeService.getOffice(any()))
                .thenReturn(Office.builder().id(UUID.randomUUID()).firm(firm2).build());
        Model model = new ConcurrentModel();
        String view = controller.getOffice(UUID.randomUUID().toString(), model, authentication);
        assertThat(view).isEqualTo("redirect:/pda/offices");
    }

    @Test
    void getCurrentEntraUser_Ok() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        when(loginService.getCurrentUser(any())).thenReturn(currentUserDto);
        when(userService.getUserByEntraId(any())).thenReturn(EntraUser.builder().build());
        assertThat(controller.getCurrentEntraUser(authentication)).isNotNull();
    }

    @Test
    void getCurrentEntraUser_fail() {
        CurrentUserDto currentUserDto = new CurrentUserDto();
        currentUserDto.setUserId(UUID.randomUUID());
        when(loginService.getCurrentUser(any())).thenReturn(new CurrentUserDto());
        when(userService.getUserByEntraId(any())).thenReturn(null);
        assertThrows(AssertionError.class, () -> {
            controller.getCurrentEntraUser(authentication);
        });
    }

    @Test
    void getUserOffices() {
        UserProfile up1 = UserProfile.builder().firm(Firm.builder().name("F1").build()).build();
        EntraUser entraUser = EntraUser.builder().userProfiles(Set.of(up1)).build();
        when(officeService.getOfficesByFirms(any())).thenReturn(new ArrayList<>());
        assertThat(controller.getUserOffices(entraUser)).isNotNull();
    }

    @Test
    void getUserFirms() {
        UserProfile up1 = UserProfile.builder().firm(Firm.builder().name("F1").build()).build();
        Set<UserProfile> userProfiles = Set.of(up1);
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        List<FirmDto> firms = controller.getUserFirms(entraUser);
        assertThat(firms).hasSize(1);
        assertThat(firms.getFirst().getName()).isEqualTo("F1");
    }

    @Test
    void isInternal_Ok() {
        Set<UserProfile> userProfiles = Set.of(UserProfile.builder().userType(UserType.INTERNAL).build());
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        assertThat(controller.isInternal(entraUser)).isTrue();
    }

    @Test
    void isInternal_Failed() {
        Set<UserProfile> userProfiles = Set.of(UserProfile.builder().userType(UserType.EXTERNAL_SINGLE_FIRM_ADMIN).build());
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        assertThat(controller.isInternal(entraUser)).isFalse();
    }
}
