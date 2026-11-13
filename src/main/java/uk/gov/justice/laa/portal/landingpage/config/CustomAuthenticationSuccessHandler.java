package uk.gov.justice.laa.portal.landingpage.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

/**
 * Custom OAuth2 authentication success handler that records the last successful SiLAS login
 * timestamp immediately after successful OAuth2 authentication.
 */
@Component
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginService loginService;
    private final EntraUserRepository entraUserRepository;
    private final UserProfileRepository userProfileRepository;

    public CustomAuthenticationSuccessHandler(LoginService loginService, 
                                               EntraUserRepository entraUserRepository,
                                               UserProfileRepository userProfileRepository) {
        this.loginService = loginService;
        this.entraUserRepository = entraUserRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            if (isEligibleForTimestampRecording(authentication)) {
                loginService.recordSuccessfulLogin(authentication);
            }
        } catch (Exception ex) {
            log.error("Failed to record login timestamp", ex);
        }
        response.sendRedirect("/home");
    }

    private boolean isEligibleForTimestampRecording(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return false;
        }

        return Optional.ofNullable(oauth2User.getAttribute("oid"))
                .map(String::valueOf)
                .flatMap(entraUserRepository::findByEntraOid)
                .filter(this::isUserAndFirmsEligible)
                .isPresent();
    }

    private boolean isUserAndFirmsEligible(EntraUser entraUser) {
        if (!entraUser.isEnabled()) {
            log.debug("User {} is disabled, skipping timestamp recording", entraUser.getId());
            return false;
        }

        List<UserProfile> profiles = userProfileRepository.findAllByEntraUser(entraUser);
        List<UserProfile> externalProfilesWithFirms = profiles.stream()
                .filter(profile -> profile.getUserType() == UserType.EXTERNAL)
                .filter(profile -> profile.getFirm() != null)
                .toList();

        if (!externalProfilesWithFirms.isEmpty()) {
            boolean hasEnabledFirm = externalProfilesWithFirms.stream()
                    .anyMatch(profile -> profile.getFirm().getEnabled());
            
            if (!hasEnabledFirm) {
                log.debug("User {} has only disabled firms, skipping timestamp recording", entraUser.getId());
                return false;
            }
        }

        return true;
    }
}
