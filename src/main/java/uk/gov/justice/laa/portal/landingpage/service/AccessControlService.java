package uk.gov.justice.laa.portal.landingpage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

import java.util.List;
import java.util.UUID;

@Service
public class AccessControlService {

    private final UserService userService;

    private final FirmService firmService;

    private final LoginService loginService;

    private static final Logger log = LoggerFactory.getLogger(AccessControlService.class);

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

        boolean canAccess = userFirms.stream().map(FirmDto::getId).anyMatch(userManagerFirms::contains);
        if (!canAccess) {
            CurrentUserDto currentUserDto = loginService.getCurrentUser(authentication);
            log.warn("User {} does not have permission to access this userId {}", currentUserDto.getName(), userId);
        }
        return canAccess;
    }

    public boolean canEditUser(String userId) {
        return canAccessUser(userId);
    }

}
