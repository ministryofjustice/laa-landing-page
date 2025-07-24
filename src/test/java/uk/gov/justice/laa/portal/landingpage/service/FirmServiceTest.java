package uk.gov.justice.laa.portal.landingpage.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

@ExtendWith(MockitoExtension.class)
class FirmServiceTest {

    @InjectMocks
    private FirmService firmService;
    @Mock
    private FirmRepository firmRepository;
    @Mock
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void setUp() {
        firmService = new FirmService(
            firmRepository,
            userProfileRepository,
            new MapperConfig().modelMapper()
        );
    }

    @Test
    void getFirms() {
        Firm firm1 = Firm.builder().build();
        Firm firm2 = Firm.builder().build();
        List<Firm> dbFirms = List.of(firm1, firm2);
        when(firmRepository.findAll()).thenReturn(dbFirms);
        List<FirmDto> firms = firmService.getFirms();
        assertThat(firms).hasSize(2);
    }

    @Test
    void getFirm() {
        UUID firmId = UUID.randomUUID();
        Firm dbFirm = Firm.builder().id(firmId).build();
        when(firmRepository.getReferenceById(firmId)).thenReturn(dbFirm);
        FirmDto firm = firmService.getFirm(firmId.toString());
        assertThat(firm.getId()).isEqualTo(firmId);
    }

    @Test
    void getUserFirms() {
        UserProfile up1 = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F1").build()).build();
        UserProfile up2 = UserProfile.builder().activeProfile(false).userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F2").build()).build();
        Set<UserProfile> userProfiles = Set.of(up1, up2);
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        List<FirmDto> firms = firmService.getUserFirms(entraUser);
        assertThat(firms).hasSize(1);
        assertThat(firms.getFirst().getName()).isEqualTo("F1");
    }

    @Test
    void getUserAllFirms() {
        UserProfile up1 = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F1").build()).build();
        UserProfile up2 = UserProfile.builder().activeProfile(false).userProfileStatus(UserProfileStatus.COMPLETE).firm(Firm.builder().name("F2").build()).build();
        Set<UserProfile> userProfiles = Set.of(up1, up2);
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        List<FirmDto> firms = firmService.getUserAllFirms(entraUser);
        assertThat(firms).hasSize(2);
    }

    @Test
    void getUserFirmsByUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID firmId = UUID.randomUUID();
        Firm firm = Firm.builder().id(firmId).name("Test Firm").build();
        UserProfile userProfile = UserProfile.builder().id(userId).activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE).firm(firm).build();

        when(userProfileRepository.findById(userId)).thenReturn(java.util.Optional.of(userProfile));

        // When
        List<FirmDto> firms = firmService.getUserFirmsByUserId(userId.toString());

        // Then
        assertThat(firms).hasSize(1);
        assertThat(firms.getFirst().getId()).isEqualTo(firmId);
        assertThat(firms.getFirst().getName()).isEqualTo("Test Firm");
    }

    @Test
    void getUserFirmsByUserId_userNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userProfileRepository.findById(userId)).thenReturn(java.util.Optional.empty());

        // When
        List<FirmDto> firms = firmService.getUserFirmsByUserId(userId.toString());

        // Then
        assertThat(firms).isEmpty();
    }
}