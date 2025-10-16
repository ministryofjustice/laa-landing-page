package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import lombok.RequiredArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.dto.SwitchProfileAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.UserFirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

/**
 * Controller for handling firm-related requests, including firm switching for multi-firm users.
 */
@Controller
@RequiredArgsConstructor
public class FirmController {

    private static final Logger logger = LoggerFactory.getLogger(FirmController.class);
    
    private final LoginService loginService;
    private final UserService userService;
    private final EventService eventService;

    /**
     * Displays the switch firm page with a list of firms available to the user.
     * Only accessible to multi-firm users.
     *
     * @param message optional message to display (e.g., success or error messages)
     * @param search optional search query
     * @param sort optional sort field
     * @param direction optional sort direction
     * @param model the model to populate with firm data
     * @param authentication the authentication object containing user credentials
     * @return the switch-firm view if user is a multi-firm user, otherwise redirects to home
     */
    @GetMapping("/switch-firm")
    public String switchFirm(
            @RequestParam(name = "message", required = false) String message,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "direction", required = false) String direction,
            Model model,
            Authentication authentication) {

        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);
        if (Objects.nonNull(entraUser) && entraUser.isMultiFirmUser()) {
            // Fetch all user profiles and convert them to UserFirmDtos
            List<UserFirmDto> userFirmList = entraUser.getUserProfiles().stream()
                    .map(userProfile -> UserFirmDto.builder()
                            .userProfileId(userProfile.getId())
                            .isActiveProfile(userProfile.isActiveProfile())
                            .firmId(userProfile.getFirm().getId())
                            .firmName(userProfile.getFirm().getName())
                            .firmCode(userProfile.getFirm().getCode())
                            .firmType(userProfile.getFirm().getType() != null 
                                    ? userProfile.getFirm().getType().getValue() 
                                    : null)
                            .build())
                    .toList();
            
            // Apply sorting if requested
            if (Objects.nonNull(sort) && Objects.nonNull(direction)) {
                userFirmList = sortUserFirmList(userFirmList, sort, direction);
            }
            
            model.addAttribute("userFirmList", userFirmList);
            model.addAttribute("sort", sort);
            model.addAttribute("direction", direction);
            model.addAttribute("search", search);
            model.addAttribute(ModelAttributes.PAGE_TITLE, "Switch firm");
            if (Objects.nonNull(message)) {
                model.addAttribute("message", message);
            }
            return "switch-firm";
        }
        return "redirect:/home";
    }
    
    /**
     * Sorts the user firm list based on the specified field and direction.
     *
     * @param list the list to sort
     * @param sortField the field to sort by
     * @param direction the sort direction (asc or desc)
     * @return the sorted list
     */
    private List<UserFirmDto> sortUserFirmList(List<UserFirmDto> list, String sortField, String direction) {
        boolean ascending = "asc".equalsIgnoreCase(direction);
        
        return list.stream()
                .sorted((a, b) -> {
                    int comparison = 0;
                    
                    switch (sortField) {
                        case "firmName":
                            comparison = compareNullable(a.getFirmName(), b.getFirmName());
                            break;
                        case "firmCode":
                            comparison = compareNullable(a.getFirmCode(), b.getFirmCode());
                            break;
                        case "firmType":
                            comparison = compareNullable(a.getFirmType(), b.getFirmType());
                            break;
                        case "activeProfile":
                            comparison = Boolean.compare(a.isActiveProfile(), b.isActiveProfile());
                            break;
                        default:
                            comparison = 0;
                    }
                    
                    return ascending ? comparison : -comparison;
                })
                .toList();
    }
    
    /**
     * Compares two nullable strings.
     *
     * @param a the first string
     * @param b the second string
     * @return comparison result
     */
    private int compareNullable(String a, String b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.compareToIgnoreCase(b);
    }

    /**
     * Handles the firm switching action when a user submits the form.
     * Changes the active firm profile for the user and logs the event.
     *
     * @param firmId the ID of the firm to switch to
     * @param authentication the authentication object containing user credentials
     * @return redirects back to switchfirm page with appropriate message
     * @throws IOException if an error occurs during the switch
     */
    @PostMapping("/switch-firm")
    public RedirectView switchFirm(@RequestParam("firmid") String firmId, Authentication authentication)
            throws IOException {
        EntraUser user = loginService.getCurrentEntraUser(authentication);
        String message = "";
        
        if (Objects.nonNull(user) && user.isMultiFirmUser()) {
            UserProfile up = user.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst()
                    .orElse(null);
            String oldFirm = "";
            
            if (Objects.nonNull(up)) {
                oldFirm = up.getFirm().getId().toString();
                
                if (oldFirm.equals(firmId)) {
                    message = "Can not switch to the same Firm";
                    logger.warn("User {} attempted to switch to the same firm: {}", user.getId(), firmId);
                    return new RedirectView("/switch-firm?message=" + message);
                }
                
                userService.setDefaultActiveProfile(user, UUID.fromString(firmId));
                SwitchProfileAuditEvent auditEvent = new SwitchProfileAuditEvent(user.getId(), oldFirm, firmId);
                eventService.logEvent(auditEvent);
                message = "Switch firm successful";
                logger.info("User {} successfully switched from firm {} to firm {}", user.getId(), oldFirm, firmId);
            }
        } else {
            message = "Apply to multi firm user only";
            logger.warn("Non-multi-firm user attempted to switch firms: {}", user != null ? user.getId() : "null");
        }
        
        return new RedirectView("/switch-firm?message=" + message);
    }
}
