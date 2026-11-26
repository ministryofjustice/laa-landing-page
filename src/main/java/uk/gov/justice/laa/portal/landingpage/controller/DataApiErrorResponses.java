package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

/**
 * Maps laa-data-user-api failures to user-facing responses so every data API journey behaves the same.
 * Raw API details are logged only, never shown.
 */
@Slf4j
final class DataApiErrorResponses {

    private static final String GENERIC_ERROR_VIEW = "errors/error-generic";

    private DataApiErrorResponses() {
    }

    /**
     * Sends the user to sign in again when the access token for a data API call is missing, expired or rejected.
     */
    static RedirectView reauthenticate(ClientAuthorizationRequiredException ex) {
        log.warn("OAuth2 authorization required for client: {}. Redirecting to re-authenticate.",
                ex.getClientRegistrationId());
        return new RedirectView("/oauth2/authorization/" + ex.getClientRegistrationId());
    }

    /**
     * 403 goes to the not-authorised page and 404 to a not-found page. A 404 on a form submission has no page
     * to go back to, so it shows the given message instead. Anything else shows the generic error page.
     * A 401 is expected to have been turned into a re-authentication before reaching here.
     */
    static ModelAndView toErrorView(UserDataApiClientException ex, HttpServletRequest request,
                                    String postNotFoundTitle, String postNotFoundMessage) {
        int status = ex.getStatusCode();
        log.warn("Data API call failed for {} {}: status={}", request.getMethod(), request.getRequestURI(), status);

        if (status == 403) {
            return new ModelAndView(new RedirectView("/not-authorised"));
        }
        if (status == 404) {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                return errorPage(HttpStatus.NOT_FOUND, postNotFoundTitle, postNotFoundMessage);
            }
            ModelAndView notFound = new ModelAndView("errors/error-404");
            notFound.setStatus(HttpStatus.NOT_FOUND);
            return notFound;
        }
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "Sorry, there is a problem with the service",
                "Unable to complete your request. Please try again later.");
    }

    private static ModelAndView errorPage(HttpStatus status, String title, String message) {
        ModelAndView mav = new ModelAndView(GENERIC_ERROR_VIEW);
        mav.setStatus(status);
        mav.addObject("errorTitle", title);
        mav.addObject("errorMessage", message);
        return mav;
    }
}
