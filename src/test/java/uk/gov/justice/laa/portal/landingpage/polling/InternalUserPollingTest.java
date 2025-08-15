package uk.gov.justice.laa.portal.landingpage.polling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.InternalUserPollingService;

import java.time.Duration;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InternalUserPollingTest {

    @Mock
    private InternalUserPollingService internalUserPollingService;

    private static final String POLLING_LOCK_KEY = "INTERNAL_USER_POLLING_LOCK";

    private InternalUserPolling internalUserPolling;
    @Mock
    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        internalUserPolling = new InternalUserPolling(internalUserPollingService, lockService);
        setPollingEnabled(true);
    }

    @Test
    void shouldAcquireLockAndCallPollForNewUsers_whenPollingEnabled() {
        // When
        internalUserPolling.poll();

        // Then
        verify(lockService).withLock(
                eq(POLLING_LOCK_KEY),
                any(Duration.class),
                any(Supplier.class)
        );
        verify(internalUserPollingService).pollForNewUsers();
    }

    @Test
    void shouldNotAcquireLock_whenPollingDisabled() {
        // Given
        setPollingEnabled(false);

        // When
        internalUserPolling.poll();

        // Then
        verify(lockService, never()).withLock(any(), any(), any(Supplier.class));
        verify(internalUserPollingService, never()).pollForNewUsers();
    }

    @Test
    void shouldHandleLockAcquisitionFailure_gracefully() {
        // Given
        doThrow(new RuntimeException("Lock acquisition failed"))
                .when(lockService)
                .withLock(any(), any(), any(Supplier.class));

        // When/Then
        assertDoesNotThrow(() -> internalUserPolling.poll());
        verify(internalUserPollingService, never()).pollForNewUsers();
    }

    @Test
    void shouldOnlyAllowOneInstanceToAcquireLock() {
        // Given
        InternalUserPolling anotherInstance = new InternalUserPolling(internalUserPollingService, lockService);
        setPollingEnabled(true);

        // When
        internalUserPolling.poll();

        // Then
        verify(lockService).withLock(
                eq(POLLING_LOCK_KEY),
                any(Duration.class),
                any(Supplier.class)
        );
        verify(internalUserPollingService).pollForNewUsers();

        // When
        anotherInstance.poll();

        // Then
        verify(lockService, times(2)).withLock(any(), any(), any(Supplier.class));
        verify(internalUserPollingService).pollForNewUsers();
    }

    @Test
    void shouldReleaseLockAfterPolling() {
        // When
        internalUserPolling.poll();

        // Then
        verify(lockService).withLock(
                eq(POLLING_LOCK_KEY),
                any(Duration.class),
                any(Supplier.class)
        );
        verify(internalUserPollingService).pollForNewUsers();
        verify(lockService).releaseLock(eq(POLLING_LOCK_KEY));
    }

    // Helper method to set polling enabled state
    private void setPollingEnabled(boolean enabled) {
        ReflectionTestUtils.setField(internalUserPolling, "pollingEnabled", enabled);
    }
}
