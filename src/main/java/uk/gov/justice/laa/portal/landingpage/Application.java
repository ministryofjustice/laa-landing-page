package uk.gov.justice.laa.portal.landingpage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Javadoc comment.
 */
@SpringBootApplication(
        exclude = {SessionAutoConfiguration.class}
)
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}