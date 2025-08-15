package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.OfficeDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

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
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        List<FirmDto> list;
        if (userService.isInternal(entraUser.getId())) {
            list = firmService.getAllFirmsFromCache();
        } else {
            list = firmService.getUserFirms(entraUser);
        }
        model.addAttribute("firms", list);
        model.addAttribute("pageTitle", "Firms");
        return "firms";
    }

    @GetMapping("/firm/{id}")
    public String getFirm(@PathVariable String id, Model model, Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        if (!userService.isInternal(entraUser.getId())) {
            boolean isMyFirm = firmService.getUserFirms(entraUser).stream()
                    .anyMatch(o -> o.getId().equals(UUID.fromString(id)));
            if (!isMyFirm) {
                log.debug("Access denied for firm id: {}, user: {}", id, entraUser.getEntraOid());
                return "redirect:/pda/firms";
            }
        }
        FirmDto firmDto = firmService.getFirm(id);
        model.addAttribute("firm", firmDto);
        model.addAttribute("pageTitle", "Firms");
        return "firm";
    }

    @GetMapping("/offices")
    public String getOffices(Model model, Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        List<OfficeDto> list;
        if (userService.isInternal(entraUser.getId())) {
            list = officeService.getOffices().stream().map(office -> mapper.map(office, OfficeDto.class)).toList();
        } else {
            list = officeService.getUserOffices(entraUser);
        }
        model.addAttribute("offices", list);
        model.addAttribute("pageTitle", "Offices");
        return "offices";
    }

    @GetMapping("/office/{id}")
    public String getOffice(@PathVariable String id, Model model, Authentication authentication) {
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        Office office = officeService.getOffice(UUID.fromString(id));
        UUID myFirmId = office.getFirm().getId();
        if (!userService.isInternal(entraUser.getId())) {
            boolean isMyOffice = entraUser.getUserProfiles()
                    .stream().filter(UserProfile::isActiveProfile)
                    .anyMatch(userProfile -> userProfile.getFirm().getId().equals(myFirmId));
            if (!isMyOffice) {
                log.debug("Access denied for office id: {}, user: {}", id, entraUser.getEntraOid());
                return "redirect:/pda/offices";
            }
        }
        OfficeDto officeDto = mapper.map(office, OfficeDto.class);
        model.addAttribute("office", officeDto);
        model.addAttribute("pageTitle", "Offices");
        return "office";
    }

}
