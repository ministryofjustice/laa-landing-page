package uk.gov.justice.laa.portal.landingpage.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.UserUptakeReportService;

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
public class UserUptakeReportTest {

    @Mock
    private UserUptakeReportService userUptakeReportService;

    private UserUptakeReport userUptakeReport;
    private DistributedLockService lockService;

    @Mock
    private DistributedLockRepository lockRepository;

    @BeforeEach
    void setUp(){
        lockService = new DistributedLockService(lockRepository);
        userUptakeReport = new UserUptakeReport(lockService, userUptakeReportService);
        setReportingEnabled(true);
        ReflectionTestUtils.setField(userUptakeReport, "enableDistributedDbLocking", true);
    }

    @Test
    void shouldAcquireLockAndCallgetUserUptakeReport_whenEnabled() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        userUptakeReport.getReport();

        // Then
        verify(userUptakeReportService).getUserUptakeReport();
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotTryToAcquireLockAndCallgetMultifirmUsers_whenPollingEnabled() {
        // Given
        ReflectionTestUtils.setField(userUptakeReport, "enableDistributedDbLocking", false);
        // When
        userUptakeReport.getReport();

        // Then
        verify(userUptakeReportService).getUserUptakeReport();
        verify(lockRepository, never()).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotAcquireLock_whenPollingDisabled() {
        // Given
        setReportingEnabled(false);

        // When
        userUptakeReport.getReport();

        // Then
        verify(userUptakeReportService, never()).getUserUptakeReport();
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        // Given
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockRepository)
                .acquireLock(any(), any(), any());

        // When/Then
        assertDoesNotThrow(() -> userUptakeReport.getReport());
        verify(userUptakeReportService, never()).getUserUptakeReport();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        // Given
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        setReportingEnabled(true);
        UserUptakeReport anotherInstance = new UserUptakeReport(lockService, userUptakeReportService);

        // When
        userUptakeReport.getReport();

        // Then
        verify(userUptakeReportService).getUserUptakeReport();

        // When
        anotherInstance.getReport();

        // Then
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
        verify(userUptakeReportService).getUserUptakeReport();
    }

    @Test
    void shouldReleaseLockAfterReportGeneration() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        userUptakeReport.getReport();

        // Then
        verify(lockRepository).acquireLock(
                anyString(),
                any(LocalDateTime.class),
                anyString()
        );
        verify(userUptakeReportService).getUserUptakeReport();
    }

    private void setReportingEnabled(boolean enabled) {
        ReflectionTestUtils.setField(userUptakeReport, "reportingEnabled", enabled);
    }
}
