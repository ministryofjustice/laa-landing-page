package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.AppService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@Slf4j
@Controller
@RequestMapping("/home")
public class HomeController {

    private final LoginService loginService;
    private final UserService userService;
    private final AppService appService;
    private final ModelMapper mapper;

    public HomeController(LoginService loginService, UserService userService,
            AppService appService, ModelMapper mapper) {
        this.loginService = loginService;
        this.userService = userService;
        this.appService = appService;
        this.mapper = mapper;
    }

    @GetMapping("/my-account-details")
    public String myAccountDetails(Model model, Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        Optional<UserProfile> currentUserProfileOptional = Optional.ofNullable(loginService.getCurrentProfile(authentication));
        EntraUserDto user = mapper.map(entraUser, EntraUserDto.class);
        boolean isMultiFirmUser = entraUser.isMultiFirmUser();

        // Default values which require a profile for users with no profiles.
        Map<String, List<String>> appAssignments = new HashMap<>();
        FirmDto firmDto = null;
        List<OfficeDto> offices = new ArrayList<>();
        boolean isUserManager = false;
        boolean isInternalUser = false;
        boolean unrestrictedOfficeAccess = false;

        if (currentUserProfileOptional.isPresent()) {
            UserProfile currentUserProfile = currentUserProfileOptional.get();
            Set<AppDto> userAssignedApps = userService.getUserAppsByUserId(currentUserProfile.getId().toString());
            List<AppRoleDto> userAssignedAppRoles = userService
                    .getUserAppRolesByUserId(currentUserProfile.getId().toString());
            List<String> accessPermission = List.of("Access");
            for (AppDto appDto : userAssignedApps) {
                Optional<App> optionalApp = appService.getById(UUID.fromString(appDto.getId()));
                if (optionalApp.isPresent()) {
                    App app = optionalApp.get();
                    List<String> appRoles = userAssignedAppRoles.stream()
                            .filter(ar -> ar.getApp().getName().equals(app.getName()))
                            .map(AppRoleDto::getName).toList();
                    if (app.getAppRoles().size() > 1) {
                        appAssignments.put(appDto.getName(), appRoles);
                    } else {
                        appAssignments.put(appDto.getName(), accessPermission);
                    }
                }
            }

            isUserManager = currentUserProfile.getAppRoles().stream()
                    .anyMatch(role -> "Firm User Manager".equals(role.getName()));
            Firm userFirm = currentUserProfile.getFirm();
            firmDto = userFirm != null ? mapper.map(userFirm, FirmDto.class) : null;
            Set<Office> userOffices = currentUserProfile.getOffices();
            offices = userOffices.stream().map(office -> mapper.map(office, OfficeDto.class)).toList();
            isInternalUser = currentUserProfile.getUserType().equals(UserType.INTERNAL);
            unrestrictedOfficeAccess = currentUserProfile.isUnrestrictedOfficeAccess();
        }

        model.addAttribute("user", user);
        model.addAttribute("isMultiFirmUser", isMultiFirmUser);
        model.addAttribute("isUserManager", isUserManager);
        model.addAttribute("userOffices", offices);
        model.addAttribute("firm", firmDto);
        model.addAttribute("appAssignments", appAssignments);
        model.addAttribute("isInternalUser", isInternalUser);
        model.addAttribute("unrestrictedOfficeAccess", unrestrictedOfficeAccess);

        model.addAttribute(ModelAttributes.PAGE_TITLE, "My Account - " + user.getFullName());

        return "home/my-account-details";
    }

}
