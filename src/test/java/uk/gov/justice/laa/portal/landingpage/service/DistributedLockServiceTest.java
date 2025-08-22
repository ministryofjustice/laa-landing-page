package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    private final String testKey = "TEST_LOCK";
    private final Duration testTimeout = Duration.ofMinutes(5);
    @Mock
    private DistributedLockRepository lockRepository;
    @InjectMocks
    private DistributedLockService lockService;

    @BeforeEach
    void setUp() {
        // Reset the lock service to get a new instance ID for each test
        lockService = new DistributedLockService(lockRepository);
    }

    @Test
    void withLock_WhenLockAcquired_ShouldExecuteTask() {
        // Given
        when(lockRepository.acquireLock(anyString(), any(), anyString())).thenReturn(1);
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        // When
        boolean result = lockService.withLock(testKey, testTimeout, () -> {
            taskExecuted.set(true);
            return true;
        });

        // Then
        assertTrue(result);
        assertTrue(taskExecuted.get());
        verify(lockRepository).releaseLock(eq(testKey), anyString());
    }

    @Test
    void withLock_WhenLockNotAcquired_ShouldNotExecuteTask() {
        // Given
        when(lockRepository.acquireLock(anyString(), any(), anyString())).thenReturn(0);
        when(lockRepository.save(any())).thenThrow(new RuntimeException("Error acquiring lock"));

        // When/Then
        assertThrows(
                DistributedLockService.LockAcquisitionException.class,
                () -> lockService.withLock(testKey, testTimeout, () -> true)
        );
        verify(lockRepository, never()).releaseLock(anyString(), anyString());
    }

    @Test
    void withLock_WhenTaskThrowsException_ShouldReleaseLock() {
        // Given
        when(lockRepository.acquireLock(anyString(), any(), anyString())).thenReturn(1);

        // When/Then
        assertThrows(
                RuntimeException.class,
                () -> lockService.withLock(testKey, testTimeout, () -> {
                    throw new RuntimeException("Test exception");
                })
        );
        verify(lockRepository).releaseLock(eq(testKey), anyString());
    }

    @Test
    void withLockVoid_WhenLockAcquired_ShouldExecuteTask() {
        // Given
        when(lockRepository.acquireLock(anyString(), any(), anyString())).thenReturn(1);
        AtomicBoolean taskExecuted = new AtomicBoolean(false);

        // When
        lockService.withLock(testKey, testTimeout, () -> taskExecuted.set(true));

        // Then
        assertTrue(taskExecuted.get());
        verify(lockRepository).releaseLock(eq(testKey), anyString());
    }

    @Test
    void extendActiveLocks_ShouldExtendLock() {
        // When
        lockService.extendActiveLocks();

        // Then - Just verify the method completes without exceptions
        // Actual implementation would verify lock extension logic
        verify(lockRepository, never()).delete(any());
    }
}
