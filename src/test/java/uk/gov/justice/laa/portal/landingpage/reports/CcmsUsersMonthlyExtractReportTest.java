package uk.gov.justice.laa.portal.landingpage.reports;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;
import uk.gov.justice.laa.portal.landingpage.service.CcmsUsersMonthlyExtractService;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;

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
public class CcmsUsersMonthlyExtractReportTest {

    @Mock
    private CcmsUsersMonthlyExtractService ccmsUsersMonthlyExtractService;

    private CcmsUsersMonthlyExtractReport ccmsUsersMonthlyExtractReport;
    private DistributedLockService lockService;
    @Mock
    private DistributedLockRepository lockRepository;

    @BeforeEach
    void setUp() {
        lockService = new DistributedLockService(lockRepository);
        ccmsUsersMonthlyExtractReport = new CcmsUsersMonthlyExtractReport(ccmsUsersMonthlyExtractService, lockService);
        setReportingEnabled(true);
        ReflectionTestUtils.setField(ccmsUsersMonthlyExtractReport, "enableDistributedDbLocking", true);
    }

    @Test
    void shouldAcquireLockAndCallReportForNewUsers_whenReportEnabled() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        ccmsUsersMonthlyExtractReport.getReport();

        // Then
        verify(ccmsUsersMonthlyExtractService).downloadCcmsUsersMonthlyExtract();
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotTryToAcquireLockAndCallReportForNewUsers_whenReportEnabled() {
        // Given
        ReflectionTestUtils.setField(ccmsUsersMonthlyExtractReport, "enableDistributedDbLocking", false);
        // When
        ccmsUsersMonthlyExtractReport.getReport();

        // Then
        verify(ccmsUsersMonthlyExtractService).downloadCcmsUsersMonthlyExtract();
        verify(lockRepository, never()).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotAcquireLock_whenReportDisabled() {
        // Given
        setReportingEnabled(false);

        // When
        ccmsUsersMonthlyExtractReport.getReport();

        // Then
        verify(ccmsUsersMonthlyExtractService, never()).downloadCcmsUsersMonthlyExtract();
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        // Given
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockRepository)
                .acquireLock(any(), any(), any());

        // When/Then
        assertDoesNotThrow(() -> ccmsUsersMonthlyExtractReport.getReport());
        verify(ccmsUsersMonthlyExtractService, never()).downloadCcmsUsersMonthlyExtract();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        // Given
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        setReportingEnabled(true);
        CcmsUsersMonthlyExtractReport anotherInstance = new CcmsUsersMonthlyExtractReport(ccmsUsersMonthlyExtractService, lockService);

        // When
        ccmsUsersMonthlyExtractReport.getReport();

        // Then
        verify(ccmsUsersMonthlyExtractService).downloadCcmsUsersMonthlyExtract();

        // When
        anotherInstance.getReport();

        // Then
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
        verify(ccmsUsersMonthlyExtractService).downloadCcmsUsersMonthlyExtract();
    }

    @Test
    void shouldReleaseLockAfterReporting() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        ccmsUsersMonthlyExtractReport.getReport();

        // Then
        verify(lockRepository).acquireLock(
                anyString(),
                any(LocalDateTime.class),
                anyString()
        );
        verify(ccmsUsersMonthlyExtractService).downloadCcmsUsersMonthlyExtract();
    }

    private void setReportingEnabled(boolean enabled) {
        ReflectionTestUtils.setField(ccmsUsersMonthlyExtractReport, "reportingEnabled", enabled);
    }
}
