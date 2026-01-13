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
    private final LoginService loginService;
    private final UserService userService;

    public AccessGuard(Environment env, LoginService loginService, UserService userService) {
        this.env = env;
        this.loginService = loginService;
        this.userService = userService;
    }

    public boolean isProdEnv() {
        return Arrays.asList(env.getActiveProfiles()).contains("prod");
    }

    public boolean canDelegateInNonProd(Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        boolean internal = userService.isInternal(entraUser.getId());
        return !isProdEnv() && internal;
    }

}
