package uk.gov.justice.laa.portal.landingpage.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserPollingService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalUserPolling {
    private static final String SYNC_LOCK_KEY = "EXTERNAL_USER_POLLING_LOCK";

    private final ExternalUserPollingService externalUserPollingService;
    private final DistributedLockService lockService;

    @Value("${app.enable.distributed.db.locking}")
    private boolean enableDistributedDbLocking;

    @Value("${app.distributed.db.locking.period}")
    private int distributedDbLockingPeriod;

    @Value("${external.user.polling.enabled}")
    private boolean pollingEnabled;

    /**
     * Scheduled task that runs at configurable intervals to update the EntraLastSyncMetadata.
     * This tracks the last successful sync operation and updates the metadata accordingly.
     */
    @Scheduled(fixedRateString = "${external.user.polling.interval}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void poll() {
        if (pollingEnabled) {
            log.debug("Starting scheduled external user sync");

            if (enableDistributedDbLocking) {
                try {
                    lockService.withLock(SYNC_LOCK_KEY, Duration.ofMinutes(distributedDbLockingPeriod), () -> {
                        log.debug("Acquired lock for external user sync");
                        externalUserPollingService.updateSyncMetadata();
                        log.debug("Completed external user sync");
                        return null;
                    });
                } catch (DistributedLockService.LockAcquisitionException e) {
                    log.debug("Could not acquire lock for external user sync. Another instance might be running.");
                } catch (Exception e) {
                    log.error("Error during external user sync", e);
                }
            } else {
                try {
                    externalUserPollingService.updateSyncMetadata();
                    log.debug("Successfully completed external user sync from Entra");
                } catch (Exception e) {
                    log.error("Error during scheduled external user sync from Entra", e);
                }
            }
        } else {
            log.debug("External user polling disabled via config");
        }
    }
}
