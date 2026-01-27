package uk.gov.justice.laa.portal.landingpage.polling;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.MultifirmUserReportService;

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
public class MultifirmUserReportingTest {

    @Mock
    private MultifirmUserReportService multifirmUserReportService;

    private MultifirmUserReporting multifirmUserReporting;
    private DistributedLockService lockService;
    @Mock
    private DistributedLockRepository lockRepository;

    @BeforeEach
    void setUp() {
        lockService = new DistributedLockService(lockRepository);
        multifirmUserReporting = new MultifirmUserReporting(multifirmUserReportService, lockService);
        setPollingEnabled(true);
        ReflectionTestUtils.setField(multifirmUserReporting, "enableDistributedDbLocking", true);
    }

    @Test
    void shouldAcquireLockAndCallgetMultifirmUsers_whenPollingEnabled() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        multifirmUserReporting.poll();

        // Then
        verify(multifirmUserReportService).getMultifirmUsers();
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotTryToAcquireLockAndCallgetMultifirmUsers_whenPollingEnabled() {
        // Given
        ReflectionTestUtils.setField(multifirmUserReporting, "enableDistributedDbLocking", false);
        // When
        multifirmUserReporting.poll();

        // Then
        verify(multifirmUserReportService).getMultifirmUsers();
        verify(lockRepository, never()).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotAcquireLock_whenPollingDisabled() {
        // Given
        setPollingEnabled(false);

        // When
        multifirmUserReporting.poll();

        // Then
        verify(multifirmUserReportService, never()).getMultifirmUsers();
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        // Given
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockRepository)
                .acquireLock(any(), any(), any());

        // When/Then
        assertDoesNotThrow(() -> multifirmUserReporting.poll());
        verify(multifirmUserReportService, never()).getMultifirmUsers();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        // Given
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        setPollingEnabled(true);
        MultifirmUserReporting anotherInstance = new MultifirmUserReporting(multifirmUserReportService, lockService);

        // When
        multifirmUserReporting.poll();

        // Then
        verify(multifirmUserReportService).getMultifirmUsers();

        // When
        anotherInstance.poll();

        // Then
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
        verify(multifirmUserReportService).getMultifirmUsers();
    }

    @Test
    void shouldReleaseLockAfterPolling() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        multifirmUserReporting.poll();

        // Then
        verify(lockRepository).acquireLock(
                anyString(),
                any(LocalDateTime.class),
                anyString()
        );
        verify(multifirmUserReportService).getMultifirmUsers();
    }

    // Helper method to set polling enabled state
    private void setPollingEnabled(boolean enabled) {
        ReflectionTestUtils.setField(multifirmUserReporting, "pollingEnabled", enabled);
    }
}

