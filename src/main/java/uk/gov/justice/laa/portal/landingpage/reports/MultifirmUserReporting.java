package uk.gov.justice.laa.portal.landingpage.reports;

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
    private static final String REPORTING_LOCK_KEY = "MULTIFIRM_USER_REPORTING_LOCK";

    private final MultifirmUserReportService multifirmUserReportService;
    private final DistributedLockService lockService;

    @Value("${multifirm.user.reporting.enabled}")
    private boolean reportingEnabled;

    @Value("${app.enable.distributed.db.locking}")
    private boolean enableDistributedDbLocking;

    @Value("${app.distributed.db.locking.period}")
    private int distributedDbLockingPeriod;

    @Scheduled(fixedRateString = "${multifirm.user.reporting.interval}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void getReport() {
        if (reportingEnabled) {
            log.debug("Starting multifirm user polling process...");
            if (enableDistributedDbLocking) {
                try {
                    lockService.withLock(REPORTING_LOCK_KEY, Duration.ofMinutes(distributedDbLockingPeriod), () -> {
                        log.debug("Acquired lock for multifirm user report");
                        multifirmUserReportService.getMultifirmUsers();
                        log.debug("Completed multifirm user report");
                        return null;
                    });
                } catch (DistributedLockService.LockAcquisitionException e) {
                    log.debug("Could not acquire lock for multifirm user report. Another instance might be running.");
                } catch (Exception e) {
                    log.error("Error during multifirm user report", e);
                }
            } else {
                multifirmUserReportService.getMultifirmUsers();
            }
        } else {
            log.debug("Multifirm user reporting is disabled via configuration");
        }
    }


}
