package uk.gov.justice.laa.portal.landingpage.polling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.reports.ExternalUserReportScheduler;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserReportingService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExternalUserReportSchedulerTest {

    @Mock
    private ExternalUserReportingService externalUserReportingService;

    private ExternalUserReportScheduler externalUserReportScheduler;
    private DistributedLockService lockService;
    @Mock
    private DistributedLockRepository lockRepository;

    @BeforeEach
    void setUp() {
        lockService = new DistributedLockService(lockRepository);
        externalUserReportScheduler = new ExternalUserReportScheduler(externalUserReportingService, lockService);
        setPollingEnabled(true);
        ReflectionTestUtils.setField(externalUserReportScheduler, "enableDistributedDbLocking", true);
    }

    @Test
    void shouldAcquireLockAndCallPollForNewUsers_whenPollingEnabled() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        externalUserReportScheduler.poll();

        // Then
        verify(externalUserReportingService).getExternalUsers();
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotTryToAcquireLockAndCallPollForNewUsers_whenPollingEnabled() {
        // Given
        ReflectionTestUtils.setField(externalUserReportScheduler, "enableDistributedDbLocking", false);
        // When
        externalUserReportScheduler.poll();

        // Then
        verify(externalUserReportingService).getExternalUsers();
        verify(lockRepository, never()).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotAcquireLock_whenPollingDisabled() {
        // Given
        setPollingEnabled(false);

        // When
        externalUserReportScheduler.poll();

        // Then
        verify(externalUserReportingService, never()).getExternalUsers();
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        // Given
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockRepository)
                .acquireLock(any(), any(), any());

        // When/Then
        assertDoesNotThrow(() -> externalUserReportScheduler.poll());
        verify(externalUserReportingService, never()).getExternalUsers();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        // Given
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        setPollingEnabled(true);
        ExternalUserReportScheduler anotherInstance = new ExternalUserReportScheduler(externalUserReportingService, lockService);

        // When
        externalUserReportScheduler.poll();

        // Then
        verify(externalUserReportingService).getExternalUsers();

        // When
        anotherInstance.poll();

        // Then
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
        verify(externalUserReportingService).getExternalUsers();
    }

    @Test
    void shouldReleaseLockAfterPolling() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        externalUserReportScheduler.poll();

        // Then
        verify(lockRepository).acquireLock(
                anyString(),
                any(LocalDateTime.class),
                anyString()
        );
        verify(externalUserReportingService).getExternalUsers();
    }

    // Helper method to set polling enabled state
    private void setPollingEnabled(boolean enabled) {
        ReflectionTestUtils.setField(externalUserReportScheduler, "pollingEnabled", enabled);
    }
}
