package uk.gov.justice.laa.portal.landingpage.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
        setReportingEnabled(true);
        ReflectionTestUtils.setField(externalUserReportScheduler, "enableDistributedDbLocking", true);
    }

    @Test
    void shouldAcquireLockAndCallReportForNewUsers_whenReportEnabled() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        externalUserReportScheduler.getReport();

        // Then
        verify(externalUserReportingService).downloadExternalUserCsv();
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotTryToAcquireLockAndCallReportForNewUsers_whenReportEnabled() {
        // Given
        ReflectionTestUtils.setField(externalUserReportScheduler, "enableDistributedDbLocking", false);
        // When
        externalUserReportScheduler.getReport();

        // Then
        verify(externalUserReportingService).downloadExternalUserCsv();
        verify(lockRepository, never()).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotAcquireLock_whenReportDisabled() {
        // Given
        setReportingEnabled(false);

        // When
        externalUserReportScheduler.getReport();

        // Then
        verify(externalUserReportingService, never()).downloadExternalUserCsv();
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        // Given
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockRepository)
                .acquireLock(any(), any(), any());

        // When/Then
        assertDoesNotThrow(() -> externalUserReportScheduler.getReport());
        verify(externalUserReportingService, never()).downloadExternalUserCsv();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        // Given
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        setReportingEnabled(true);
        ExternalUserReportScheduler anotherInstance = new ExternalUserReportScheduler(externalUserReportingService, lockService);

        // When
        externalUserReportScheduler.getReport();

        // Then
        verify(externalUserReportingService).downloadExternalUserCsv();

        // When
        anotherInstance.getReport();

        // Then
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
        verify(externalUserReportingService).downloadExternalUserCsv();
    }

    @Test
    void shouldReleaseLockAfterReporting() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        externalUserReportScheduler.getReport();

        // Then
        verify(lockRepository).acquireLock(
                anyString(),
                any(LocalDateTime.class),
                anyString()
        );
        verify(externalUserReportingService).downloadExternalUserCsv();
    }

    private void setReportingEnabled(boolean enabled) {
        ReflectionTestUtils.setField(externalUserReportScheduler, "reportingEnabled", enabled);
    }
}
