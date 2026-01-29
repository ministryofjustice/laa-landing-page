package uk.gov.justice.laa.portal.landingpage.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserPollingService;

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
class ExternalUserPollingTest {

    @Mock
    private ExternalUserPollingService externalUserPollingService;

    private ExternalUserPolling externalUserPolling;
    private DistributedLockService lockService;
    @Mock
    private DistributedLockRepository lockRepository;

    @BeforeEach
    void setUp() {
        lockService = new DistributedLockService(lockRepository);
        externalUserPolling = new ExternalUserPolling(externalUserPollingService, lockService);
        ReflectionTestUtils.setField(externalUserPolling, "enableDistributedDbLocking", true);
        ReflectionTestUtils.setField(externalUserPolling, "distributedDbLockingPeriod", 5);
        ReflectionTestUtils.setField(externalUserPolling, "pollingEnabled", true);
    }

    @Test
    void shouldAcquireLockAndCallUpdateSyncMetadata_whenLockingEnabled() {
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        externalUserPolling.poll();

        verify(externalUserPollingService).updateSyncMetadata();
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotTryToAcquireLockAndCallUpdateSyncMetadata_whenLockingDisabled() {
        ReflectionTestUtils.setField(externalUserPolling, "enableDistributedDbLocking", false);
        
        externalUserPolling.poll();

        verify(externalUserPollingService).updateSyncMetadata();
        verify(lockRepository, never()).acquireLock(any(), any(), any());
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockRepository)
                .acquireLock(any(), any(), any());

        assertDoesNotThrow(() -> externalUserPolling.poll());
        verify(externalUserPollingService, never()).updateSyncMetadata();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1).thenReturn(0);
        ExternalUserPolling anotherInstance = new ExternalUserPolling(externalUserPollingService, lockService);
        ReflectionTestUtils.setField(anotherInstance, "enableDistributedDbLocking", true);
        ReflectionTestUtils.setField(anotherInstance, "distributedDbLockingPeriod", 5);
        ReflectionTestUtils.setField(anotherInstance, "pollingEnabled", true);

        externalUserPolling.poll();

        verify(externalUserPollingService).updateSyncMetadata();

        anotherInstance.poll();

        verify(lockRepository, times(2)).acquireLock(any(), any(), any());
        verify(externalUserPollingService, times(1)).updateSyncMetadata();
    }

    @Test
    void shouldReleaseLockAfterPolling() {
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        externalUserPolling.poll();

        verify(lockRepository).acquireLock(
                anyString(),
                any(LocalDateTime.class),
                anyString()
        );
        verify(externalUserPollingService).updateSyncMetadata();
    }

    @Test
    void shouldHandleServiceException_gracefully() {
        ReflectionTestUtils.setField(externalUserPolling, "enableDistributedDbLocking", false);
        doThrow(new RuntimeException("Service error"))
                .when(externalUserPollingService)
                .updateSyncMetadata();

        assertDoesNotThrow(() -> externalUserPolling.poll());
        verify(externalUserPollingService).updateSyncMetadata();
    }

    @Test
    void shouldHandleServiceExceptionWithLocking_gracefully() {
        when(lockRepository.acquireLock(any(), any(), any())).thenReturn(1);
        doThrow(new RuntimeException("Service error"))
                .when(externalUserPollingService)
                .updateSyncMetadata();

        assertDoesNotThrow(() -> externalUserPolling.poll());
        verify(externalUserPollingService).updateSyncMetadata();
    }
}
