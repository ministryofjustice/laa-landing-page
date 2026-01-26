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
import uk.gov.justice.laa.portal.landingpage.service.MultifirmUserReportService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "multifirm.user.reporting.enabled", havingValue = "true")
@Slf4j
public class MultifirmUserReporting {
    private static final String POLLING_LOCK_KEY = "MULTIFIRM_USER_REPORTING_LOCK";

    private final MultifirmUserReportService multifirmUserReportService;
    private final DistributedLockService lockService;

    @Value("${multifirm.user.reporting.enabled}")
    private boolean pollingEnabled;

    @Value("${app.enable.distributed.db.locking}")
    private boolean enableDistributedDbLocking;

    @Value("${app.distributed.db.locking.period}")
    private int distributedDbLockingPeriod;

    @Scheduled(fixedRateString = "${multifirm.user.polling.interval}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void poll() {
        if (pollingEnabled) {
            log.debug("Starting multifirm user polling process...");
            if (enableDistributedDbLocking) {
                try {
                    lockService.withLock(POLLING_LOCK_KEY, Duration.ofMinutes(distributedDbLockingPeriod), () -> {
                        log.debug("Acquired lock for multifirm user polling");
                        multifirmUserReportService.getMultifirmUsers();
                        log.debug("Completed multifirm user polling");
                        return null;
                    });
                } catch (DistributedLockService.LockAcquisitionException e) {
                    log.debug("Could not acquire lock for multifirm user polling. Another instance might be running.");
                } catch (Exception e) {
                    log.error("Error during multifirm user polling", e);
                }
            } else {
                multifirmUserReportService.getMultifirmUsers();
            }
        } else {
            log.debug("Multifirm user polling is disabled via configuration");
        }
    }


}
