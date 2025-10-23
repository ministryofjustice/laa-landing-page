package uk.gov.justice.laa.portal.landingpage.controller.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class MultFirmFeatureFlagInterceptor implements HandlerInterceptor {

    @Value("${feature.flag.enable.multi.firm.user}")
    private boolean enableMultiFirmUser;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
        if (!enableMultiFirmUser) {
            log.warn("Multi Firm User functionality is disabled. Access denied to the requested resource: {}", request.getRequestURI());
            throw new AccessDeniedException("Multi Firm User functionality is disabled.");
        }
        return true;
    }

}
