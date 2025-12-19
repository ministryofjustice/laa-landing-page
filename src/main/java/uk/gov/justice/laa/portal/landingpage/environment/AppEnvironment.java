package uk.gov.justice.laa.portal.landingpage.environment;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AppEnvironment {

    private final Environment env;

    public AppEnvironment(Environment env) {
        this.env = env;
    }

    public boolean isProdEnv() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }

}
