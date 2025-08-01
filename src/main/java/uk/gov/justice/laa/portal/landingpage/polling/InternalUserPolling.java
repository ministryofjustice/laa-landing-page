package uk.gov.justice.laa.portal.landingpage.polling;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.justice.laa.portal.landingpage.service.InternalUserPollingService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "internal.user.polling.enabled", havingValue = "true")
public class InternalUserPolling {

    private final InternalUserPollingService internalUserPollingService;

    @Value("${internal.user.polling.enabled}")
    private boolean pollingEnabled;

    @Scheduled(fixedRateString = "${internal.user.polling.interval}")
    public void poll() {
        if (pollingEnabled) {
            internalUserPollingService.pollForNewUsers();
        }
    }
}
