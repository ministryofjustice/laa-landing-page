package uk.gov.justice.laa.portal.landingpage.environment;

import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Arrays;

@Component
public class AccessGuard {

    private final Environment env;

    public AccessGuard(Environment env, LoginService loginService, UserService userService) {
        this.env = env;
    }

    public boolean isProdEnv() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }

}
