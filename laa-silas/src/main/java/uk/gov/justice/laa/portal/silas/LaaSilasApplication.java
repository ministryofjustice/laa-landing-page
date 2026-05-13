package uk.gov.justice.laa.portal.silas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LaaSilasApplication {

    public static void main(String[] args) {
        SpringApplication.run(LaaSilasApplication.class, args);
    }
}
