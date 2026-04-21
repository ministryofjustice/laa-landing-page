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
import uk.gov.justice.laa.portal.landingpage.service.UserUptakeReportService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user.uptake.reporting.enabled", havingValue = "true")
@Slf4j
public class UserUptakeReport {
    private static final String REPORTING_LOCK_KEY = "USER_UPTAKE_REPORTING_LOCK";

    private final DistributedLockService lockService;
    private final UserUptakeReportService userUptakeReportService;

    @Value("${user.uptake.reporting.enabled}")
    private boolean reportingEnabled;

    @Value("${app.enable.distributed.db.locking}")
    private boolean enableDistributedDbLocking;

    @Value("${app.distributed.db.locking.period}")
    private int distributedDbLockingPeriod;

    @Scheduled(cron = "${user.uptake.reporting.interval}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void getReport() {
        if (reportingEnabled) {
            log.debug("Starting user uptake reporting process...");
            if (enableDistributedDbLocking) {
                try {
                    lockService.withLock(REPORTING_LOCK_KEY, Duration.ofMinutes(distributedDbLockingPeriod), () -> {
                        log.debug("Acquired lock for user uptake report");
                        userUptakeReportService.getUserUptakeReport();
                        log.debug("Completed user uptake report");
                        return null;
                    });
                } catch (DistributedLockService.LockAcquisitionException e) {
                    log.debug("Could not acquire lock for user uptake report. Another instance might be running.");
                } catch (Exception e) {
                    log.error("Error during user uptake report", e);
                }
            } else {
                userUptakeReportService.getUserUptakeReport();
            }
        } else {
            log.debug("User uptake reporting is disabled via configuration");
        }
    }

}
