package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;
import java.util.UUID;


/**
 * Provider Data Controller
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/pda")
public class PdaController {

    private final LoginService loginService;
    private final UserService userService;
    private final OfficeService officeService;
    private final FirmService firmService;
    private final ModelMapper mapper;

    @GetMapping("/firms")
    public String getFirms(Model model, Authentication authentication) {
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        EntraUser entraUser = userService.getUserByEntraUserId(currentUserDto.getUserId());
        assert entraUser != null;
        List<FirmDto> list;
        if (isInternal(entraUser)) {
            list = firmService.getFirms();
        } else {
            list = getUserFirms(entraUser);
        }
        model.addAttribute("firms", list);
        return "firms";
    }

    @GetMapping("/firms/{id}")
    public String getFirm(@PathVariable String id, Model model, Authentication authentication) {
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        EntraUser entraUser = userService.getUserByEntraUserId(currentUserDto.getUserId());
        assert entraUser != null;
        if (!isInternal(entraUser)) {
            boolean isMyFirm = getUserFirms(entraUser).stream().anyMatch(o -> o.getId().equals(UUID.fromString(id)));
            if (!isMyFirm) {
                log.debug("Access denied for firm id: {}, user: {}", id, currentUserDto.getUserId());
                return "redirect:/pda/firms";
            }
        }
        FirmDto firmDto = firmService.getFirm(id);
        model.addAttribute("firm", firmDto);
        return "/firm";
    }

    @GetMapping("/offices")
    public String getOffices(Model model, Authentication authentication) {
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        EntraUser entraUser = userService.getUserByEntraUserId(currentUserDto.getUserId());
        assert entraUser != null;
        List<OfficeDto> list;
        if (isInternal(entraUser)) {
            list = officeService.getOffices().stream().map(office -> mapper.map(office, OfficeDto.class)).toList();
        } else {
            list = getUserOffices(entraUser);
        }
        model.addAttribute("offices", list);
        return "offices";
    }

    @GetMapping("/offices/{id}")
    public String getOffice(@PathVariable String id, Model model, Authentication authentication) {
        CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
        EntraUser entraUser = userService.getUserByEntraUserId(currentUserDto.getUserId());
        assert entraUser != null;
        Office office = officeService.getOffice(UUID.fromString(id));
        UUID myFirmId = office.getFirm().getId();
        if (!isInternal(entraUser)) {
            boolean isMyOffice = entraUser.getUserProfiles()
                    .stream().anyMatch(userProfile -> userProfile.getFirm().getId().equals(myFirmId));
            if (!isMyOffice) {
                log.debug("Access denied for office id: {}, user: {}", id, currentUserDto.getUserId());
                return "redirect:/pda/offices";
            }
        }
        model.addAttribute("office", office);
        return "office";
    }

    protected List<OfficeDto> getUserOffices(EntraUser entraUser) {
        List<UUID> firms = entraUser.getUserProfiles().stream()
                .map(userProfile -> userProfile.getFirm().getId()).toList();
        return officeService.getOfficesByFirms(firms)
                .stream().map(office -> mapper.map(office, OfficeDto.class)).toList();
    }

    protected List<FirmDto> getUserFirms(EntraUser entraUser) {
        return entraUser.getUserProfiles().stream()
                .map(userProfile -> mapper.map(userProfile.getFirm(), FirmDto.class)).toList();
    }

    protected boolean isInternal(EntraUser entraUser) {
        List<UserType> userTypes = entraUser.getUserProfiles().stream()
                .map(UserProfile::getUserType).toList();
        return userTypes.contains(UserType.INTERNAL);
    }

}
