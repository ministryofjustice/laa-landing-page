package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.HashMap;
import java.util.Map;
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
    private Map<String, Firm> firmsByCode;

    @BeforeEach
    void setUp() {
        result = PdaSyncResultDto.builder().build();
        firmsByCode = new HashMap<>();
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

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

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

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

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

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

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

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

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

            firmsByCode.put("PARENT", parentFirm);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

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

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

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

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

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

            firmsByCode.put("ADVOCATE", advocateParent);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(0); // No update due to validation
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

            firmsByCode.put("PARENT", parentWithParent);
            firmsByCode.put("GRANDPARENT", grandParent);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

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

            // Don't add MISSING to firmsByCode map

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(0);
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result.getWarnings().get(0)).contains("not found");
            verify(firmRepository, never()).save(any());
        }

        @Test
        void shouldPreventInfiniteUpdateLoopWhenParentNotFound() {
            // Given - This scenario was causing infinite update loops
            // Firm has null parent in DB, PDA says parent is "261367", but 261367 doesn't exist
            Firm existingFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("49058")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(null) // Current state: null
                .build();

            PdaFirmData pdaFirm = PdaFirmData.builder()
                .firmNumber("49058")
                .firmName("Test Firm")
                .firmType("Advocate")
                .parentFirmNumber("261367") // Ghost parent - doesn't exist
                .build();

            // Don't add 261367 to firmsByCode (it doesn't exist)

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

            // When - execute twice to simulate subsequent syncs
            command.execute(result);
            PdaSyncResultDto result2 = PdaSyncResultDto.builder().build();
            UpdateFirmCommand command2 = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);
            command2.execute(result2);

            // Then - should normalize parent to null and not detect change on second run
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(0); // No update (parent stays null)
            assertThat(result2.getFirmsUpdated()).isEqualTo(0); // Still no update on second run
            assertThat(result.getWarnings()).hasSize(1);
            assertThat(result2.getWarnings()).hasSize(1);
            verify(firmRepository, never()).save(any()); // Never saved
        }

        @Test
        void shouldUseCachedFirmsMapInsteadOfDatabaseQueries() {
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

            // Add parent to cache
            firmsByCode.put("PARENT", parentFirm);

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

            // When
            command.execute(result);

            // Then - should use cache, NOT call findByCode
            verify(firmRepository, never()).findByCode(any());
            assertThat(existingFirm.getParentFirm()).isEqualTo(parentFirm);
        }

        @Test
        void shouldNormalizeEmptyStringToNull() {
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
                .parentFirmNumber("   ") // Empty/whitespace
                .build();

            UpdateFirmCommand command = new UpdateFirmCommand(firmRepository, existingFirm, pdaFirm, firmsByCode);

            // When
            command.execute(result);

            // Then
            assertThat(existingFirm.getParentFirm()).isNull();
            assertThat(result.getFirmsUpdated()).isEqualTo(0);
            verify(firmRepository, never()).save(any());
        }
    }
}
