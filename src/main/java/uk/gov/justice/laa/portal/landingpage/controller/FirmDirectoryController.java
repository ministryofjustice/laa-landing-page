package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

/**
 * Controller for handling firm-directory requests
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/firmDirectory")
public class FirmDirectoryController {


    public static final String SEARCH_PAGE = "/firm-directory/search-page";

    @GetMapping()
/*    @PreAuthorize("@accessControlService.authenticatedUserHasAnyGivenPermissions(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_EXTERNAL_USER,"
            + "T(uk.gov.justice.laa.portal.landingpage.entity.Permission).VIEW_INTERNAL_USER)")*/
    public String displayAllFirmDirectory(
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "usertype", required = false) String usertype,
            @RequestParam(name = "search", required = false, defaultValue = "") String search,
            @RequestParam(name = "showFirmAdmins", required = false) boolean showFirmAdmins,
            @RequestParam(name = "backButton", required = false) boolean backButton,
            @RequestParam(name = "showMultiFirmUsers", required = false) boolean showMultiFirmUsers,
            FirmSearchForm firmSearchForm,
            Model model, HttpSession session, Authentication authentication) {

        return SEARCH_PAGE;
    }
}
