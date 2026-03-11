package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.DisableUserReasonDto;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
                .errors(new String[]{"Error"})
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
        UUID disabledById = UUID.randomUUID();
        UserProfileDto disabledByUserProfile = UserProfileDto.builder()
                .id(disabledById)
                .activeProfile(true)
                .appRoles(List.of(AppRoleDto.builder().id(UUID.randomUUID().toString()).name("Global Admin").build()))
                .build();
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
                .disabledBy(disabledById)
                .userProfiles(Set.of(enabledUserProfile))
                .build();
        UserProfile enabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .appRoles(Set.of(AppRole.builder().id(UUID.randomUUID()).name("Global Admin").build()))
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
        when(userService.getUserProfileById(disabledById.toString())).thenReturn(Optional.of(disabledByUserProfile));

        userAccountStatusService.enableUser(enabledUser.getId(), enabledByUser.getId());

        assertThat(enabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(1)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserEnablesUserWhenEnabledByInternalUser() {
        UUID disabledById = UUID.randomUUID();
        UserProfileDto disabledByUserProfile = UserProfileDto.builder()
                .id(disabledById)
                .activeProfile(true)
                .appRoles(List.of(AppRoleDto.builder().id(UUID.randomUUID().toString()).name("External User Admin").build()))
                .build();
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
                .disabledBy(disabledById)
                .build();
        UserProfile enabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.INTERNAL)
                .appRoles(Set.of(AppRole.builder().id(UUID.randomUUID()).name("External User Admin").build()))
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
        when(userService.getUserProfileById(disabledById.toString())).thenReturn(Optional.of(disabledByUserProfile));

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
        UserProfileDto disabledByUserProfile = UserProfileDto.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .userType(UserType.INTERNAL)
                .appRoles(List.of(AppRoleDto.builder().id(UUID.randomUUID().toString()).name("External User Admin").build()))
                .build();
        Firm firm = Firm.builder().id(UUID.randomUUID()).name("Test Firm").build();
        UserProfile enabledUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .appRoles(Set.of(AppRole.builder().id(UUID.randomUUID()).name("External User Admin").build()))
                .build();
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .enabled(false)
                .userProfiles(Set.of(enabledUserProfile))
                .disabledBy(disabledByUserProfile.getId())
                .build();
        UserProfile enabledByUserProfile = UserProfile.builder().id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .appRoles(Set.of(AppRole.builder().id(UUID.randomUUID()).name("External User Admin").build()))
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
                .errors(new String[]{"Error"})
                .message("An error occurred")
                .build();
        TechServicesApiResponse<ChangeAccountEnabledResponse> techServicesResponse = TechServicesApiResponse.error(errorResponse);

        when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));
        when(entraUserRepository.findById(eq(enabledByUser.getId()))).thenReturn(Optional.of(enabledByUser));
        when(techServicesClient.enableUser(any())).thenReturn(techServicesResponse);
        when(userService.getUserProfileById(disabledByUserProfile.getId().toString())).thenReturn(Optional.of(disabledByUserProfile));

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

    @Test
    public void testEnableUserThrowsExceptionWhenUserAlreadyEnabled() {
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .enabled(true)
                .build();

        when(entraUserRepository.findById(eq(enabledUser.getId()))).thenReturn(Optional.of(enabledUser));

        assertThrows(RuntimeException.class, () -> userAccountStatusService.enableUser(enabledUser.getId(), UUID.randomUUID()));
        assertThat(enabledUser.isEnabled()).isTrue();
        verify(entraUserRepository, times(2)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testEnableUserThrowsExceptionWhenUserIsMultiFirmEnabledByFirmUserManager() {
        EntraUser enabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Enabled")
                .lastName("User")
                .multiFirmUser(true)
                .enabled(false)
                .build();

        when(userService.isInternal(any(UUID.class))).thenReturn(false);
        when(entraUserRepository.findById(any(UUID.class))).thenReturn(Optional.of(enabledUser));

        assertThrows(RuntimeException.class, () -> userAccountStatusService.enableUser(enabledUser.getId(), UUID.randomUUID()));
        verify(entraUserRepository, times(2)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    void shouldThrow_whenActorEnablesSelf() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> userAccountStatusService.enableUser(id, id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("can not be enabled by themselves");
    }

    @Test
    void shouldThrow_whenTargetUserNotFound() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountStatusService.enableUser(targetId, actorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not find a user account to disable");
    }

    @Test
    void shouldThrow_whenActorUserNotFound() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .build();

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAccountStatusService.enableUser(targetId, actorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Could not find a user account with id");
    }

    @Test
    void shouldThrow_whenEnablementNotAllowedForMultiFirmUser() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .multiFirmUser(true)
                .disabledBy(UUID.randomUUID())
                .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true).build()))
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .userProfiles(Set.of(UserProfile.builder().id(UUID.randomUUID()).activeProfile(true).build()))
                .build();

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));

        // internal rule returns false
        when(userService.isInternal(actorId)).thenReturn(false);

        assertThatThrownBy(() -> userAccountStatusService.enableUser(targetId, actorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to enable the user");
    }

    @Test
    void shouldThrow_whenTechServiceCallFails() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Firm firm = Firm.builder().id(UUID.randomUUID()).build();

        UserProfile actorProfile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .firm(firm)
                .appRoles(Set.of(AppRole.builder()
                        .name(AuthzRole.FIRM_USER_MANAGER.getRoleName()).build()))
                .build();

        UserProfile targetProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();

        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .disabledBy(profileId)
                .userProfiles(Set.of(targetProfile))
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .userProfiles(Set.of(actorProfile))
                .firstName("John")
                .lastName("Doe")
                .build();

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userService.isInternal(actorId)).thenReturn(true);

        // Tech services failure mock
        TechServicesApiResponse<ChangeAccountEnabledResponse> failedResponse =
                TechServicesApiResponse.error(TechServicesErrorResponse.builder().code("FAIL")
                        .message("FAIL").build());

        when(techServicesClient.enableUser(any())).thenReturn(failedResponse);

        assertThatThrownBy(() -> userAccountStatusService.enableUser(targetId, actorId))
                .isInstanceOf(TechServicesClientException.class)
                .hasMessageContaining("FAIL");

        verify(entraUserRepository, never()).saveAndFlush(any());
        verify(userAccountStatusAuditRepository, never()).saveAndFlush(any());
    }

    @Test
    void shouldEnableUserSuccessfullyEnabledAndDisabledByExternalUserAdmin() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Firm firm = Firm.builder().id(UUID.randomUUID()).build();

        UserProfile actorProfile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .firm(firm)
                .appRoles(Set.of(
                        AppRole.builder()
                                .name(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())
                                .build()
                ))
                .build();

        UserProfile targetProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();

        UUID disabledById = UUID.randomUUID();
        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .disabledBy(disabledById)
                .userProfiles(Set.of(targetProfile))
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .firstName("John")
                .lastName("Doe")
                .userProfiles(Set.of(actorProfile))
                .build();

        UserProfileDto disablerProfile = UserProfileDto.builder()
                .id(disabledById)
                .appRoles(List.of(AppRoleDto.builder()
                        .name(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())
                        .build()))
                .build();

        ChangeAccountEnabledResponse changeResp = ChangeAccountEnabledResponse.builder().build();

        TechServicesApiResponse<ChangeAccountEnabledResponse> successResp =
                TechServicesApiResponse.success(changeResp);

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userService.getUserProfileById(disabledById.toString())).thenReturn(Optional.of(disablerProfile));

        when(userService.isInternal(actorId)).thenReturn(true);
        when(techServicesClient.enableUser(any())).thenReturn(successResp);

        userAccountStatusService.enableUser(targetId, actorId);

        // assert user enabled
        assertThat(target.isEnabled()).isTrue();
        assertThat(target.getDisabledBy()).isNull();

        verify(entraUserRepository).saveAndFlush(target);

        ArgumentCaptor<UserAccountStatusAudit> auditCaptor =
                ArgumentCaptor.forClass(UserAccountStatusAudit.class);

        verify(userAccountStatusAuditRepository).saveAndFlush(auditCaptor.capture());

        UserAccountStatusAudit audit = auditCaptor.getValue();

        assertThat(audit.getEntraUser()).isEqualTo(target);
        assertThat(audit.getStatusChange()).isEqualTo(UserAccountStatus.ENABLED);
        assertThat(audit.getStatusChangedBy()).isEqualTo("John Doe");
    }

    @Test
    void shouldEnableUserSuccessfullyEnabledAndDisabledByFirmUserManager() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Firm firm = Firm.builder().id(UUID.randomUUID()).build();

        UserProfile actorProfile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .firm(firm)
                .appRoles(Set.of(
                        AppRole.builder()
                                .name(AuthzRole.FIRM_USER_MANAGER.getRoleName())
                                .build()
                ))
                .build();

        UserProfile targetProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();

        UUID disabledById = UUID.randomUUID();
        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .disabledBy(disabledById)
                .userProfiles(Set.of(targetProfile))
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .firstName("John")
                .lastName("Doe")
                .userProfiles(Set.of(actorProfile))
                .build();

        UserProfileDto disablerProfile = UserProfileDto.builder()
                .id(disabledById)
                .appRoles(List.of(AppRoleDto.builder()
                        .name(AuthzRole.FIRM_USER_MANAGER.getRoleName())
                        .build()))
                .build();

        ChangeAccountEnabledResponse changeResp = ChangeAccountEnabledResponse.builder().build();

        TechServicesApiResponse<ChangeAccountEnabledResponse> successResp =
                TechServicesApiResponse.success(changeResp);

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userService.getUserProfileById(disabledById.toString())).thenReturn(Optional.of(disablerProfile));

        when(userService.isInternal(actorId)).thenReturn(true);
        when(techServicesClient.enableUser(any())).thenReturn(successResp);

        userAccountStatusService.enableUser(targetId, actorId);

        // assert user enabled
        assertThat(target.isEnabled()).isTrue();
        assertThat(target.getDisabledBy()).isNull();

        verify(entraUserRepository).saveAndFlush(target);

        ArgumentCaptor<UserAccountStatusAudit> auditCaptor =
                ArgumentCaptor.forClass(UserAccountStatusAudit.class);

        verify(userAccountStatusAuditRepository).saveAndFlush(auditCaptor.capture());

        UserAccountStatusAudit audit = auditCaptor.getValue();

        assertThat(audit.getEntraUser()).isEqualTo(target);
        assertThat(audit.getStatusChange()).isEqualTo(UserAccountStatus.ENABLED);
        assertThat(audit.getStatusChangedBy()).isEqualTo("John Doe");
    }

    @Test
    void shouldEnableUserFailedEnabledAndDisabledByFirmUserManagerButOfDifferentFirm() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Firm firm1 = Firm.builder().id(UUID.randomUUID()).build();
        Firm firm2 = Firm.builder().id(UUID.randomUUID()).build();

        UserProfile actorProfile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .firm(firm1)
                .appRoles(Set.of(
                        AppRole.builder()
                                .name(AuthzRole.FIRM_USER_MANAGER.getRoleName())
                                .build()
                ))
                .build();

        UserProfile targetProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm2)
                .build();

        UUID disabledById = UUID.randomUUID();
        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .disabledBy(disabledById)
                .userProfiles(Set.of(targetProfile))
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .firstName("John")
                .lastName("Doe")
                .userProfiles(Set.of(actorProfile))
                .build();

        UserProfileDto disablerProfile = UserProfileDto.builder()
                .id(disabledById)
                .appRoles(List.of(AppRoleDto.builder()
                        .name(AuthzRole.FIRM_USER_MANAGER.getRoleName())
                        .build()))
                .build();

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userService.getUserProfileById(disabledById.toString())).thenReturn(Optional.of(disablerProfile));

        when(userService.isInternal(actorId)).thenReturn(false);

        assertThatThrownBy(() -> userAccountStatusService.enableUser(targetId, actorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to enable the user ");
    }

    @Test
    void shouldEnableUserSuccessfullyEnabledByGlobalAdmin() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Firm firm = Firm.builder().id(UUID.randomUUID()).build();

        UserProfile actorProfile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .appRoles(Set.of(
                        AppRole.builder()
                                .name(AuthzRole.GLOBAL_ADMIN.getRoleName())
                                .build()
                ))
                .build();

        UserProfile targetProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();

        UUID disabledById = UUID.randomUUID();
        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .disabledBy(disabledById)
                .userProfiles(Set.of(targetProfile))
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .firstName("John")
                .lastName("Doe")
                .userProfiles(Set.of(actorProfile))
                .build();

        UserProfileDto disablerProfile = UserProfileDto.builder()
                .id(disabledById)
                .appRoles(List.of(AppRoleDto.builder()
                        .name(AuthzRole.FIRM_USER_MANAGER.getRoleName())
                        .build()))
                .build();

        ChangeAccountEnabledResponse changeResp = ChangeAccountEnabledResponse.builder().build();

        TechServicesApiResponse<ChangeAccountEnabledResponse> successResp =
                TechServicesApiResponse.success(changeResp);

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userService.getUserProfileById(disabledById.toString())).thenReturn(Optional.of(disablerProfile));

        when(userService.isInternal(actorId)).thenReturn(true);
        when(techServicesClient.enableUser(any())).thenReturn(successResp);

        userAccountStatusService.enableUser(targetId, actorId);

        // assert user enabled
        assertThat(target.isEnabled()).isTrue();
        assertThat(target.getDisabledBy()).isNull();

        verify(entraUserRepository).saveAndFlush(target);

        ArgumentCaptor<UserAccountStatusAudit> auditCaptor =
                ArgumentCaptor.forClass(UserAccountStatusAudit.class);

        verify(userAccountStatusAuditRepository).saveAndFlush(auditCaptor.capture());

        UserAccountStatusAudit audit = auditCaptor.getValue();

        assertThat(audit.getEntraUser()).isEqualTo(target);
        assertThat(audit.getStatusChange()).isEqualTo(UserAccountStatus.ENABLED);
        assertThat(audit.getStatusChangedBy()).isEqualTo("John Doe");
    }

    @Test
    void shouldEnableMultiFirmUserSuccessfullyEnabledByGlobalAdmin() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Firm firm = Firm.builder().id(UUID.randomUUID()).build();

        UserProfile actorProfile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .appRoles(Set.of(
                        AppRole.builder()
                                .name(AuthzRole.GLOBAL_ADMIN.getRoleName())
                                .build()
                ))
                .build();

        UserProfile targetProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();

        UUID disabledById = UUID.randomUUID();
        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .disabledBy(disabledById)
                .userProfiles(Set.of(targetProfile))
                .multiFirmUser(true)
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .firstName("John")
                .lastName("Doe")
                .userProfiles(Set.of(actorProfile))
                .build();

        UserProfileDto disablerProfile = UserProfileDto.builder()
                .id(disabledById)
                .appRoles(List.of(AppRoleDto.builder()
                        .name(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())
                        .build()))
                .build();

        ChangeAccountEnabledResponse changeResp = ChangeAccountEnabledResponse.builder().build();

        TechServicesApiResponse<ChangeAccountEnabledResponse> successResp =
                TechServicesApiResponse.success(changeResp);

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(userService.getUserProfileById(disabledById.toString())).thenReturn(Optional.of(disablerProfile));

        when(userService.isInternal(actorId)).thenReturn(true);
        when(techServicesClient.enableUser(any())).thenReturn(successResp);

        userAccountStatusService.enableUser(targetId, actorId);

        // assert user enabled
        assertThat(target.isEnabled()).isTrue();
        assertThat(target.getDisabledBy()).isNull();

        verify(entraUserRepository).saveAndFlush(target);

        ArgumentCaptor<UserAccountStatusAudit> auditCaptor =
                ArgumentCaptor.forClass(UserAccountStatusAudit.class);

        verify(userAccountStatusAuditRepository).saveAndFlush(auditCaptor.capture());

        UserAccountStatusAudit audit = auditCaptor.getValue();

        assertThat(audit.getEntraUser()).isEqualTo(target);
        assertThat(audit.getStatusChange()).isEqualTo(UserAccountStatus.ENABLED);
        assertThat(audit.getStatusChangedBy()).isEqualTo("John Doe");
    }

    @Test
    void globalAdminCanEnableUserDisabledByUnknown() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Firm firm = Firm.builder().id(UUID.randomUUID()).build();

        UserProfile actorProfile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .appRoles(Set.of(
                        AppRole.builder()
                                .name(AuthzRole.GLOBAL_ADMIN.getRoleName())
                                .build()
                ))
                .build();

        UserProfile targetProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();

        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .userProfiles(Set.of(targetProfile))
                .multiFirmUser(true)
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .firstName("John")
                .lastName("Doe")
                .userProfiles(Set.of(actorProfile))
                .build();


        ChangeAccountEnabledResponse changeResp = ChangeAccountEnabledResponse.builder().build();

        TechServicesApiResponse<ChangeAccountEnabledResponse> successResp =
                TechServicesApiResponse.success(changeResp);

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));

        when(userService.isInternal(actorId)).thenReturn(true);
        when(techServicesClient.enableUser(any())).thenReturn(successResp);

        userAccountStatusService.enableUser(targetId, actorId);

        // assert user enabled
        assertThat(target.isEnabled()).isTrue();
        assertThat(target.getDisabledBy()).isNull();

        verify(entraUserRepository).saveAndFlush(target);

        ArgumentCaptor<UserAccountStatusAudit> auditCaptor =
                ArgumentCaptor.forClass(UserAccountStatusAudit.class);

        verify(userAccountStatusAuditRepository).saveAndFlush(auditCaptor.capture());

        UserAccountStatusAudit audit = auditCaptor.getValue();

        assertThat(audit.getEntraUser()).isEqualTo(target);
        assertThat(audit.getStatusChange()).isEqualTo(UserAccountStatus.ENABLED);
        assertThat(audit.getStatusChangedBy()).isEqualTo("John Doe");
    }

    @Test
    void firmUserManagerCannotEnableUserDisabledByUnknown() {
        UUID targetId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID profileId = UUID.randomUUID();

        Firm firm = Firm.builder().id(UUID.randomUUID()).build();

        UserProfile actorProfile = UserProfile.builder()
                .id(profileId)
                .activeProfile(true)
                .appRoles(Set.of(
                        AppRole.builder()
                                .name(AuthzRole.FIRM_USER_MANAGER.getRoleName())
                                .build()
                ))
                .build();

        UserProfile targetProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .activeProfile(true)
                .firm(firm)
                .build();

        EntraUser target = EntraUser.builder()
                .id(targetId)
                .enabled(false)
                .userProfiles(Set.of(targetProfile))
                .multiFirmUser(true)
                .build();

        EntraUser actor = EntraUser.builder()
                .id(actorId)
                .firstName("John")
                .lastName("Doe")
                .userProfiles(Set.of(actorProfile))
                .build();

        when(entraUserRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(entraUserRepository.findById(actorId)).thenReturn(Optional.of(actor));

        when(userService.isInternal(actorId)).thenReturn(true);

        assertThatThrownBy(() -> userAccountStatusService.enableUser(targetId, actorId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to enable the user ");
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


            Map<String, Integer> result = userAccountStatusService.getUserCountsForFirm(String.valueOf(firmId));


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

            Map<String, Integer> result = userAccountStatusService.getUserCountsForFirm(String.valueOf(firmId));

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

            Map<String, Integer> result = userAccountStatusService.getUserCountsForFirm(String.valueOf(firmId));

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
