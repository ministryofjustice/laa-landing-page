package uk.gov.justice.laa.portal.landingpage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Exception resolver that handles OAuth2 client authorization exceptions.
 * This resolver catches exceptions that occur during method parameter resolution,
 * which @ExceptionHandler in @ControllerAdvice might not catch.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OAuth2ExceptionResolver implements HandlerExceptionResolver {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2ExceptionResolver.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
                                          Object handler, Exception ex) {
        if (ex instanceof ClientAuthorizationRequiredException) {
            ClientAuthorizationRequiredException authEx = (ClientAuthorizationRequiredException) ex;
            logger.warn("OAuth2 authorization required for client: {}. Redirecting to re-authenticate.", 
                    authEx.getClientRegistrationId());
            
            // Redirect to OAuth2 authorization endpoint
            String redirectUrl = "/oauth2/authorization/" + authEx.getClientRegistrationId();
            ModelAndView mav = new ModelAndView();
            mav.setViewName("redirect:" + redirectUrl);
            return mav;
        }
        
        // Return null to let other exception resolvers handle the exception
        return null;
    }
}
