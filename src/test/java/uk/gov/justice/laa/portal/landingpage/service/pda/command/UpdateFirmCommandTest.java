package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.PdaFirmData;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

@ExtendWith(MockitoExtension.class)
class UpdateFirmCommandTest {

    @Mock
    private FirmRepository firmRepository;

    private PdaSyncResultDto result;

    @BeforeEach
    void setUp() {
        result = PdaSyncResultDto.builder().build();
    }

    @Nested
    class FirmNameUpdateTests {

        @Test
        void shouldUpdateFirmNameWhenChanged() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Old Name")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("New Name")
                .firmType("Legal Services Provider")
                .build();

            when(firmRepository.findFirmByName("New Name")).thenReturn(null);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getName()).isEqualTo("New Name");
            assertThat(result.getFirmsUpdated()).isEqualTo(1);
            verify(firmRepository).save(existingFirm);
        }

        @Test
        void shouldNotUpdateWhenNameUnchanged() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Same Name")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Same Name")
                .firmType("Legal Services Provider")
                .build();

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsUpdated()).isEqualTo(0);
            verify(firmRepository, never()).save(any());
        }

        @Test
        void shouldSkipNameUpdateWhenDuplicateNameExists() {
            // Given
            UUID firmId = UUID.randomUUID();
            Firm existingFirm = Firm.builder()
                .id(firmId)
                .code("12345")
                .name("Old Name")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            Firm otherFirmWithSameName = Firm.builder()
                .id(UUID.randomUUID())
                .code("67890")
                .name("Duplicate Name")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Duplicate Name")
                .firmType("Legal Services Provider")
                .build();

            when(firmRepository.findFirmByName("Duplicate Name")).thenReturn(otherFirmWithSameName);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getName()).isEqualTo("Old Name"); // Name should not change
            assertThat(result.getFirmsUpdated()).isEqualTo(0);
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("Duplicate firm name");
            verify(firmRepository, never()).save(any());
        }
    }

    @Nested
    class FirmTypeValidationTests {

        @Test
        void shouldRejectFirmTypeChange() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Advocate") // Different type
                .build();

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getType()).isEqualTo(FirmType.LEGAL_SERVICES_PROVIDER); // Type should not change
            assertThat(result.getFirmsUpdated()).isEqualTo(0);
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("CRITICAL");
            assertThat(result.getWarnings().get(0)).contains("type change rejected");
            verify(firmRepository, never()).save(any());
        }
    }

    @Nested
    class ParentFirmUpdateTests {

        @Test
        void shouldUpdateParentFirmWhenChanged() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(null)
                .build();

            Firm parentFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("PARENT")
                .name("Parent Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .parentFirmNumber("PARENT")
                .build();

            when(firmRepository.findByCode("PARENT")).thenReturn(parentFirm);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isEqualTo(parentFirm);
            assertThat(result.getFirmsUpdated()).isEqualTo(1);
            verify(firmRepository).save(existingFirm);
        }

        @Test
        void shouldHandleNullParentFirmNumber() {
            // Given
            Firm parentFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("PARENT")
                .name("Parent Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(parentFirm)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .parentFirmNumber(null)
                .build();

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(1);
            verify(firmRepository).save(existingFirm);
        }

        @Test
        void shouldHandleStringNullAsParentFirmNumber() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(null)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .parentFirmNumber("null") // String "null"
                .build();

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(0); // No change
            verify(firmRepository, never()).save(any());
        }

        @Test
        void shouldRejectAdvocateAsParentFirm() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(null)
                .build();

            Firm advocateParent = Firm.builder()
                .id(UUID.randomUUID())
                .code("ADVOCATE")
                .name("Advocate Firm")
                .type(FirmType.ADVOCATE)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .parentFirmNumber("ADVOCATE")
                .build();

            when(firmRepository.findByCode("ADVOCATE")).thenReturn(advocateParent);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(0); // Already null
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("ADVOCATE");
            assertThat(result.getWarnings().get(0)).contains("cannot be a parent");
            verify(firmRepository, never()).save(any());
        }

        @Test
        void shouldRejectMultiLevelHierarchy() {
            // Given
            Firm grandParent = Firm.builder()
                .id(UUID.randomUUID())
                .code("GRANDPARENT")
                .name("Grand Parent Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            Firm parentWithParent = Firm.builder()
                .id(UUID.randomUUID())
                .code("PARENT")
                .name("Parent Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(grandParent)
                .build();

            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(null)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .parentFirmNumber("PARENT")
                .build();

            when(firmRepository.findByCode("PARENT")).thenReturn(parentWithParent);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(0);
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("multi-level hierarchy not allowed");
            verify(firmRepository, never()).save(any());
        }

        @Test
        void shouldHandleMissingParentFirm() {
            // Given
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(null)
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("12345")
                .firmName("Test Firm")
                .firmType("Legal Services Provider")
                .parentFirmNumber("MISSING")
                .build();

            when(firmRepository.findByCode("MISSING")).thenReturn(null);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(0);
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("not found");
            verify(firmRepository, never()).save(any());
        }
    }
}
