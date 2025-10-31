package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@Slf4j
@Controller
@RequestMapping("/home")
public class HomeController {

    private final LoginService loginService;
    private final UserService userService;
    private final ModelMapper mapper;

    public HomeController(LoginService loginService, UserService userService, ModelMapper mapper) {
        this.loginService = loginService;
        this.userService = userService;
        this.mapper = mapper;
    }

    @GetMapping("/my-account-details")
    public String myAccountDetails(Model model, Authentication authentication) {
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        EntraUser entraUser = currentUserProfile.getEntraUser();
        Firm userFirm = currentUserProfile.getFirm();
        Set<Office> userOffices = currentUserProfile.getOffices();

        EntraUserDto user = mapper.map(entraUser, EntraUserDto.class);
        FirmDto firmDto = userFirm != null ? mapper.map(userFirm, FirmDto.class) : null;
        Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(currentUserProfile.getId().toString());
        List<OfficeDto> offices = userOffices.stream().map(office -> mapper.map(office, OfficeDto.class)).toList();

        boolean isMultiFirmUser = user.isMultiFirmUser();
        boolean isUserManager = currentUserProfile.getAppRoles().stream()
                .anyMatch(role -> "Firm User Manager".equals(role.getName()));

        model.addAttribute("user", user);
        model.addAttribute("isMultiFirmUser", isMultiFirmUser);
        model.addAttribute("isUserManager", isUserManager);
        model.addAttribute("userOffices", offices);
        model.addAttribute("firm", firmDto);
        model.addAttribute("appAssignments", userAssignedApps);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "My Account - " + user.getFullName());

        return "home/my-account-details";
    }

}
