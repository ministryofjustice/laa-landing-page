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
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentMatrixReportService;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "role.assignment.matrix.reporting.enabled", havingValue = "true")
@Slf4j
public class RoleAssignmentMatrixReport {
    private static final String REPORTING_LOCK_KEY = "ROLE_ASSIGNMENT_REPORT_LOCK";

    private final RoleAssignmentMatrixReportService roleAssignmentMatrixReportService;
    private final DistributedLockService lockService;

    @Value("${role.assignment.matrix.reporting.enabled}")
    private boolean reportingEnabled;

    @Value("${app.enable.distributed.db.locking}")
    private boolean enableDistributedDbLocking;

    @Value("${app.distributed.db.locking.period}")
    private int distributedDbLockingPeriod;

    @Scheduled(fixedRateString = "${role.assignment.matrix.reporting.interval}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void getReport() {
        if (reportingEnabled) {
            log.debug("Starting role assignment matrix report process...");
            if (enableDistributedDbLocking) {
                try {
                    lockService.withLock(REPORTING_LOCK_KEY, Duration.ofMinutes(distributedDbLockingPeriod), () -> {
                        log.debug("Acquired lock for internal role assignment matrix report");
                        roleAssignmentMatrixReportService.getRoleAssignmentMatrixReport();
                        log.debug("Completed internal role assignment matrix report");
                        return null;
                    });
                } catch (DistributedLockService.LockAcquisitionException e) {
                    log.debug("Could not acquire lock for role assignment matrix report. Another instance might be running.");
                } catch (Exception e) {
                    log.error("Error during role assignment matrix report", e);
                }
            } else {
                roleAssignmentMatrixReportService.getRoleAssignmentMatrixReport();
            }
        } else {
            log.debug("Role assignment matrix report is disabled via configuration");
        }
    }
}

