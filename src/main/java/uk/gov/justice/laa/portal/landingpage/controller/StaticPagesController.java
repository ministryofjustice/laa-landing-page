package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

/**
 * Controller for handling static page requests like cookies, privacy policy, etc.
 */
@Controller
public class StaticPagesController {

    @Value("${app.external-identity-service-link}")
    private String externalIdentityServiceLink;

    @GetMapping("/cookies")
    public String cookies(Model model) {
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Cookies");
        return "footer/cookies";
    }

    @GetMapping("/accessibility")
    public String accessibility(Model model) {
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Accessibility");
        return "footer/accessibility";
    }

    @GetMapping("/start")
    public String start(Model model) {
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Start");
        model.addAttribute("externalIdentityServiceLink", externalIdentityServiceLink);
        return "start";
    }
}
