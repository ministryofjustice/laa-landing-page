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
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserReportingService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "external.user.reporting.enabled", havingValue = "true")
@Slf4j
public class ExternalUserReportScheduler {
    private static final String REPORTING_LOCK_KEY = "EXTERNAL_USER_REPORT_LOCK";

    private final ExternalUserReportingService externalUserReportingService;
    private final DistributedLockService lockService;

    @Value("${external.user.reporting.enabled}")
    private boolean reportingEnabled;

    @Value("${app.enable.distributed.db.locking}")
    private boolean enableDistributedDbLocking;

    @Value("${app.distributed.db.locking.period}")
    private int distributedDbLockingPeriod;

    @Scheduled(fixedRateString = "${external.user.reporting.schedule}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void getReport() {
        if (reportingEnabled) {
            log.debug("Starting external user polling process...");
            if (enableDistributedDbLocking) {
                try {
                    lockService.withLock(REPORTING_LOCK_KEY, Duration.ofMinutes(distributedDbLockingPeriod), () -> {
                        log.debug("Acquired lock for external user report");
                        externalUserReportingService.downloadExternalUserCsv();
                        log.debug("Completed external user report");
                        return null;
                    });
                } catch (DistributedLockService.LockAcquisitionException e) {
                    log.debug("Could not acquire lock for external user report. Another instance might be running.");
                } catch (Exception e) {
                    log.error("Error during external user report", e);
                }
            } else {
                externalUserReportingService.downloadExternalUserCsv();
            }
        } else {
            log.debug("External user report is disabled via configuration");
        }
    }
}
