package uk.gov.justice.laa.portal.landingpage.service.pda.command;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;

@ExtendWith(MockitoExtension.class)
class EnableFirmCommandTest {

    @Mock
    private FirmRepository firmRepository;

    private PdaSyncResultDto result;

    @BeforeEach
    void setUp() {
        result = PdaSyncResultDto.builder().build();
    }

    @Nested
    class FirmEnablingTests {

        @Test
        void shouldEnableFirmSuccessfully() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(false) // Disabled
                .build();

            EnableFirmCommand command = new EnableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getEnabled()).isTrue();
            assertThat(result.getFirmsReactivated()).isEqualTo(1);
            verify(firmRepository).save(firm);
        }

        @Test
        void shouldEnableAlreadyEnabledFirm() {
            // Given - although this scenario is unlikely in practice
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(true) // Already enabled
                .build();

            EnableFirmCommand command = new EnableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getEnabled()).isTrue();
            assertThat(result.getFirmsReactivated()).isEqualTo(1);
            verify(firmRepository).save(firm);
        }

        @Test
        void shouldRestoreAccessForDisabledFirm() {
            // Given - firm was disabled due to contract gap
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("CONTRACTOR")
                .name("Contractor Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(false)
                .build();

            EnableFirmCommand command = new EnableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then - firm is now accessible to users again
            assertThat(firm.getEnabled()).isTrue();
            assertThat(result.getFirmsReactivated()).isEqualTo(1);
            verify(firmRepository).save(firm);
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void shouldHandleExceptionDuringEnable() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("12345")
                .name("Test Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(false)
                .build();

            when(firmRepository.save(any(Firm.class)))
                .thenThrow(new RuntimeException("Database error"));

            EnableFirmCommand command = new EnableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsReactivated()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Failed to enable firm");
            assertThat(result.getErrors().get(0)).contains("12345");
        }

        @Test
        void shouldHandleDatabaseConnectionError() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("FIRM123")
                .name("Database Test Firm")
                .type(FirmType.ADVOCATE)
                .enabled(false)
                .build();

            when(firmRepository.save(any(Firm.class)))
                .thenThrow(new RuntimeException("Connection timeout"));

            EnableFirmCommand command = new EnableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(result.getFirmsReactivated()).isEqualTo(0);
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0)).contains("Failed to enable firm");
            assertThat(result.getErrors().get(0)).contains("FIRM123");
            assertThat(result.getErrors().get(0)).contains("Connection timeout");
        }
    }

    @Nested
    class DifferentFirmTypesTests {

        @Test
        void shouldEnableLegalServicesProviderFirm() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("LSP123")
                .name("Legal Services Provider")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .enabled(false)
                .build();

            EnableFirmCommand command = new EnableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getEnabled()).isTrue();
            assertThat(result.getFirmsReactivated()).isEqualTo(1);
        }

        @Test
        void shouldEnableAdvocateFirm() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("ADV123")
                .name("Advocate Firm")
                .type(FirmType.ADVOCATE)
                .enabled(false)
                .build();

            EnableFirmCommand command = new EnableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getEnabled()).isTrue();
            assertThat(result.getFirmsReactivated()).isEqualTo(1);
        }

        @Test
        void shouldEnableChambersFirm() {
            // Given
            Firm firm = Firm.builder()
                .id(UUID.randomUUID())
                .code("CHAM123")
                .name("Chambers Firm")
                .type(FirmType.CHAMBERS)
                .enabled(false)
                .build();

            EnableFirmCommand command = new EnableFirmCommand(firmRepository, firm);

            // When
            command.execute(result);

            // Then
            assertThat(firm.getEnabled()).isTrue();
            assertThat(result.getFirmsReactivated()).isEqualTo(1);
        }
    }
}
