package uk.gov.justice.laa.portal.landingpage.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;

import java.util.Collections;
import java.util.List;

@Configuration
public class LaaAppsConfig {

    @Bean
    public LaaApplicationsList getApplicationsList() {
        return new LaaApplicationsList();
    }

    @Setter
    @ConfigurationProperties(prefix = "laa-apps")
    public static class LaaApplicationsList {

        private List<LaaApplication> applications;

        public List<LaaApplication> getApplications() {
            if (applications == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(applications);
        }

    }
}
