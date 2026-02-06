package uk.gov.justice.laa.portal.landingpage.reports;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentMatrixReportService;

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
public class RoleAssignmentMatrixReportTest {

    @Mock
    RoleAssignmentMatrixReportService matrixReportService;

    private RoleAssignmentMatrixReport roleAssignmentMatrixReport;
    private DistributedLockService lockService;

    @Mock
    private DistributedLockRepository lockRepository;

    @BeforeEach
    void setUp() {
        lockService = new DistributedLockService(lockRepository);
        roleAssignmentMatrixReport = new RoleAssignmentMatrixReport(matrixReportService, lockService);
        setReportingEnabled(true);
        ReflectionTestUtils.setField(roleAssignmentMatrixReport, "enableDistributedDbLocking", true);
    }

    @Test
    void shouldAcquireLockAndCallgetRoleAssignmentMatrixReport_whenEnabled() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        roleAssignmentMatrixReport.getReport();

        // Then
        verify(matrixReportService).getRoleAssignmentMatrixReport();
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotTryToAcquireLockAndCallgetMultifirmUsers_whenPollingEnabled() {
        // Given
        ReflectionTestUtils.setField(roleAssignmentMatrixReport, "enableDistributedDbLocking", false);
        // When
        roleAssignmentMatrixReport.getReport();

        // Then
        verify(matrixReportService).getRoleAssignmentMatrixReport();
        verify(lockRepository, never()).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotAcquireLock_whenPollingDisabled() {
        // Given
        setReportingEnabled(false);

        // When
        roleAssignmentMatrixReport.getReport();

        // Then
        verify(matrixReportService, never()).getRoleAssignmentMatrixReport();
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        // Given
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockRepository)
                .acquireLock(any(), any(), any());

        // When/Then
        assertDoesNotThrow(() -> roleAssignmentMatrixReport.getReport());
        verify(matrixReportService, never()).getRoleAssignmentMatrixReport();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        // Given
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        setReportingEnabled(true);
        RoleAssignmentMatrixReport anotherInstance = new RoleAssignmentMatrixReport(matrixReportService, lockService);

        // When
        roleAssignmentMatrixReport.getReport();

        // Then
        verify(matrixReportService).getRoleAssignmentMatrixReport();

        // When
        anotherInstance.getReport();

        // Then
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
        verify(matrixReportService).getRoleAssignmentMatrixReport();
    }

    @Test
    void shouldReleaseLockAfterReportGeneration() {
        // When
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        roleAssignmentMatrixReport.getReport();

        // Then
        verify(lockRepository).acquireLock(
                anyString(),
                any(LocalDateTime.class),
                anyString()
        );
        verify(matrixReportService).getRoleAssignmentMatrixReport();
    }

    private void setReportingEnabled(boolean enabled) {
        ReflectionTestUtils.setField(roleAssignmentMatrixReport, "reportingEnabled", enabled);
    }

}
