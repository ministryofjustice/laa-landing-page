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
import uk.gov.justice.laa.portal.landingpage.service.CcmsUsersMonthlyExtractService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "external.user.reporting.pui.enabled", havingValue = "true")
@Slf4j
public class CcmsUsersMonthlyExtractReport {
    private static final String REPORTING_LOCK_KEY = "EXTERNAL_USER_PUI_REPORT_LOCK";

    private final CcmsUsersMonthlyExtractService ccmsUsersMonthlyExtractService;
    private final DistributedLockService lockService;

    @Value("${ccms.user.monthly.reporting.enabled}")
    private boolean reportingEnabled;

    @Value("${app.enable.distributed.db.locking}")
    private boolean enableDistributedDbLocking;

    @Value("${app.distributed.db.locking.period}")
    private int distributedDbLockingPeriod;

    @Scheduled(cron = "${ccms.user.monthly.reporting.schedule}", zone = "Europe/London")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void getReport() {
        if (reportingEnabled) {
            log.debug("Starting ccms user monthly report process...");
            if (enableDistributedDbLocking) {
                try {
                    lockService.withLock(REPORTING_LOCK_KEY, Duration.ofMinutes(distributedDbLockingPeriod), () -> {
                        log.debug("Acquired lock for ccms user monthly report");
                        ccmsUsersMonthlyExtractService.downloadCcmsUsersMonthlyExtract();
                        log.debug("Completed ccms user monthly report");
                        return null;
                    });
                } catch (DistributedLockService.LockAcquisitionException e) {
                    log.debug("Could not acquire lock for ccms user monthly report. Another instance might be running.");
                } catch (Exception e) {
                    log.error("Error during ccms user monthly report", e);
                }
            } else {
                ccmsUsersMonthlyExtractService.downloadCcmsUsersMonthlyExtract();
            }
        } else {
            log.debug("Ccms user monthly report is disabled via configuration");
        }
    }
}
