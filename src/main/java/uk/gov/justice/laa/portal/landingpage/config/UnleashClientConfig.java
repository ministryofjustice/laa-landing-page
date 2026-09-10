package uk.gov.justice.laa.portal.landingpage.config;

import io.getunleash.DefaultUnleash;
import io.getunleash.Unleash;
import io.getunleash.util.UnleashConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UnleashClientConfig {

    @Bean
    @ConditionalOnProperty(name = "unleash.enabled", havingValue = "true")
    public Unleash unleash(
            @Value("${unleash.url}") String url,
            @Value("${unleash.api-key}") String apiKey,
            @Value("${unleash.app-name:laa-landing-page}") String appName,
            @Value("${unleash.environment:development}") String environment) {
        UnleashConfig config = UnleashConfig.builder()
                .appName(appName)
                .environment(environment)
                .unleashAPI(url)
                .apiKey(apiKey)
                .build();
        return new DefaultUnleash(config);
    }
}
