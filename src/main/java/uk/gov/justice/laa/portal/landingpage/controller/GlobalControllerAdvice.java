package uk.gov.justice.laa.portal.landingpage.controller;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final LoginService loginService;

    public GlobalControllerAdvice(LoginService loginService, ModelMapper mapper) {
        this.loginService = loginService;
    }

    @ModelAttribute("activeFirm")
    public FirmDto getActiveFirm(Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        FirmDto firm = new FirmDto();
        if (Objects.nonNull(entraUser) && entraUser.isMultiFirmUser()) {
            //profile not set
            if (Objects.isNull(entraUser.getUserProfiles()) || entraUser.getUserProfiles().isEmpty()) {
                firm.setName("You currently don’t have access to any Provider Firms. Please contact the provider firm’s admin to be added.");
            } else {
                UserProfile up = entraUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElse(null);
                //have active profile
                if (Objects.nonNull(up)) {
                    //have more than 1 firms
                    firm.setName(up.getFirm().getName());
                    firm.setCode(up.getFirm().getCode());
                    if (entraUser.getUserProfiles().size() > 1) {
                        firm.setCanChange(true);
                    }
                } else {
                    //have no active profile
                    firm.setName("You currently don’t have any active firms. Please select one active firm.");
                    firm.setCanChange(true);
                }
            }
            return firm;
        }
        return null;
    }
}
