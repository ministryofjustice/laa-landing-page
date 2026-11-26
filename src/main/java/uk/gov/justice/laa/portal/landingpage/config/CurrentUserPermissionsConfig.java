package uk.gov.justice.laa.portal.landingpage.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ModelAndViewDefiningException;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UrlPathHelper;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserPermissions;
import uk.gov.justice.laa.portal.landingpage.service.CurrentUserPermissionsService;

import java.util.List;

@Configuration
@ConditionalOnExpression("${feature.flag.current-user-permissions:false} or ${feature.flag.manage-users.data-api:false}")
@Slf4j
public class CurrentUserPermissionsConfig implements WebMvcConfigurer, HandlerInterceptor {

    private static final String MODEL_ATTRIBUTE = "userDataPermissions";
    private final CurrentUserPermissionsService permissionsService;

    public CurrentUserPermissionsConfig(CurrentUserPermissionsService permissionsService,
                                        @Value("${SPRING_SESSION_JDBC_ENABLED:false}") boolean jdbcSessionsEnabled) {
        Assert.state(jdbcSessionsEnabled,
                "Current-user permissions require SPRING_SESSION_JDBC_ENABLED=true");
        this.permissionsService = permissionsService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this).addPathPatterns("/home", "/admin/users", "/admin/users/manage/*",
                "/admin/users/manage/*/assign-roles");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws ModelAndViewDefiningException {
        if (!HttpMethod.GET.matches(request.getMethod()) && !HttpMethod.HEAD.matches(request.getMethod())) {
            return true;
        }
        boolean home = "/home".equals(UrlPathHelper.defaultInstance.getPathWithinApplication(request));
        try {
            request.setAttribute(MODEL_ATTRIBUTE,
                    permissionsService.load(request, SecurityContextHolder.getContext().getAuthentication(), !home));
        } catch (UserDataApiClientException e) {
            int status = e.getStatusCode();
            log.warn("Current-user permissions could not be loaded: status={}", status);
            if (home) {
                request.setAttribute(MODEL_ATTRIBUTE, new CurrentUserPermissions(List.of()));
                return true;
            }
            ModelAndView view;
            if (status == 403) {
                view = new ModelAndView(new RedirectView("/not-authorised", true));
            } else if (status == 401) {
                // A fresh login was already attempted; require user action rather than redirecting again.
                view = new ModelAndView("errors/error-403");
                view.setStatus(HttpStatus.FORBIDDEN);
            } else {
                view = new ModelAndView("errors/error-generic");
                view.setStatus(HttpStatus.SERVICE_UNAVAILABLE);
                view.addObject("errorTitle", "Sorry, there is a problem with the service");
                view.addObject("errorMessage", "Unable to complete your request. Please try again later.");
            }
            throw new ModelAndViewDefiningException(view);
        } catch (RuntimeException e) {
            if (!home) {
                throw e;
            }
            log.error("Current-user permissions could not be loaded on home: exceptionType={}",
                    e.getClass().getName());
            request.setAttribute(MODEL_ATTRIBUTE, new CurrentUserPermissions(List.of()));
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView view) {
        Object permissions = request.getAttribute(MODEL_ATTRIBUTE);
        if (permissions != null && view != null && view.getViewName() != null
                && !view.getViewName().startsWith("redirect:")) {
            view.addObject(MODEL_ATTRIBUTE, permissions);
        }
    }
}
