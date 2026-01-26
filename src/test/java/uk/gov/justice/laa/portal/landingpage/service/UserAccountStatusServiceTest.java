package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.dto.DisableUserReasonDto;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.exception.TechServicesClientException;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserAuditRepository;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserReasonRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.ChangeAccountEnabledResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;

import java.util.List;
import java.util.Optional;
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
    private DisableUserAuditRepository disableUserAuditRepository;
    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private TechServicesClient techServicesClient;

    @InjectMocks
    private UserAccountStatusService userAccountStatusService;

    @BeforeEach
    public void setup() {
        ModelMapper mapper = new MapperConfig().modelMapper();
        userAccountStatusService = new UserAccountStatusService(disableUserReasonRepository, disableUserAuditRepository, mapper, entraUserRepository, techServicesClient);
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
        List<DisableUserReasonDto> returnedReasons = userAccountStatusService.getDisableUserReasons();

        // Then
        assertThat(returnedReasons).isNotEmpty();
        DisableUserReasonDto returnedReason = returnedReasons.getFirst();
        assertThat(returnedReason.getId()).isEqualTo(reason.getId());
        assertThat(returnedReason.getName()).isEqualTo(reason.getName());
        assertThat(returnedReason.getDescription()).isEqualTo(reason.getDescription());
    }

    @Test
    public void testDisableUserDisabledUserWhenTechServicesRequestIsSuccess() {
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
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

        assertThat(disabledUser.isDisabled()).isTrue();
        verify(entraUserRepository, times(1)).saveAndFlush(any());
        verify(disableUserAuditRepository, times(1)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionWhenTechServicesRequestIsError() {
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
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
        assertThat(disabledUser.isDisabled()).isFalse();
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(disableUserAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionWhenDisabledUserNotFound() {
        assertThrows(RuntimeException.class, () -> userAccountStatusService.disableUser(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));
        verify(entraUserRepository, times(1)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(disableUserAuditRepository, times(0)).saveAndFlush(any());
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
        assertThat(disabledUser.isDisabled()).isFalse();
        verify(entraUserRepository, times(2)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(disableUserAuditRepository, times(0)).saveAndFlush(any());
    }

    @Test
    public void testDisableUserThrowsExceptionDisableReasonNotFound() {
        EntraUser disabledUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("Disabled")
                .lastName("User")
                .build();
        EntraUser disabledByUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .firstName("DisabledBy")
                .lastName("User")
                .build();

        when(entraUserRepository.findById(eq(disabledUser.getId()))).thenReturn(Optional.of(disabledUser));
        when(entraUserRepository.findById(eq(disabledByUser.getId()))).thenReturn(Optional.of(disabledByUser));

        assertThrows(RuntimeException.class, () -> userAccountStatusService.disableUser(disabledUser.getId(), UUID.randomUUID(), disabledByUser.getId()));
        assertThat(disabledUser.isDisabled()).isFalse();
        verify(entraUserRepository, times(2)).findById(any());
        verify(disableUserReasonRepository, times(1)).findById(any());
        verify(entraUserRepository, times(0)).saveAndFlush(any());
        verify(disableUserAuditRepository, times(0)).saveAndFlush(any());
    }
}
