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
    private final ModelMapper mapper;

    public GlobalControllerAdvice(LoginService loginService, ModelMapper mapper) {
        this.loginService = loginService;
        this.mapper = mapper;
    }

    @ModelAttribute("activeFirm")
    public FirmDto getActiveFirm(Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        if (Objects.nonNull(entraUser)) {
            if (entraUser.getUserProfiles().size() > 1) {

            } else {
                return mapper.map(entraUser, FirmDto.class);
            }
            UserProfile up = entraUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst()
                    .orElse(entraUser.getUserProfiles().stream().findFirst().orElse(null));
            if (Objects.nonNull(up) && up.getUserType().equals(UserType.EXTERNAL)) {
                return mapper.map(up.getFirm(), FirmDto.class);
            }
        }
        return null;
    }
}
