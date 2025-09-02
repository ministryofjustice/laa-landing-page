package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping(value = {"/", "/login", "/css/style.css", "/js/script.js", "/assets/image.png", "/actuator/health"})
    public String publicEndpoint() {
        return "public";
    }

    @GetMapping("/secure")
    public String secureEndpoint() {
        return "secure";
    }

    @PostMapping("/secure")
    public String securePostEndpoint() {
        return "secure-post";
    }

    @GetMapping("/admin/dashboard")
    public String adminEndpoint() {
        return "admin";
    }

    @GetMapping("/admin/users/manage")
    public String adminManageUserEndpoint() {
        return "manage-user";
    }

    @GetMapping("/admin/user/create")
    public String adminCreateUserEndpoint() {
        return "create-user";
    }

    @PostMapping({"/api/v1/claims/enrich"})
    public String claimsEnrichEndpoint() {
        return "claims-enrich";
    }
}
