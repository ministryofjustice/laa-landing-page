package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

import java.util.Map;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(assignableTypes = {UserController.class, FirmSearchController.class})
public class CreateUserApiExceptionHandler {

    @ExceptionHandler(UserDataApiClientException.class)
    public Object handleApiError(UserDataApiClientException exception, HttpServletRequest request) {
        int status = exception.getStatusCode();
        log.warn("Create-user API failure: status={}", status, exception);
        if (isSearch(request)) {
            if (status == 400) {
                return ResponseEntity.badRequest().body(Map.of("message", "Check the firm search and try again."));
            }
            String redirect = switch (status) {
                case 401 -> "/oauth2/authorization/azure";
                case 403 -> "/not-authorised";
                case 404 -> "/admin/user/create/firm/error?status=404";
                default -> "/admin/user/create/firm/error";
            };
            int responseStatus = status >= 400 && status <= 599 ? status : 502;
            return ResponseEntity.status(responseStatus).body(Map.of("redirect", redirect));
        }
        return switch (status) {
            case 401 -> new RedirectView("/oauth2/authorization/azure");
            case 403 -> new RedirectView("/not-authorised");
            default -> errorPage(status);
        };
    }

    @ExceptionHandler(ClientAuthorizationRequiredException.class)
    public Object reauthenticate(ClientAuthorizationRequiredException exception, HttpServletRequest request) {
        log.info("Create-user API requires reauthentication");
        String redirect = "/oauth2/authorization/" + exception.getClientRegistrationId();
        if (isSearch(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("redirect", redirect));
        }
        return new RedirectView(redirect);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public Object tokenExchangeFailure(HttpClientErrorException exception, HttpServletRequest request) {
        return handleApiError(new UserDataApiClientException("Token exchange failed",
                exception.getStatusCode().value(), exception), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object searchAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        if (!isSearch(request)) {
            throw exception;
        }
        log.warn("Create-user firm search permission denied", exception);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("redirect", "/not-authorised"));
    }

    public static ModelAndView errorPage(int status) {
        ModelAndView page = new ModelAndView("errors/error-generic");
        page.setStatus(status == 404 ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR);
        page.addObject("errorTitle", status == 404 ? "Resource not found" : "Sorry, there is a problem with the service");
        page.addObject("errorMessage", status == 404
                ? "The requested information could not be found. Go back to the create-user journey and check your selection."
                : "Unable to complete your request. Please try again later.");
        return page;
    }

    private boolean isSearch(HttpServletRequest request) {
        return request.getRequestURI().equals("/admin/user/create/firm/search");
    }
}
