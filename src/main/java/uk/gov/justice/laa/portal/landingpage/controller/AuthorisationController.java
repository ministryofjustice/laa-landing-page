package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

/**
 * Controller for handling authorisation-related pages
 */
@Controller
public class AuthorisationController {

    /**
     * Display the not authorised page
     */
    @GetMapping("/not-authorised")
    public String notAuthorised(Model model) {
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Not Authorised");
        return "not-authorised";
    }
}
