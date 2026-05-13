package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.service.RoleChangeNotificationService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class OneTimeTaskController {

    private final RoleChangeNotificationService roleChangeNotificationService;

    private final UserService userService;

    @PostMapping("/ccms-role-sync")
    @PreAuthorize("@accessControlService"
            + ".authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).TRIGGER_CCMS_ROLE_SYNC)")
    public RedirectView oneTimeTasks(RedirectAttributes redirectAttributes) {
        log.info("CCMS Role Sync triggered");
        roleChangeNotificationService.ccmsRoleSync();
        redirectAttributes.addFlashAttribute("successMessage",
                "CCMS Role Sync has been triggered successfully");
        return new RedirectView("/admin/users");
    }

    @PostMapping("/update-profile-status")
    @PreAuthorize("@accessControlService"
            + ".authenticatedUserHasPermission(T(uk.gov.justice.laa.portal.landingpage.entity.Permission).UPDATE_USER_PROFILE_SILAS_STATUS)")
    public RedirectView updateUserProfileSilasStatus(RedirectAttributes redirectAttributes) {
        log.info("Update user profile silas status action triggered");
        userService.updateUserProfileSilasStatus();
        redirectAttributes.addFlashAttribute("successMessage",
                "User profile silas statuses have been updated successfully");
        return new RedirectView("/admin/users");
    }

}
