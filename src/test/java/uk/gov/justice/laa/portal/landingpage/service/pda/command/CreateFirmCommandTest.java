package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

@ExtendWith(MockitoExtension.class)
class CreateFirmCommandTest {

    @Mock
    private FirmRepository firmRepository;

    private PdaSyncResultDto result;

    @BeforeEach
    void setUp() {
        result = PdaSyncResultDto.builder().build();
    }

    @Nested
    class FirmCreationTests {

        @Test
        void shouldCreateFirmSuccessfully() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .build();

            when(firmRepository.findFirmByName("Test Firm")).thenReturn(null);

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            ArgumentCaptor<Firm> firmCaptor = ArgumentCaptor.forClass(Firm.class);
            verify(firmRepository).save(firmCaptor.capture());

            Firm savedFirm = firmCaptor.getValue();
            assertThat(savedFirm.getCode()).isEqualTo("12345");
            assertThat(savedFirm.getName()).isEqualTo("Test Firm");
            assertThat(savedFirm.getType()).isEqualTo(FirmType.LEGAL_SERVICES_PROVIDER);
            assertThat(savedFirm.getParentFirm()).isNull(); // Parent not set in creation
            assertThat(result.getFirmsCreated()).isEqualTo(1);
        }

        @Test
        void shouldCreateAdvocateFirm() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("ADV123")
                .firmName("Advocate Firm")
                .firmType("Advocate")
                .build();

            when(firmRepository.findFirmByName("Advocate Firm")).thenReturn(null);

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            ArgumentCaptor<Firm> firmCaptor = ArgumentCaptor.forClass(Firm.class);
            verify(firmRepository).save(firmCaptor.capture());

            Firm savedFirm = firmCaptor.getValue();
            assertThat(savedFirm.getType()).isEqualTo(FirmType.ADVOCATE);
        }

        @Test
        void shouldHandleFirmTypeWithSpaces() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Chambers")
                .build();

            when(firmRepository.findFirmByName("Test Firm")).thenReturn(null);

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            ArgumentCaptor<Firm> firmCaptor = ArgumentCaptor.forClass(Firm.class);
            verify(firmRepository).save(firmCaptor.capture());

            Firm savedFirm = firmCaptor.getValue();
            assertThat(savedFirm.getType()).isEqualTo(FirmType.CHAMBERS);
        }
    }

    @Nested
    class DuplicateNameHandlingTests {

        @Test
        void shouldAppendFirmCodeWhenDuplicateNameExists() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("OLD123")
                .name("Duplicate Name")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("NEW456")
                .firmName("Duplicate Name")
                .firmType("Legal Services Provider")
                .build();

            when(firmRepository.findFirmByName("Duplicate Name")).thenReturn(existingFirm);

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            ArgumentCaptor<Firm> firmCaptor = ArgumentCaptor.forClass(Firm.class);
            verify(firmRepository).save(firmCaptor.capture());

            Firm savedFirm = firmCaptor.getValue();
            assertThat(savedFirm.getName()).isEqualTo("Duplicate Name (NEW456)");
            assertThat(result.getFirmsCreated()).isEqualTo(1);
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("Duplicate firm name");
            assertThat(result.getWarnings().get(0)).contains("appended firm code");
        }

        @Test
        void shouldNotModifyNameWhenNoDuplicate() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Unique Name")
                .firmType("Legal Services Provider")
                .build();

            when(firmRepository.findFirmByName("Unique Name")).thenReturn(null);

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            ArgumentCaptor<Firm> firmCaptor = ArgumentCaptor.forClass(Firm.class);
            verify(firmRepository).save(firmCaptor.capture());

            Firm savedFirm = firmCaptor.getValue();
            assertThat(savedFirm.getName()).isEqualTo("Unique Name");
            assertThat(result.getWarnings()).isEmpty();
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void shouldHandleDataIntegrityViolation() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .build();

            when(firmRepository.findFirmByName("Test Firm")).thenReturn(null);
            when(firmRepository.save(any(Firm.class)))
                .thenThrow(new DataIntegrityViolationException("Constraint violation"));

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsCreated()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).containsAnyOf("Data integrity violation", "Failed to create firm");
            assertThat(result.getErrors().get(0)).contains("12345");
        }

        @Test
        void shouldHandleGenericException() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .build();

            when(firmRepository.findFirmByName("Test Firm")).thenReturn(null);
            when(firmRepository.save(any(Firm.class)))
                .thenThrow(new RuntimeException("Database error"));

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsCreated()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Failed to create firm");
            assertThat(result.getErrors().get(0)).contains("12345");
        }
    }

    @Nested
    class ParentFirmHandlingTests {

        @Test
        void shouldNotSetParentFirmDuringCreation() {
            // Given - PDA data includes parent firm number
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Child Firm")
                .firmType("Legal Services Provider")
                .parentFirmNumber("PARENT123") // Parent specified
                .build();

            when(firmRepository.findFirmByName("Child Firm")).thenReturn(null);

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            ArgumentCaptor<Firm> firmCaptor = ArgumentCaptor.forClass(Firm.class);
            verify(firmRepository).save(firmCaptor.capture());

            Firm savedFirm = firmCaptor.getValue();
            // Parent should NOT be set during creation (handled in second pass)
            assertThat(savedFirm.getParentFirm()).isNull();
        }
    }

    @Nested
    class FirmTypeValidationTests {

        @Test
        void shouldRejectEmptyFirmType() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("") // Empty firmType
                .build();

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsCreated()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("firmType is empty or null");
            verify(firmRepository, never()).save(any(Firm.class));
        }

        @Test
        void shouldRejectNullFirmType() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType(null) // Null firmType
                .build();

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsCreated()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("firmType is empty or null");
            verify(firmRepository, never()).save(any(Firm.class));
        }

        @Test
        void shouldRejectWhitespaceFirmType() {
            // Given
            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("   ") // Whitespace only
                .build();

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsCreated()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("firmType is empty or null");
            verify(firmRepository, never()).save(any(Firm.class));
        }
    }

    @Nested
    class DuplicateCodeValidationTests {

        @Test
        void shouldRejectDuplicateCode() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("DUPLICATE")
                .name("Existing Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("DUPLICATE")
                .firmName("New Firm")
                .firmType("Legal Services Provider")
                .build();

            when(firmRepository.findByCode("DUPLICATE")).thenReturn(existingFirm);

            CreateFirmCommand command = new CreateFirmCommand(firmRepository, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsCreated()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("code already exists");
            assertThat(result.getErrors().get(0)).contains("DUPLICATE");
            verify(firmRepository, never()).save(any(Firm.class));
        }
    }
}
