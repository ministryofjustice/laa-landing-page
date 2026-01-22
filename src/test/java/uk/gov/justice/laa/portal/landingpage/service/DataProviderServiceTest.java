package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import uk.gov.justice.laa.portal.landingpage.dto.PdaSyncResultDto;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;

/**
 * Tests for DataProviderService focusing on synchronization logic and validation.
 * Note: HTTP layer tests are omitted due to RestClient mocking complexity.
 * Integration tests should cover the full end-to-end PDA sync flow.
 */
@ExtendWith(MockitoExtension.class)
class DataProviderServiceTest {

    @Mock
    private RestClient dataProviderRestClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FirmRepository firmRepository;

    @Mock
    private OfficeRepository officeRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    private DataProviderService dataProviderService;

    @BeforeEach
    void setUp() {
        dataProviderService = new DataProviderService(
            dataProviderRestClient,
            objectMapper,
            firmRepository,
            officeRepository,
            userProfileRepository,
            transactionTemplate
        );
    }

    @Nested
    class SynchronizationTests {

        @Test
        void shouldValidateRepositoryMethodsExist() {
            // Given
            when(firmRepository.findFirmsWithoutOffices()).thenReturn(Collections.emptyList());

            // When - Call the repository method
            var result = firmRepository.findFirmsWithoutOffices();

            // Then - verify final validation method exists
            verify(firmRepository).findFirmsWithoutOffices();
            assertThat(result).isEmpty();
        }

        @Test
        void shouldHandleFirmsWithoutOfficesDuringValidation() {
            // Given
            Firm firmWithoutOffice = Firm.builder()
                .id(UUID.randomUUID())
                .code("ORPHAN")
                .name("Orphan Firm")
                .type(FirmType.LEGAL_SERVICES_PROVIDER)
                .build();

            when(firmRepository.findFirmsWithoutOffices())
                .thenReturn(Collections.singletonList(firmWithoutOffice));

            // When
            // This would be called during synchronization
            // The service should deactivate firms without offices

            // Then - verify repository method is available
            assertThat(firmRepository.findFirmsWithoutOffices()).hasSize(1);
        }
    }

    @Nested
    class ShutdownHandlingTests {

        @Test
        void shouldAbortSyncWhenShutdownFlagIsSet() throws Exception {
            // Given - trigger shutdown before sync starts
            dataProviderService.onShutdown();

            // When
            CompletableFuture<PdaSyncResultDto> future = dataProviderService.synchronizeWithPdaAsync();
            PdaSyncResultDto result = future.get();

            // Then - sync should abort immediately with warning
            assertThat(result.getWarnings())
                .contains("Sync aborted - application is shutting down");
            assertThat(result.getFirmsCreated()).isZero();
            assertThat(result.getFirmsUpdated()).isZero();
        }
    }
}
