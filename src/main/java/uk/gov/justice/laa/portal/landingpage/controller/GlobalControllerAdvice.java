package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final LoginService loginService;

    public GlobalControllerAdvice(LoginService loginService) {
        this.loginService = loginService;
    }

    @ModelAttribute("activeFirm")
    public FirmDto getActiveFirm(Authentication authentication, HttpServletRequest request) {
        // Skip for claim enrichment and other rest endpoints
        if (request != null) {
            String uri = request.getRequestURI();
            if (uri != null && uri.startsWith("/api/")) {
                return null;
            }
        }
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        FirmDto firm = new FirmDto();
        if (Objects.nonNull(entraUser)) {
            //profile not set
            if (Objects.isNull(entraUser.getUserProfiles()) || entraUser.getUserProfiles().isEmpty()) {
                firm.setName("You currently don’t have access to any profiles. Please contact the admin to be added.");
            } else if (entraUser.isMultiFirmUser()) {
                UserProfile up = entraUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElse(null);
                //have active profile
                if (Objects.nonNull(up)) {
                    String firmName = up.getFirm().getName();
                    firm.setName(firmName);
                    //have more than 1 firms
                    if (entraUser.getUserProfiles().size() > 1) {
                        firm.setCanChange(true);
                    }
                } else {
                    //have no active profile
                    firm.setName("You currently don't have access to any Provider Firms. Please contact the provider firm's admin to be added.");
                    firm.setCanChange(false);
                }
            } else {
                //single firm
                UserProfile up = entraUser.getUserProfiles().stream().findFirst().get();
                if (up.getUserType().equals(UserType.EXTERNAL)) {
                    String firmName = up.getFirm().getName();
                    firm.setName(firmName);
                    firm.setCanChange(false);
                } else {
                    //internal
                    return null;
                }
            }
            return firm;
        }
        return null;
    }
}