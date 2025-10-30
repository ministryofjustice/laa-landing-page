package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/home")
public class HomeController {

    private final LoginService loginService;
    private final ModelMapper mapper;

    public HomeController(LoginService loginService, ModelMapper mapper) {
        this.loginService = loginService;
        this.mapper = mapper;
    }

    @GetMapping("/my-account-details")
    public String myAccountDetails(Model model, Authentication authentication) {
        UserProfile currentUserProfile = loginService.getCurrentProfile(authentication);
        EntraUser entraUser = currentUserProfile.getEntraUser();
        Firm userFirm = currentUserProfile.getFirm();
        Set<AppRole> userAppRoles = currentUserProfile.getAppRoles();
        Set<Office> userOffices = currentUserProfile.getOffices();

        EntraUserDto user = mapper.map(entraUser, EntraUserDto.class);
        FirmDto firmDto = mapper.map(userFirm, FirmDto.class);
        List<AppRoleDto> appRoleDtoList = userAppRoles.stream().map(role -> mapper.map(role, AppRoleDto.class)).sorted().toList();
        Map<String, List<AppRoleDto>> appAssignments = appRoleDtoList.stream().collect(Collectors.groupingBy(appRole -> appRole.getApp().getName()));
        List<OfficeDto> offices = userOffices.stream().map(office -> mapper.map(office, OfficeDto.class)).toList();

        model.addAttribute("user", user);
        model.addAttribute("userOffices", offices);
        model.addAttribute("firm", firmDto);
        model.addAttribute("appAssignments", appAssignments);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "My Account - " + user.getFullName());

        return "home/my-account-details";
    }

}
