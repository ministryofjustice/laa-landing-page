package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.HashSet;
import java.util.Set;
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

import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

@ExtendWith(MockitoExtension.class)
class DisableFirmCommandTest {

    @Mock
    private FirmRepository firmRepository;

    private PdaSyncResultDto result;

    @BeforeEach
    void setUp() {
        result = PdaSyncResultDto.builder().build();
    }

    @Nested
    class FirmDisablingTests {

        @Test
        void shouldDisableFirmSuccessfully() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .build();

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getEnabled()).isFalse();
            assertThat(result.getFirmsDisabled()).isEqualTo(1);
            verify(firmRepository).save(firm);
        }

        @Test
        void shouldDisableAlreadyDisabledFirm() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(false) // Already disabled
                .build();

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then - idempotent operation, no changes made
            assertThat(firm.getEnabled()).isFalse();
            assertThat(result.getFirmsDisabled()).isEqualTo(0); // No increment since already disabled
            verify(firmRepository, never()).save(firm); // No save needed
        }
    }

    @Nested
    class ParentFirmClearingTests {

        @Test
        void shouldClearParentFirmReference() {
            // Given
            Firm parentFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("PARENT")
                .name("Parent Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .parentFirm(parentFirm)
                .build();

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getParentFirm()).isNull();
            assertThat(firm.getEnabled()).isFalse();
            assertThat(result.getFirmsDisabled()).isEqualTo(1);
            verify(firmRepository).save(firm);
        }

        @Test
        void shouldHandleNullParentFirm() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .parentFirm(null)
                .build();

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getParentFirm()).isNull();
            assertThat(firm.getEnabled()).isFalse();
            assertThat(result.getFirmsDisabled()).isEqualTo(1);
            verify(firmRepository).save(firm);
        }
    }

    @Nested
    class ChildFirmHandlingTests {

        @Test
        void shouldClearParentReferenceFromChildFirms() {
            // Given
            Firm parentFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("PARENT")
                .name("Parent Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .build();

            Firm childFirm1 = Firm.builder()
                .id(UUID.randomUUID())
                .code("CHILD1")
                .name("Child Firm 1")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(parentFirm)
                .build();

            Firm childFirm2 = Firm.builder()
                .id(UUID.randomUUID())
                .code("CHILD2")
                .name("Child Firm 2")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(parentFirm)
                .build();

            Set<Firm> childFirms = new HashSet<>();
            childFirms.add(childFirm1);
            childFirms.add(childFirm2);
            parentFirm.setChildFirms(childFirms);

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, parentFirm);

            // When
            command.execute(result);

            // Then
            assertThat(parentFirm.getEnabled()).isFalse();
            assertThat(parentFirm.getChildFirms()).isEmpty();
            assertThat(childFirm1.getParentFirm()).isNull();
            assertThat(childFirm2.getParentFirm()).isNull();
            assertThat(result.getFirmsDisabled()).isEqualTo(1);
            verify(firmRepository).save(parentFirm);
            verify(firmRepository).save(childFirm1);
            verify(firmRepository).save(childFirm2);
        }

        @Test
        void shouldHandleEmptyChildFirmsList() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .childFirms(new HashSet<>())
                .build();

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getEnabled()).isFalse();
            assertThat(result.getFirmsDisabled()).isEqualTo(1);
            verify(firmRepository).save(firm);
        }

        @Test
        void shouldHandleNullChildFirmsList() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .childFirms(null)
                .build();

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getEnabled()).isFalse();
            assertThat(result.getFirmsDisabled()).isEqualTo(1);
            verify(firmRepository).save(firm);
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void shouldHandleExceptionDuringDisable() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .build();

            when(firmRepository.save(any(Firm.class)))
                .thenThrow(new RuntimeException("Database error"));

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsDisabled()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Failed to disable firm");
            assertThat(result.getErrors().get(0)).contains("12345");
        }

        @Test
        void shouldHandleExceptionWhenClearingChildReferences() {
            // Given
            Firm parentFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("PARENT")
                .name("Parent Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true)
                .build();

            Firm childFirm = Firm.builder()
                .id(UUID.randomUUID())
                .code("CHILD")
                .name("Child Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .parentFirm(parentFirm)
                .build();

            Set<Firm> childFirms = new HashSet<>();
            childFirms.add(childFirm);
            parentFirm.setChildFirms(childFirms);

            // First save succeeds (child), second save fails (parent)
            when(firmRepository.save(childFirm)).thenReturn(childFirm);
            when(firmRepository.save(parentFirm))
                .thenThrow(new RuntimeException("Database error"));

            DisableFirmCommand command = new DisableFirmCommand(firmRepository, parentFirm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsDisabled()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Failed to disable firm");
        }
    }
}
