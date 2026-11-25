package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

/**
 * Maps user deletion API failures to the existing user-facing states.
 */
final class UserDeletionApiErrorHandler {

    private UserDeletionApiErrorHandler() {
    }

    static String handle(UserDataApiClientException exception, String confirmationView,
                         Model model, HttpServletResponse response) {
        return switch (exception.getStatusCode()) {
            case 400 -> {
                model.addAttribute("globalErrorMessage",
                        "The user could not be deleted. Check the deletion reason and try again.");
                yield confirmationView;
            }
            case 401 -> "redirect:/oauth2/authorization/azure";
            case 403 -> "redirect:/not-authorised";
            case 404 -> {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                yield "errors/error-404";
            }
            default -> {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
                model.addAttribute("errorTitle", "Sorry, there is a problem with the service");
                model.addAttribute("errorMessage", "Unable to complete your request. Please try again later.");
                yield "errors/error-generic";
            }
        };
    }
}
