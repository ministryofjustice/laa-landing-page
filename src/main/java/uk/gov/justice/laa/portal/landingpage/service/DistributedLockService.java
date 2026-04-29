package uk.gov.justice.laa.portal.landingpage.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DistributedLockService {

    private final DistributedLockRepository lockRepository;
    private final String instanceId = UUID.randomUUID().toString();

    @Retryable(retryFor = LockAcquisitionException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 10000, multiplier = 2))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public <T> T withLock(String lockKey, Duration lockDuration, Supplier<T> task) {
        if (acquireLock(lockKey, lockDuration)) {
            try {
                return task.get();
            } finally {
                releaseLock(lockKey);
            }
        } else {
            throw new LockAcquisitionException("Failed to acquire lock for key: " + lockKey);
        }
    }

    @Retryable(retryFor = LockAcquisitionException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 10000, multiplier = 2))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void withLock(String lockKey, Duration lockDuration, Runnable task) {
        withLock(lockKey, lockDuration, () -> {
            task.run();
            return null;
        });
    }

    /**
     * Attempts to acquire the lock once without retrying.
     * Returns true if the lock was acquired and the task executed, false otherwise.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryOnceWithLock(String lockKey, Duration lockDuration, Runnable task) {
        if (acquireLock(lockKey, lockDuration)) {
            try {
                task.run();
            } finally {
                releaseLock(lockKey);
            }
            return true;
        }
        return false;
    }

    private boolean acquireLock(String key, Duration lockDuration) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = now.plus(lockDuration);

        try {
            int updated = lockRepository.acquireLock(key, lockedUntil, instanceId);
            if (updated == 0) {
                return false;
            }
        } catch (Exception e) {
            log.debug("Failed to acquire lock for key: {}", key, e);
            return false;
        }
        return true;
    }

    public void releaseLock(String key) {
        try {
            lockRepository.releaseLock(key, instanceId);
        } catch (Exception e) {
            log.warn("Failed to release lock for key: {}", key, e);
        }
    }

    @Scheduled(fixedDelay = 30000) // Extend lock every 30 seconds
    @Transactional
    public void extendActiveLocks() {
        // This would be called periodically to extend active locks
        // Implementation depends on your specific requirements
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
