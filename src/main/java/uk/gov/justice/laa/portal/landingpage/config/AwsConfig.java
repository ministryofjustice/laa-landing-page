package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
public class AwsConfig {

    @Bean
    @ConditionalOnProperty(name = "feature.flags.ssm.enabled", havingValue = "true")
    public SsmClient ssmClient() {
        return SsmClient.builder()
                .region(Region.EU_WEST_2)
                .build();
    }
}
