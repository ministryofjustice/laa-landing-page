package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.PdaOfficeData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

/**
 * Tests for CreateOfficeCommand.
 */
@ExtendWith(MockitoExtension.class)
class CreateOfficeCommandTest {

    @Mock
    private OfficeRepository officeRepository;

    @Captor
    private ArgumentCaptor<Office> officeCaptor;

    private Firm firm;
    private PdaOfficeData pdaOffice;
    private PdaSyncResultDto result;

    @BeforeEach
    void setUp() {
        firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .build();

        pdaOffice = PdaOfficeData.builder()
                .officeAccountNo("12345-67890")
                .firmNumber("12345")
                .addressLine1("123 Test Street")
                .addressLine2("Suite 100")
                .addressLine3("Building A")
                .city("London")
                .postcode("SW1A 1AA")
                .build();

        result = PdaSyncResultDto.builder()
                .officesCreated(0)
                .errors(new ArrayList<>())
                .build();
    }

    @Test
    void shouldCreateOfficeWithAllAddressFields() {
        // Given
        CreateOfficeCommand command = new CreateOfficeCommand(officeRepository, pdaOffice, firm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(officeCaptor.capture());
        Office savedOffice = officeCaptor.getValue();

        assertThat(savedOffice.getCode()).isEqualTo("12345-67890");
        assertThat(savedOffice.getFirm()).isEqualTo(firm);
        assertThat(savedOffice.getAddress().getAddressLine1()).isEqualTo("123 Test Street");
        assertThat(savedOffice.getAddress().getAddressLine2()).isEqualTo("Suite 100");
        assertThat(savedOffice.getAddress().getAddressLine3()).isEqualTo("Building A");
        assertThat(savedOffice.getAddress().getCity()).isEqualTo("London");
        assertThat(savedOffice.getAddress().getPostcode()).isEqualTo("SW1A 1AA");

        assertThat(result.getOfficesCreated()).isEqualTo(1);
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldCreateOfficeWithMinimalAddressFields() {
        // Given
        pdaOffice = PdaOfficeData.builder()
                .officeAccountNo("12345-67890")
                .firmNumber("12345")
                .addressLine1("123 Test Street")
                .city("London")
                .build();

        CreateOfficeCommand command = new CreateOfficeCommand(officeRepository, pdaOffice, firm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(officeCaptor.capture());
        Office savedOffice = officeCaptor.getValue();

        assertThat(savedOffice.getAddress().getAddressLine1()).isEqualTo("123 Test Street");
        assertThat(savedOffice.getAddress().getAddressLine2()).isNull();
        assertThat(savedOffice.getAddress().getAddressLine3()).isNull();
        assertThat(savedOffice.getAddress().getCity()).isEqualTo("London");
        assertThat(savedOffice.getAddress().getPostcode()).isNull();
    }

    @Test
    void shouldConvertEmptyStringsToNull() {
        // Given
        pdaOffice = PdaOfficeData.builder()
                .officeAccountNo("12345-67890")
                .firmNumber("12345")
                .addressLine1("123 Test Street")
                .addressLine2("")
                .addressLine3("   ")
                .city("London")
                .postcode("")
                .build();

        CreateOfficeCommand command = new CreateOfficeCommand(officeRepository, pdaOffice, firm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(officeCaptor.capture());
        Office savedOffice = officeCaptor.getValue();

        assertThat(savedOffice.getAddress().getAddressLine2()).isNull();
        assertThat(savedOffice.getAddress().getAddressLine3()).isNull();
        assertThat(savedOffice.getAddress().getPostcode()).isNull();
    }

    @Test
    void shouldHandleExceptionDuringCreation() {
        // Given
        CreateOfficeCommand command = new CreateOfficeCommand(officeRepository, pdaOffice, firm);
        when(officeRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // When
        command.execute(result);

        // Then
        assertThat(result.getOfficesCreated()).isEqualTo(0);
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0)).contains("Failed to create office");
        assertThat(result.getErrors().get(0)).contains("12345-67890");
    }

    @Test
    void shouldCreateOfficeWithNullAddress() {
        // Given
        pdaOffice = PdaOfficeData.builder()
                .officeAccountNo("12345-67890")
                .firmNumber("12345")
                .build();

        CreateOfficeCommand command = new CreateOfficeCommand(officeRepository, pdaOffice, firm);

        // When
        command.execute(result);

        // Then
        verify(officeRepository).save(officeCaptor.capture());
        Office savedOffice = officeCaptor.getValue();

        assertThat(savedOffice.getCode()).isEqualTo("12345-67890");
        assertThat(savedOffice.getAddress().getAddressLine1()).isNull();
        assertThat(savedOffice.getAddress().getCity()).isNull();
    }

    @Test
    void shouldIncrementOfficesCreatedCounter() {
        // Given
        result = PdaSyncResultDto.builder()
                .officesCreated(5)
                .errors(new ArrayList<>())
                .build();

        CreateOfficeCommand command = new CreateOfficeCommand(officeRepository, pdaOffice, firm);

        // When
        command.execute(result);

        // Then
        assertThat(result.getOfficesCreated()).isEqualTo(6);
    }
}
