package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * Tests for UpdateOfficeCommand.
 */
@ExtendWith(MockitoExtension.class)
class UpdateOfficeCommandTest {

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Captor
    private ArgumentCaptor<Office> officeCaptor;

    private Firm originalFirm;
    private Firm newFirm;
    private Office office;
    private PdaOfficeData pdaOffice;
    private PdaSyncResultDto result;

    @BeforeEach
    void setUp() {
        originalFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Original Firm")
                .build();

        newFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("67890")
                .name("New Firm")
                .build();

        office = Office.builder()
                .id(UUID.randomUUID())
                .code("12345-001")
                .firm(originalFirm)
                .address(Office.Address.builder()
                        .addressLine1("Old Street")
                        .city("Old City")
                        .postcode("OLD 123")
                        .build())
                .build();

        pdaOffice = PdaOfficeData.builder()
                .officeAccountNo("12345-001")
                .firmNumber("12345")
                .addressLine1("New Street")
                .addressLine2("Suite 200")
                .city("New City")
                .postcode("NEW 456")
                .build();

        result = PdaSyncResultDto.builder()
                .officesUpdated(0)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();
    }

    @Test
    void shouldUpdateOfficeAddress() {
        // Given
        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(officeCaptor.capture());
        Office updatedOffice = officeCaptor.getValue();

        assertThat(updatedOffice.getAddress().getAddressLine1()).isEqualTo("New Street");
        assertThat(updatedOffice.getAddress().getAddressLine2()).isEqualTo("Suite 200");
        assertThat(updatedOffice.getAddress().getCity()).isEqualTo("New City");
        assertThat(updatedOffice.getAddress().getPostcode()).isEqualTo("NEW 456");
        assertThat(result.getOfficesUpdated()).isEqualTo(1);
    }

    @Test
    void shouldNotUpdateIfNoChanges() {
        // Given
        office.getAddress().setAddressLine1("New Street");
        office.getAddress().setAddressLine2("Suite 200");
        office.getAddress().setCity("New City");
        office.getAddress().setPostcode("NEW 456");

        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository, never()).save(any());
        assertThat(result.getOfficesUpdated()).isEqualTo(0);
    }

    @Test
    void shouldRemoveUserAssociationsWhenFirmChanges() {
        // Given
        UserProfile profile1 = UserProfile.builder()
                .id(UUID.randomUUID())
                .offices(new HashSet<>(Set.of(office)))
                .build();
        UserProfile profile2 = UserProfile.builder()
                .id(UUID.randomUUID())
                .offices(new HashSet<>(Set.of(office)))
                .build();

        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(List.of(profile1, profile2));

        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, newFirm);

        // When
        command.execute(result);

        // Then
        verify(userProfileRepository, times(2)).save(any(UserProfile.class));
        assertThat(office.getFirm()).isEqualTo(newFirm);
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("switched firms");
        assertThat(result.getWarnings().get(0)).contains("2 user association(s)");
    }

    @Test
    void shouldHandleFirmChangeWithNoUserAssociations() {
        // Given
        when(userProfileRepository.findByOfficeId(office.getId()))
                .thenReturn(Collections.emptyList());

        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, newFirm);

        // When
        command.execute(result);

        // Then
        assertThat(office.getFirm()).isEqualTo(newFirm);
        assertThat(result.getWarnings()).isEmpty();
        verify(officeRepository).save(office);
    }

    @Test
    void shouldConvertEmptyStringsToNull() {
        // Given
        pdaOffice = PdaOfficeData.builder()
                .officeAccountNo("12345-001")
                .firmNumber("12345")
                .addressLine1("")
                .addressLine2("   ")
                .city("New City")
                .build();

        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(officeCaptor.capture());
        Office updatedOffice = officeCaptor.getValue();

        assertThat(updatedOffice.getAddress().getAddressLine1()).isNull();
        assertThat(updatedOffice.getAddress().getAddressLine2()).isNull();
    }

    @Test
    void shouldHandleNullAddressOnOffice() {
        // Given
        office.setAddress(null);

        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(officeCaptor.capture());
        Office updatedOffice = officeCaptor.getValue();

        assertThat(updatedOffice.getAddress()).isNotNull();
        assertThat(updatedOffice.getAddress().getAddressLine1()).isEqualTo("New Street");
    }

    @Test
    void shouldHandleDeletedInstanceException() {
        // Given
        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        InvalidDataAccessApiUsageException exception =
                new InvalidDataAccessApiUsageException("deleted instance passed to merge");
        when(officeRepository.save(any())).thenThrow(exception);

        // When
        command.execute(result);

        // Then
        assertThat(result.getErrors()).isEmpty();
        assertThat(result.getOfficesUpdated()).isEqualTo(0);
    }

    @Test
    void shouldHandleOtherExceptions() {
        // Given
        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        when(officeRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // When
        command.execute(result);

        // Then
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Failed to update office");
    }

    @Test
    void shouldUpdateOnlyChangedFields() {
        // Given
        office.getAddress().setAddressLine1("New Street");
        office.getAddress().setCity("New City");
        // addressLine2 and postcode are different

        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(any());
        assertThat(result.getOfficesUpdated()).isEqualTo(1);
    }

    @Test
    void shouldHandlePartialAddressUpdate() {
        // Given
        pdaOffice = PdaOfficeData.builder()
                .officeAccountNo("12345-001")
                .firmNumber("12345")
                .addressLine1("Old Street") // Same
                .city("New City") // Different
                .postcode("OLD 123") // Same
                .build();

        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(any());
        assertThat(result.getOfficesUpdated()).isEqualTo(1);
    }

    @Test
    void shouldIncrementOfficesUpdatedCounter() {
        // Given
        result = PdaSyncResultDto.builder()
                .officesUpdated(10)
                .errors(new ArrayList<>())
                .warnings(new ArrayList<>())
                .build();

        UpdateOfficeCommand command = new UpdateOfficeCommand(
                officeRepository, userProfileRepository, office, pdaOffice, originalFirm);

        // When
        command.execute(result);

        // Then
        assertThat(result.getOfficesUpdated()).isEqualTo(11);
    }
}
