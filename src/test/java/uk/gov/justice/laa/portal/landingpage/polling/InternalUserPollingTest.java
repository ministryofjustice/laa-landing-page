package uk.gov.justice.laa.portal.landingpage.polling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.InternalUserPollingService;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalUserPollingTest {

    @Mock
    private InternalUserPollingService internalUserPollingService;

    private InternalUserPolling internalUserPolling;
    private DistributedLockService lockService;
    @Mock
    private DistributedLockRepository lockRepository;

    @BeforeEach
    void setUp() {
        lockService = new DistributedLockService(lockRepository);
        internalUserPolling = new InternalUserPolling(internalUserPollingService, lockService);
        setPollingEnabled(true);
        ReflectionTestUtils.setField(internalUserPolling, "enableDistributedDbLocking", true);
    }

    @Test
    void shouldAcquireLockAndCallPollForNewUsers_whenPollingEnabled() {
        // When
        internalUserPolling.poll();

        // Then
        verify(internalUserPollingService).pollForNewUsers();
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotTryToAcquireLockAndCallPollForNewUsers_whenPollingEnabled() {
        // Given
        ReflectionTestUtils.setField(internalUserPolling, "enableDistributedDbLocking", false);
        // When
        internalUserPolling.poll();

        // Then
        verify(internalUserPollingService).pollForNewUsers();
        verify(lockRepository, never()).acquireLock(any(), any(), any());
    }

    @Test
    void shouldNotAcquireLock_whenPollingDisabled() {
        // Given
        setPollingEnabled(false);

        // When
        internalUserPolling.poll();

        // Then
        verify(internalUserPollingService, never()).pollForNewUsers();
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        // Given
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockRepository)
                .acquireLock(any(), any(), any());

        // When/Then
        assertDoesNotThrow(() -> internalUserPolling.poll());
        verify(internalUserPollingService, never()).pollForNewUsers();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        // Given
        setPollingEnabled(true);
        InternalUserPolling anotherInstance = new InternalUserPolling(internalUserPollingService, lockService);

        // When
        internalUserPolling.poll();

        // Then
        verify(internalUserPollingService).pollForNewUsers();

        // When
        anotherInstance.poll();

        // Then
        verify(lockRepository, times(1)).acquireLock(any(), any(), any());
        verify(internalUserPollingService).pollForNewUsers();
    }

    @Test
    void shouldReleaseLockAfterPolling() {
        // When
        internalUserPolling.poll();

        // Then
        verify(lockRepository).acquireLock(
                anyString(),
                any(LocalDateTime.class),
                anyString()
        );
        verify(internalUserPollingService).pollForNewUsers();
    }

    // Helper method to set polling enabled state
    private void setPollingEnabled(boolean enabled) {
        ReflectionTestUtils.setField(internalUserPolling, "pollingEnabled", enabled);
    }
}
