package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.DistributedLock;
import uk.gov.justice.laa.portal.landingpage.repository.DistributedLockRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DistributedLockService {

    private final DistributedLockRepository lockRepository;
    private final String instanceId = UUID.randomUUID().toString();

    @Retryable(retryFor = LockAcquisitionException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
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
            maxAttempts = 5,
            backoff = @Backoff(delay = 1000, multiplier = 2))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void withLock(String lockKey, Duration lockDuration, Runnable task) {
        withLock(lockKey, lockDuration, () -> {
            task.run();
            return null;
        });
    }

    private boolean acquireLock(String key, Duration lockDuration) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lockedUntil = now.plus(lockDuration);

        int updated = lockRepository.acquireLock(key, lockedUntil, instanceId);
        if (updated == 0) {
            // Try to insert new lock
            try {
                DistributedLock lock = new DistributedLock();
                lock.setKey(key);
                lock.setLockedUntil(lockedUntil);
                lock.setLockedBy(instanceId);
                lockRepository.save(lock);
                return true;
            } catch (Exception e) {
                log.debug("Failed to acquire lock for key: {}", key, e);
                return false;
            }
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

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
