package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

@ExtendWith(MockitoExtension.class)
class DeactivateFirmCommandTest {

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    private PdaSyncResultDto result;

    @BeforeEach
    void setUp() {
        result = PdaSyncResultDto.builder().build();
    }

    @Nested
    class UserProfileHandlingTests {

        @Test
        void shouldDeleteExternalUserProfilesWhenDeactivatingFirm() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            UserProfile externalProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userType(UserType.EXTERNAL)
                .firm(firm)
                .build();

            when(userProfileRepository.findByFirmId(firm.getId()))
                .thenReturn(Collections.singletonList(externalProfile));
            when(officeRepository.findByFirm(firm)).thenReturn(Collections.emptyList());

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            verify(userProfileRepository).delete(externalProfile);
            verify(firmRepository).delete(firm);
        }

        @Test
        void shouldClearFirmFromInternalUserProfiles() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            UserProfile internalProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userType(UserType.INTERNAL)
                .firm(firm)
                .build();

            when(userProfileRepository.findByFirmId(firm.getId()))
                .thenReturn(Collections.singletonList(internalProfile));
            when(officeRepository.findByFirm(firm)).thenReturn(Collections.emptyList());

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            verify(userProfileRepository, never()).delete(internalProfile);
            verify(userProfileRepository).save(internalProfile);
            assertThat(internalProfile.getFirm()).isNull();
            verify(firmRepository).delete(firm);
        }

        @Test
        void shouldHandleMixedUserProfiles() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            UserProfile externalProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userType(UserType.EXTERNAL)
                .firm(firm)
                .build();

            UserProfile internalProfile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userType(UserType.INTERNAL)
                .firm(firm)
                .build();

            when(userProfileRepository.findByFirmId(firm.getId()))
                .thenReturn(Arrays.asList(externalProfile, internalProfile));
            when(officeRepository.findByFirm(firm)).thenReturn(Collections.emptyList());

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            verify(userProfileRepository).delete(externalProfile);
            verify(userProfileRepository).save(internalProfile);
            assertThat(internalProfile.getFirm()).isNull();
        }
    }

    @Nested
    class OfficeHandlingTests {

        @Test
        void shouldDeleteOfficesBeforeDeletingFirm() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            Office office1 = Office.builder()
                .id(UUID.randomUUID())
                .code("12345-1")
                .firm(firm)
                .address(Office.Address.builder().addressLine1("Office 1").build())
                .build();

            Office office2 = Office.builder()
                .id(UUID.randomUUID())
                .code("12345-2")
                .firm(firm)
                .address(Office.Address.builder().addressLine1("Office 2").build())
                .build();

            when(userProfileRepository.findByFirmId(firm.getId())).thenReturn(Collections.emptyList());
            when(officeRepository.findByFirm(firm)).thenReturn(Arrays.asList(office1, office2));
            when(userProfileRepository.findByOfficeId(any())).thenReturn(Collections.emptyList());

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            InOrder inOrder = inOrder(officeRepository, firmRepository);
            inOrder.verify(officeRepository).delete(office1);
            inOrder.verify(officeRepository).delete(office2);
            inOrder.verify(firmRepository).delete(firm);

            assertThat(result.getOfficesDeleted()).isEqualTo(2);
        }

        @Test
        void shouldRemoveOfficeAssociationsBeforeDeletingOffice() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            Office office = Office.builder()
                .id(UUID.randomUUID())
                .code("12345-1")
                .firm(firm)
                .address(Office.Address.builder().addressLine1("Office 1").build())
                .build();

            Set<Office> offices = new HashSet<>();
            offices.add(office);

            UserProfile profileWithOffice = UserProfile.builder()
                .id(UUID.randomUUID())
                .userType(UserType.EXTERNAL)
                .firm(firm)
                .offices(offices)
                .build();

            when(userProfileRepository.findByFirmId(firm.getId())).thenReturn(Collections.emptyList());
            when(officeRepository.findByFirm(firm)).thenReturn(Collections.singletonList(office));
            when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(Collections.singletonList(profileWithOffice));

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            verify(userProfileRepository).save(profileWithOffice);
            assertThat(profileWithOffice.getOffices()).isEmpty();
            verify(officeRepository).delete(office);
        }
    }

    @Nested
    class FirmDeletionTests {

        @Test
        void shouldDeleteFirmWithNoAssociations() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            when(userProfileRepository.findByFirmId(firm.getId())).thenReturn(Collections.emptyList());
            when(officeRepository.findByFirm(firm)).thenReturn(Collections.emptyList());

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            verify(firmRepository).delete(firm);
            assertThat(result.getFirmsDeleted()).isEqualTo(1);
        }

        @Test
        void shouldIncrementDeactivationCounter() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            when(userProfileRepository.findByFirmId(firm.getId())).thenReturn(Collections.emptyList());
            when(officeRepository.findByFirm(firm)).thenReturn(Collections.emptyList());

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsDeleted()).isEqualTo(1);
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void shouldCaptureExceptionsDuringDeactivation() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            when(userProfileRepository.findByFirmId(firm.getId())).thenReturn(Collections.emptyList());
            when(officeRepository.findByFirm(firm)).thenReturn(Collections.emptyList());
            doThrow(new RuntimeException("Database error")).when(firmRepository).delete(firm);

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsDeleted()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Failed to deactivate firm");
            assertThat(result.getErrors().get(0)).contains("12345");
        }
    }

    @Nested
    class OrderingTests {

        @Test
        void shouldHandleUserProfilesBeforeOffices() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .userType(UserType.EXTERNAL)
                .firm(firm)
                .build();

            Office office = Office.builder()
                .id(UUID.randomUUID())
                .code("12345-1")
                .firm(firm)
                .address(Office.Address.builder().addressLine1("Office 1").build())
                .build();

            when(userProfileRepository.findByFirmId(firm.getId()))
                .thenReturn(Collections.singletonList(profile));
            when(officeRepository.findByFirm(firm)).thenReturn(Collections.singletonList(office));
            when(userProfileRepository.findByOfficeId(office.getId())).thenReturn(Collections.emptyList());

            DeactivateFirmCommand command = new DeactivateFirmCommand(
                firmRepository, officeRepository, userProfileRepository, firm);

            // When
            command.execute(result);

            // Then
            InOrder inOrder = inOrder(userProfileRepository, officeRepository, firmRepository);
            inOrder.verify(userProfileRepository).delete(profile); // User profiles first
            inOrder.verify(officeRepository).delete(office); // Then offices
            inOrder.verify(firmRepository).delete(firm); // Finally firm
        }
    }
}
