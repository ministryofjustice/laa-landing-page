package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.FirmOfficesCriteria;
import uk.gov.justice.laa.portal.landingpage.dto.PaginatedOffices;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/firmDirectory/firmOffices")
@PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions("
        + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_FIRM_DIRECTORY)")
public class FirmOfficesController {

    private final FirmService firmService;
    private final OfficeService officeService;

    @GetMapping("/{id}")
    public String displayFirmDetails(
            @PathVariable UUID id,
            Model model,
            @ModelAttribute FirmOfficesCriteria criteria) {

        PaginatedOffices paginatedOffices = officeService.getOfficesPage(
                id,
                criteria.getPage(),
                criteria.getSize(),
                criteria.getSort(),
                criteria.getDirection()
        );

        model.addAttribute("firm", firmService.getFirm(id));
        model.addAttribute("firmOffices", paginatedOffices);
        model.addAttribute("criteria", criteria);
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Firm Details");

        return "firm-directory/firm-offices";
    }
}
