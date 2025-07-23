package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

import java.util.List;
import java.util.UUID;

@Service
public class AccessControlService {

    private final UserService userService;

    private final FirmService firmService;

    private final LoginService loginService;

    public AccessControlService(UserService userService, LoginService loginService, FirmService firmService) {
        this.userService = userService;
        this.loginService = loginService;
        this.firmService = firmService;
    }

    public boolean canAccessUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        EntraUser entraUser = loginService.getCurrentEntraUser(authentication);

        // If internal user, allow access
        if (userService.isInternal(entraUser)) {
            return true;
        }

        List<UUID> userManagerFirms = firmService.getUserAllFirms(entraUser).stream().map(FirmDto::getId).toList();

        List<FirmDto> userFirms = firmService.getUserFirmsByUserId(userId);

        return userFirms.stream().map(FirmDto::getId).anyMatch(userManagerFirms::contains);
    }

    public boolean canEditUser(String userId) {
        return canAccessUser(userId);
    }

}
