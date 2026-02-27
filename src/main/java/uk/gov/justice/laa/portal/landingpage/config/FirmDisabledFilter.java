package uk.gov.justice.laa.portal.landingpage.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

/**
 * Filter to block access for users whose firm has been disabled.
 * This is applied after OAuth authentication and UserDisabledFilter.
 *
 * EXTERNAL users: Access is blocked if their firm is disabled (contract gap)
 * INTERNAL users: Not affected by firm disabled status (may not have a firm)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FirmDisabledFilter extends OncePerRequestFilter {

    private final EntraUserRepository entraUserRepository;
    private final UserProfileRepository userProfileRepository;
    private final LoginService loginService;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth instanceof OAuth2AuthenticationToken) {
            CurrentUserDto currentUserDto = loginService.getCurrentUser(auth);
            if (currentUserDto != null) {
                // Check if user has an EXTERNAL profile with a disabled firm
                Optional<EntraUser> entraUserOpt = entraUserRepository.findByEntraOid(currentUserDto.getUserId().toString());

                if (entraUserOpt.isPresent()) {
                    EntraUser entraUser = entraUserOpt.get();
                    List<UserProfile> profiles = userProfileRepository.findAllByEntraUser(entraUser);

                    // Check if any EXTERNAL user profile has a disabled firm
                    boolean hasDisabledFirm = profiles.stream()
                        .filter(profile -> profile.getUserType() == UserType.EXTERNAL)
                        .filter(profile -> profile.getFirm() != null)
                        .anyMatch(profile -> !profile.getFirm().getEnabled());

                    if (hasDisabledFirm) {
                        log.warn("Access blocked for user {} - firm is disabled", currentUserDto.getUserId());
                        SecurityContextHolder.clearContext();
                        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                            "Access denied - your firm is temporarily disabled due to contract status");
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
