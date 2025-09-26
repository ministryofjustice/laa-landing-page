package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

/**
 * Controller for handling static page requests like cookies, privacy policy, etc.
 */
@Controller
public class StaticPagesController {

    @GetMapping("/cookies")
    public String cookies(Model model) {
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Cookies");
        return "footer/cookies";
    }

}
