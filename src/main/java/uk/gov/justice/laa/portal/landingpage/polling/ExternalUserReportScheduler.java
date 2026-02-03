package uk.gov.justice.laa.portal.landingpage.polling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.service.DistributedLockService;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserReportingService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "external.user.reporting.enabled", havingValue = "true")
@Slf4j
public class ExternalUserReportScheduler {
    private static final String POLLING_LOCK_KEY = "EXTERNAL_USER_REPORTING_LOCK";

    private final ExternalUserReportingService externalUserReportingService;
    private final DistributedLockService lockService;

    @Value("${external.user.reporting.enabled}")
    private boolean pollingEnabled;

    @Value("${app.enable.distributed.db.locking}")
    private boolean enableDistributedDbLocking;

    @Value("${app.distributed.db.locking.period}")
    private int distributedDbLockingPeriod;

    @Scheduled(fixedRateString = "${external.user.polling.interval}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void poll() {
        if (pollingEnabled) {
            log.debug("Starting external user polling process...");
            if (enableDistributedDbLocking) {
                try {
                    lockService.withLock(POLLING_LOCK_KEY, Duration.ofMinutes(distributedDbLockingPeriod), () -> {
                        log.debug("Acquired lock for external user polling");
                        externalUserReportingService.getExternalUsers();
                        log.debug("Completed external user polling");
                        return null;
                    });
                } catch (DistributedLockService.LockAcquisitionException e) {
                    log.debug("Could not acquire lock for external user polling. Another instance might be running.");
                } catch (Exception e) {
                    log.error("Error during external user polling", e);
                }
            } else {
                externalUserReportingService.getExternalUsers();
            }
        } else {
            log.debug("External user polling is disabled via configuration");
        }
    }
}
