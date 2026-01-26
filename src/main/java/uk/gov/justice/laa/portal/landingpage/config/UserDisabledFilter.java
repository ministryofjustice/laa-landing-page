package uk.gov.justice.laa.portal.landingpage.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class UserDisabledFilter extends OncePerRequestFilter {

    private final EntraUserRepository userRepository;
    private final LoginService loginService;

    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain)
            throws ServletException, IOException {


        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            CurrentUserDto currentUserDto = loginService.getCurrentUser(auth);
            if (currentUserDto != null) {
                boolean disabled = userRepository
                        .existsByEntraOidAndDisabledTrue(currentUserDto.getUserId().toString()); // optimized query

                if (disabled) {
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }

        }

        filterChain.doFilter(request, response);
    }
}

