package uk.gov.justice.laa.portal.landingpage.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthenticatedUser {

    public Optional<CurrentUserDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User principal = oauthToken.getPrincipal();

            String name = principal.getAttribute("name");
            UUID userId = UUID.fromString(Objects.requireNonNull(principal.getAttribute("oid")));
            CurrentUserDto currentUserDto = new CurrentUserDto();
            currentUserDto.setName(name);
            currentUserDto.setUserId(userId);
            return Optional.of(currentUserDto);
        }

        return Optional.empty();
    }

    public EntraUser getCurrentEntraUser(UserService userService) {
        CurrentUserDto currentUserDto = getCurrentUser().orElseThrow();
        EntraUser entraUser = userService.getUserByEntraId(currentUserDto.getUserId());
        assert entraUser != null;
        return entraUser;
    }
}
