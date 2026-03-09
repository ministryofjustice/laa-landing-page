package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.DisableUserReasonDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.entity.UserTypeReasonDisable;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserReasonRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.ChangeAccountEnabledResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserAccountStatusServiceTest {

    @Mock
    private DisableUserReasonRepository disableUserReasonRepository;
    @Mock
    private UserAccountStatusAuditRepository userAccountStatusAuditRepository;
    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private TechServicesClient techServicesClient;
    @Mock
    private UserService userService;
    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private UserAccountStatusService userAccountStatusService;

    @BeforeEach
    public void setup() {
        ModelMapper mapper = new MapperConfig().modelMapper();
        userAccountStatusService = new UserAccountStatusService(disableUserReasonRepository,
                userAccountStatusAuditRepository,
                mapper,
                entraUserRepository,
                techServicesClient,
                userService,
                userProfileRepository);
    }

    @Test
    public void testGetDisableUserReasonsMapsReasonsCorrectly() {
        // Given
        DisableUserReason reason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Test Reason")
                .description("A test reason.")
                .build();

        when(disableUserReasonRepository.findAll()).thenReturn(List.of(reason));

        // When
        List<DisableUserReasonDto> returnedReasons = userAccountStatusService.getDisableUserReasons(UserTypeReasonDisable.DEFAULT);

        // Then
        assertThat(returnedReasons).isNotEmpty();
        DisableUserReasonDto returnedReason = returnedReasons.getFirst();
        assertThat(returnedReason.getId()).isEqualTo(reason.getId());
        assertThat(returnedReason.getName()).isEqualTo(reason.getName());
        assertThat(returnedReason.getDescription()).isEqualTo(reason.getDescription());
    }

    @Test
    public void testGetDisableUserReasonsMapsReasonsCorrectlyForProvideAdmin() {
        // Given
        DisableUserReason reason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Test Reason")
                .description("A test reason.")
                .build();
        DisableUserReason absence = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Absence")
                .description("A Absence reason.")
                .build();
        DisableUserReason providerDiscretion = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Provider Discretion")
                .description("A Provider Discretion reason.")
                .build();

        when(disableUserReasonRepository.findAll()).thenReturn(List.of(reason, absence, providerDiscretion));

        // When
        List<DisableUserReasonDto> returnedReasons = userAccountStatusService.getDisableUserReasons(UserTypeReasonDisable.IS_USER_DISABLE);

        // Then
        assertThat(returnedReasons).isNotEmpty();
        assertThat(returnedReasons.size()).isEqualTo(2);
        ModelMapper mapper = new ModelMapper();
        assertThat(returnedReasons)
                .isEqualTo(List.of(mapper.map(absence, DisableUserReasonDto.class),
                        mapper.map(providerDiscretion, DisableUserReasonDto.class)));

    }

    @Test
    public void testGetDisableUserReasonsMapsReasonsCorrectlyForBulkDisable() {
        // Given
        DisableUserReason reason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Test Reason")
                .description("A test reason.")
                .build();

        DisableUserReason complianceBreach = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Compliance Breach")
                .description("A compliance breach reason.")
                .build();

        DisableUserReason contractEnded = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Contract Ended")
                .description("A contract ended reason.")
                .build();

        DisableUserReason cyberRisk = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Cyber Risk")
                .description("A cyber risk reason.")
                .build();

        DisableUserReason firmClosureMerger = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Firm Closure / Merger")
                .description("A firm closure / merger reason.")
                .build();

        DisableUserReason investigationPending = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Investigation Pending")
                .description("An investigation pending reason.")
                .build();

        DisableUserReason userRequest = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("User Request")
                .description("A user request reason.")
                .build();

        when(disableUserReasonRepository.findAll()).thenReturn(List.of(reason,
                complianceBreach,
                contractEnded,
                cyberRisk,
                firmClosureMerger,
                investigationPending,
                userRequest));

        // When
        List<DisableUserReasonDto> returnedReasons = userAccountStatusService.getDisableUserReasons(UserTypeReasonDisable.BULK_DISABLE);

        // Then
        assertThat(returnedReasons).isNotEmpty();
        assertThat(returnedReasons.size()).isEqualTo(6);
        ModelMapper mapper = new ModelMapper();
        assertThat(returnedReasons)
                .isEqualTo(List.of(
                        mapper.map(complianceBreach, DisableUserReasonDto.class),
                        mapper.map(contractEnded, DisableUserReasonDto.class),
                        mapper.map(cyberRisk, DisableUserReasonDto.class),
                        mapper.map(firmClosureMerger, DisableUserReasonDto.class),
                        mapper.map(investigationPending, DisableUserReasonDto.class),
                        mapper.map(userRequest, DisableUserReasonDto.class)
                        ));

    }

    @Test
    public void testDisableUserDisabledUserWhenTechServicesRequestIsSuccess() {
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile disabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .userProfiles(Set.of(disabledUserProfile))
                .build();
        UserProfile disabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
                .userProfiles(Set.of(disabledByUserProfile))
                .build();
        DisableUserReason disableUserReason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Reason")
                .description("A test reason")
                .entraDescription("ATestReason")
                .build();
        TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.success(null);

        when(entraUserRepository.findById(eq(disabledUser.getId()))).thenReturn(Optional.of(disabledUser));
        when(entraUserRepository.findById(eq(disabledByUser.getId()))).thenReturn(Optional.of(disabledByUser));
        when(disableUserReasonRepository.findById(eq(disableUserReason.getId()))).thenReturn(Optional.of(disableUserReason));
        when(techServicesClient.disableUser(any(), any())).thenReturn(techServicesResponse);

        userAccountStatusService.disableUser(disabledUser.getId(), disableUserReason.getId(), disabledByUser.getId());

        assertThat(disabledUser.isEnabled()).isFalse();
        verify(entraUserRepository, times(1)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserDisabledUserWhenDisabledByInternalUser() {
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile disabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .userProfiles(Set.of(disabledUserProfile))
                .build();
        UserProfile disabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.INTERNAL)
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
                .userProfiles(Set.of(disabledByUserProfile))
                .build();
        DisableUserReason disableUserReason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Reason")
                .description("A test reason")
                .entraDescription("ATestReason")
                .build();
        TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.success(null);

        when(entraUserRepository.findById(eq(disabledUser.getId()))).thenReturn(Optional.of(disabledUser));
        when(entraUserRepository.findById(eq(disabledByUser.getId()))).thenReturn(Optional.of(disabledByUser));
        when(disableUserReasonRepository.findById(eq(disableUserReason.getId()))).thenReturn(Optional.of(disableUserReason));
        when(techServicesClient.disableUser(any(), any())).thenReturn(techServicesResponse);
        when(userService.isInternal(any(UUID.class))).thenReturn(true);

        userAccountStatusService.disableUser(disabledUser.getId(), disableUserReason.getId(), disabledByUser.getId());

        assertThat(disabledUser.isEnabled()).isFalse();
        verify(entraUserRepository, times(1)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionWhenDisableMultiFirmUser() {
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile disabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .multiFirmUser(true)
                .userProfiles(Set.of(disabledUserProfile))
                .build();
        UserProfile disabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.INTERNAL)
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
                .userProfiles(Set.of(disabledByUserProfile))
                .build();
        DisableUserReason disableUserReason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Reason")
                .description("A test reason")
                .entraDescription("ATestReason")
                .build();

        when(entraUserRepository.findById(eq(disabledUser.getId()))).thenReturn(Optional.of(disabledUser));
        when(entraUserRepository.findById(eq(disabledByUser.getId()))).thenReturn(Optional.of(disabledByUser));
        when(disableUserReasonRepository.findById(eq(disableUserReason.getId()))).thenReturn(Optional.of(disableUserReason));

        assertThrows(RuntimeException.class,
                () -> userAccountStatusService.disableUser(disabledUser.getId(), disableUserReason.getId(), disabledByUser.getId()));
        assertThat(disabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionWhenSelfDisable() {
        UserProfile disabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
                .userProfiles(Set.of(disabledByUserProfile))
                .build();
        DisableUserReason disableUserReason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Reason")
                .description("A test reason")
                .entraDescription("ATestReason")
                .build();

        assertThrows(RuntimeException.class,
                () -> userAccountStatusService.disableUser(disabledByUser.getId(), disableUserReason.getId(), disabledByUser.getId()));
        assertThat(disabledByUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionWhenTechServicesRequestIsError() {
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile disabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .userProfiles(Set.of(disabledUserProfile))
                .build();
        UserProfile disabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
                .userProfiles(Set.of(disabledByUserProfile))
                .build();
        DisableUserReason disableUserReason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Reason")
                .description("A test reason")
                .entraDescription("ATestReason")
                .build();
        TechServicesErrorResponse errorResponse = TechServicesErrorResponse.builder()
                .code("TestCode")
                .success(false)
                .errors(new String[] {"Error"})
                .message("An error occurred")
                .build();
        TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.error(errorResponse);

        when(entraUserRepository.findById(eq(disabledUser.getId()))).thenReturn(Optional.of(disabledUser));
        when(entraUserRepository.findById(eq(disabledByUser.getId()))).thenReturn(Optional.of(disabledByUser));
        when(disableUserReasonRepository.findById(eq(disableUserReason.getId()))).thenReturn(Optional.of(disableUserReason));
        when(techServicesClient.disableUser(any(), any())).thenReturn(techServicesResponse);

        assertThrows(TechServicesClientException.class, () -> userAccountStatusService.disableUser(disabledUser.getId(), disableUserReason.getId(), disabledByUser.getId()));
        assertThat(disabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionWhenNotInSameFirm() {
        Firm firm1 = Firm.builder().id(UUID.randomUUID()).name("Test Firm 1").build();
        UserProfile disabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm1)
                .build();
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .userProfiles(Set.of(disabledUserProfile))
                .build();
        Firm firm2 = Firm.builder().id(UUID.randomUUID()).name("Test Firm 2").build();
        UserProfile disabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm2)
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
                .userProfiles(Set.of(disabledByUserProfile))
                .build();
        DisableUserReason disableUserReason = DisableUserReason.builder()
                .id(UUID.randomUUID())
                .name("Reason")
                .description("A test reason")
                .entraDescription("ATestReason")
                .build();

        when(entraUserRepository.findById(eq(disabledUser.getId()))).thenReturn(Optional.of(disabledUser));
        when(entraUserRepository.findById(eq(disabledByUser.getId()))).thenReturn(Optional.of(disabledByUser));
        when(disableUserReasonRepository.findById(eq(disableUserReason.getId()))).thenReturn(Optional.of(disableUserReason));

        assertThrows(RuntimeException.class, () -> userAccountStatusService.disableUser(disabledUser.getId(), disableUserReason.getId(), disabledByUser.getId()));
        assertThat(disabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionWhenDisabledUserNotFound() {
        assertThrows(RuntimeException.class, () -> userAccountStatusService.disableUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        verify(entraUserRepository, times(1)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionWhenDisabledByUserNotFound() {
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .build();

        when(entraUserRepository.findById(eq(disabledUser.getId()))).thenReturn(Optional.of(disabledUser));

        assertThrows(RuntimeException.class, () -> userAccountStatusService.disableUser(disabledUser.getId(), UUID.randomUUID(), UUID.randomUUID()));
        assertThat(disabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(2)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserThrowsExceptionDisableReasonNotFound() {
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("EnabledBy")
                .lastName("User")
                .build();

        when(entraUserRepository.findById(eq(disabledUser.getId()))).thenReturn(Optional.of(disabledUser));
        when(entraUserRepository.findById(eq(disabledByUser.getId()))).thenReturn(Optional.of(disabledByUser));

        assertThrows(RuntimeException.class, () -> userAccountStatusService.disableUser(disabledUser.getId(), UUID.randomUUID(), disabledByUser.getId()));
        assertThat(disabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(2)).findById(any());
        verify(disableUserReasonRepository, times(1)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserEnabledUserWhenTechServicesRequestIsSuccess() {
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile enabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .enabled(false)
                .userProfiles(Set.of(enabledUserProfile))
                .build();
        UserProfile enabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser enabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("EnabledBy")
                .lastName("User")
                .userProfiles(Set.of(enabledByUserProfile))
                .build();

        TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.success(null);

        when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
        when(entraUserRepository.findById(eq(enabledByUser.getId()))).thenReturn(Optional.of(enabledByUser));
        when(techServicesClient.enableUser(any())).thenReturn(techServicesResponse);

        userAccountStatusService.enableUser(enabledUser.getId(), enabledByUser.getId());

        assertThat(enabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(1)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserEnablesUserWhenEnabledByInternalUser() {
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile enabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .enabled(false)
                .userProfiles(Set.of(enabledUserProfile))
                .build();
        UserProfile enabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.INTERNAL)
                .build();
        EntraUser enabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("EnabledBy")
                .lastName("User")
                .userProfiles(Set.of(enabledByUserProfile))
                .build();
        TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.success(null);

        when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
        when(entraUserRepository.findById(eq(enabledByUser.getId()))).thenReturn(Optional.of(enabledByUser));
        when(techServicesClient.enableUser(any())).thenReturn(techServicesResponse);
        when(userService.isInternal(any(UUID.class))).thenReturn(true);

        userAccountStatusService.enableUser(enabledUser.getId(), enabledByUser.getId());

        assertThat(enabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(1)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserThrowsExceptionWhenEnableMultiFirmUser() {
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile enabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .multiFirmUser(true)
                .enabled(false)
                .userProfiles(Set.of(enabledUserProfile))
                .build();
        UserProfile disabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.INTERNAL)
                .build();
        EntraUser enabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("EnabledBy")
                .lastName("User")
                .userProfiles(Set.of(disabledByUserProfile))
                .build();

        when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
        when(entraUserRepository.findById(eq(enabledByUser.getId()))).thenReturn(Optional.of(enabledByUser));

        assertThrows(RuntimeException.class,
                () -> userAccountStatusService.enableUser(enabledUser.getId(), enabledByUser.getId()));
        assertThat(enabledUser.isEnabled()).isFalse();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserThrowsExceptionWhenSelfEnable() {
        UserProfile enabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.EXTERNAL)
                .build();
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("EnabledBy")
                .lastName("User")
                .enabled(false)
                .userProfiles(Set.of(enabledByUserProfile))
                .build();

        assertThrows(RuntimeException.class,
                () -> userAccountStatusService.enableUser(enabledUser.getId(), enabledUser.getId()));
        assertThat(enabledUser.isEnabled()).isFalse();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserThrowsExceptionWhenTechServicesRequestIsError() {
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile enabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .enabled(false)
                .userProfiles(Set.of(enabledUserProfile))
                .build();
        UserProfile enabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();
        EntraUser enabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("EnabledBy")
                .lastName("User")
                .userProfiles(Set.of(enabledByUserProfile))
                .build();
        TechServicesErrorResponse errorResponse = TechServicesErrorResponse.builder()
                .code("TestCode")
                .success(false)
                .errors(new String[] {"Error"})
                .message("An error occurred")
                .build();
        TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.error(errorResponse);

        when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
        when(entraUserRepository.findById(eq(enabledByUser.getId()))).thenReturn(Optional.of(enabledByUser));
        when(techServicesClient.enableUser(any())).thenReturn(techServicesResponse);

        assertThrows(TechServicesClientException.class, () -> userAccountStatusService.enableUser(enabledUser.getId(), enabledByUser.getId()));
        assertThat(enabledUser.isEnabled()).isFalse();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserThrowsExceptionWhenNotInSameFirm() {
        Firm firm1 = Firm.builder().id(UUID.randomUUID()).name("Test Firm 1").build();
        UserProfile enabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm1)
                .build();
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .enabled(false)
                .userProfiles(Set.of(enabledUserProfile))
                .build();
        Firm firm2 = Firm.builder().id(UUID.randomUUID()).name("Test Firm 2").build();
        UserProfile enabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm2)
                .build();
        EntraUser enabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("EnabledBy")
                .lastName("User")
                .userProfiles(Set.of(enabledByUserProfile))
                .build();

        when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
        when(entraUserRepository.findById(eq(enabledByUser.getId()))).thenReturn(Optional.of(enabledByUser));

        assertThrows(RuntimeException.class, () -> userAccountStatusService.enableUser(enabledUser.getId(), enabledByUser.getId()));
        assertThat(enabledUser.isEnabled()).isFalse();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserThrowsExceptionWhenEnabledUserNotFound() {
        assertThrows(RuntimeException.class, () -> userAccountStatusService.enableUser(UUID.randomUUID(), UUID.randomUUID()));
        verify(entraUserRepository, times(1)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserThrowsExceptionWhenDisabledByUserNotFound() {
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .build();

        when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));

        assertThrows(RuntimeException.class, () -> userAccountStatusService.enableUser(enabledUser.getId(), UUID.randomUUID()));
        assertThat(enabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(2)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Nested
    class UserCountsForFirmTests {

        @Test
        void shouldReturnCorrectCountsForSingleAndMultiFirmUsers() {
            UUID firmId = UUID.randomUUID();
            List<UserProfile> userProfiles = List.of(
                    UserProfile.builder()
                            .entraUser(EntraUser.builder()
                                    .enabled(true)
                                    .build())
                            .build(),
                    UserProfile.builder()
                            .entraUser(EntraUser.builder()
                                    .enabled(true)
                                    .multiFirmUser(true)
                                    .build())
                            .build()
            );
            when(userProfileRepository.findByFirmId(firmId)).thenReturn(userProfiles);


            Map<String, Long> result = userAccountStatusService.getUserCountsForFirm(String.valueOf(firmId));


            // Assert
            assertThat(result.get("totalOfSingleFirm")).isEqualTo(1);
            assertThat(result.get("totalOfMultiFirm")).isEqualTo(1);

        }

        @Test
        void shouldReturnCorrectCountsForMultiFirmUsersOnly() {
            UUID firmId = UUID.randomUUID();
            List<UserProfile> userProfiles = List.of(
                    UserProfile.builder()
                            .entraUser(EntraUser.builder()
                                    .enabled(true)
                                    .multiFirmUser(true)
                                    .build())
                            .build()
            );
            when(userProfileRepository.findByFirmId(firmId)).thenReturn(userProfiles);

            Map<String, Long> result = userAccountStatusService.getUserCountsForFirm(String.valueOf(firmId));

            // Assert
            assertThat(result.get("totalOfSingleFirm")).isEqualTo(0);
            assertThat(result.get("totalOfMultiFirm")).isEqualTo(1);

        }

        @Test
        void shouldReturnCorrectCountsForSingleFirmOnly() {
            UUID firmId = UUID.randomUUID();
            List<UserProfile> userProfiles = List.of(
                    UserProfile.builder()
                            .entraUser(EntraUser.builder()
                                    .enabled(true)
                                    .build())
                            .build()
            );
            when(userProfileRepository.findByFirmId(firmId)).thenReturn(userProfiles);

            Map<String, Long> result = userAccountStatusService.getUserCountsForFirm(String.valueOf(firmId));

            // Assert
            assertThat(result.get("totalOfSingleFirm")).isEqualTo(1);
            assertThat(result.get("totalOfMultiFirm")).isEqualTo(0);

        }

    }

    @Nested
    class HasActiveUserByFirmIdTest {

        @Test
        void shouldReturnTrueWhenIsWithoutAnyActiveUserByFirmId() {
            UUID firmId = UUID.randomUUID();
            boolean result = userAccountStatusService.hasActiveUserByFirmId(String.valueOf(firmId));
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnTrueWhenHasActiveUserByFirmId() {
            UUID firmId = UUID.randomUUID();
            List<UserProfile> userProfiles = List.of(
                    UserProfile.builder()
                            .entraUser(EntraUser.builder()
                                    .enabled(true)
                                    .build())
                            .build()
            );
            when(userProfileRepository.findByFirmId(firmId)).thenReturn(userProfiles);

            boolean result = userAccountStatusService.hasActiveUserByFirmId(String.valueOf(firmId));

            assertThat(result).isTrue();
        }

    }

    @Nested
    class DisableUserAllUserByFirmIdTest {

        @Test
        public void testDisableAllUserThrowsExceptionWhenDisabledByUserNotFound() {
            EntraUser enabledUser = EntraUser.builder()
                    .id(UUID.randomUUID())
                    .firstName("Enabled")
                    .lastName("User")
                    .build();

            when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
            assertThrows(RuntimeException.class, () -> userAccountStatusService.disableUserAllUserByFirmId("firmId", UUID.randomUUID(), UUID.randomUUID()));

        }

        @Test
        public void testDisableAllUserThrowsExceptionWhenDisabledByReasonNotFound() {
            EntraUser enabledUser = EntraUser.builder()
                    .id(UUID.randomUUID())
                    .firstName("Enabled")
                    .lastName("User")
                    .build();

            DisableUserReason reason = DisableUserReason.builder()
                    .id(UUID.randomUUID())
                    .build();

            when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
            when(disableUserReasonRepository.findById(eq(reason.getId()))).thenReturn(Optional.of(reason));
            assertThrows(RuntimeException.class, () -> userAccountStatusService.disableUserAllUserByFirmId("firmID", UUID.randomUUID(), enabledUser.getId()));

        }

        @Test
        public void testDisableAllUserSuccessfully() {
            UUID firmId = UUID.randomUUID();
            EntraUser enabledUser = EntraUser.builder()
                    .id(UUID.randomUUID())
                    .firstName("Enabled")
                    .lastName("User")
                    .build();

            DisableUserReason reason = DisableUserReason.builder()
                    .id(UUID.randomUUID())
                    .name("test Reason")
                    .build();

            List<UserProfile> userProfiles = List.of(
                    UserProfile.builder()
                            .entraUser(EntraUser.builder()
                                    .enabled(true)
                                    .build())
                            .id(UUID.randomUUID())
                            .build()

            );
            TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.success(null);

            when(techServicesClient.disableUser(any(), any())).thenReturn(techServicesResponse);
            when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
            when(disableUserReasonRepository.findById(eq(reason.getId()))).thenReturn(Optional.of(reason));
            when(userProfileRepository.findByFirmId(eq(firmId))).thenReturn(userProfiles);

            userAccountStatusService.disableUserAllUserByFirmId(String.valueOf(firmId), reason.getId(), enabledUser.getId());

            verify(entraUserRepository, times(1)).saveAndFlush(any());
            verify(techServicesClient, times(1)).disableUser(any(), any());
            verify(userAccountStatusAuditRepository, times(1)).saveAndFlush(any());

        }

        @Test
        public void testDisableAllUserExceptionFromTechService() {
            UUID firmId = UUID.randomUUID();
            EntraUser enabledUser = EntraUser.builder()
                    .id(UUID.randomUUID())
                    .firstName("Enabled")
                    .lastName("User")
                    .build();

            DisableUserReason reason = DisableUserReason.builder()
                    .id(UUID.randomUUID())
                    .name("test Reason")
                    .build();

            List<UserProfile> userProfiles = List.of(
                    UserProfile.builder()
                            .entraUser(EntraUser.builder()
                                    .enabled(true)
                                    .build())
                            .id(UUID.randomUUID())
                            .build()

            );
            TechServicesErrorResponse errorResponse = TechServicesErrorResponse.builder()
                    .code("TestCode")
                    .success(false)
                    .errors(new String[] {"Error"})
                    .message("An error occurred")
                    .build();
            TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.error(errorResponse);

            when(techServicesClient.disableUser(any(), any())).thenReturn(techServicesResponse);
            when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
            when(disableUserReasonRepository.findById(eq(reason.getId()))).thenReturn(Optional.of(reason));
            when(userProfileRepository.findByFirmId(eq(firmId))).thenReturn(userProfiles);

            assertThrows(TechServicesClientException.class, () ->
                    userAccountStatusService.disableUserAllUserByFirmId(
                            String.valueOf(firmId),
                            reason.getId(),
                            enabledUser.getId()));

            verify(techServicesClient, times(1)).disableUser(any(), any());
            verify(entraUserRepository, times(0)).saveAndFlush(any());
            verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());

        }
    }
}
