package uk.gov.justice.laa.portal.silas.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.silas.service.InternalUserPollingService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "internal.user.polling.enabled", havingValue = "true")
@Slf4j
public class InternalUserPollingScheduler {

    private final InternalUserPollingService internalUserPollingService;

    @Value("${internal.user.polling.enabled}")
    private boolean pollingEnabled;

    @Scheduled(cron = "${internal.user.polling.schedule}")
    public void poll() {
        if (!pollingEnabled) {
            log.debug("Internal user polling is disabled via configuration");
            return;
        }

        log.info("Scheduled internal user polling triggered");
        try {
            internalUserPollingService.pollForNewUsers();
        } catch (Exception e) {
            log.error("Error during scheduled internal user polling", e);
        }
    }
}
