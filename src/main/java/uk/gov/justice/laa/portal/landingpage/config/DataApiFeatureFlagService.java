package uk.gov.justice.laa.portal.landingpage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

import java.util.Optional;

@Service
public class DataApiFeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(DataApiFeatureFlagService.class);

    private static final String SSM_PATH = "/laa/portal/%s/feature-flags/data-api/%s";

    private final Optional<SsmClient> ssmClient;
    private final String environment;

    private volatile boolean userDataApiCallEnabled;
    private volatile boolean userDataApiRequestTokenEnabled;

    public DataApiFeatureFlagService(
            @Autowired(required = false) SsmClient ssmClient,
            @Value("${app.environment:dev}") String environment,
            @Value("${app.enable.user.data.api.call:false}") boolean defaultApiCallEnabled,
            @Value("${app.enable.user.data.api.request.token:false}") boolean defaultRequestTokenEnabled) {
        this.ssmClient = Optional.ofNullable(ssmClient);
        this.environment = environment;
        this.userDataApiCallEnabled = defaultApiCallEnabled;
        this.userDataApiRequestTokenEnabled = defaultRequestTokenEnabled;
    }

    @Scheduled(fixedDelayString = "${feature.flags.ssm.refresh-interval-ms:60000}")
    public void refresh() {
        if (ssmClient.isEmpty()) {
            return;
        }
        userDataApiCallEnabled = readFlag("user-data-api-calls-enabled", userDataApiCallEnabled);
        userDataApiRequestTokenEnabled = readFlag("user-data-api-request-token-enabled", userDataApiRequestTokenEnabled);
    }

    public boolean isUserDataApiCallEnabled() {
        return userDataApiCallEnabled;
    }

    public boolean isUserDataApiRequestTokenEnabled() {
        return userDataApiRequestTokenEnabled;
    }

    private boolean readFlag(String flagName, boolean currentValue) {
        String paramName = String.format(SSM_PATH, environment, flagName);
        try {
            String value = ssmClient.get()
                    .getParameter(GetParameterRequest.builder().name(paramName).build())
                    .parameter().value();
            boolean updated = Boolean.parseBoolean(value);
            if (updated != currentValue) {
                log.info("Feature flag '{}' changed: {} -> {}", paramName, currentValue, updated);
            }
            return updated;
        } catch (Exception e) {
            log.warn("Could not read flag '{}' from SSM, retaining current value={}: {}", paramName, currentValue, e.getMessage());
            return currentValue;
        }
    }
}
