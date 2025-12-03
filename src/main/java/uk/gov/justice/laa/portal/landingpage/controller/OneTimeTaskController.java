package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.service.RoleChangeNotificationService;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class OneTimeTaskController {

    private final RoleChangeNotificationService roleChangeNotificationService;

    @RequestMapping("/ccms-role-sync")
    @PreAuthorize("@accessControlService"
            + ".authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).TRIGGER_CCMS_SYNC)")
    public RedirectView oneTimeTasks(RedirectAttributes redirectAttributes) {
        log.info("CCMS Role Sync triggered");
        roleChangeNotificationService.ccmsRoleSync();
        redirectAttributes.addFlashAttribute("successMessage",
                "CCMS Role Sync has been triggered successfully");
        return new RedirectView("/admin/users");
    }
}
