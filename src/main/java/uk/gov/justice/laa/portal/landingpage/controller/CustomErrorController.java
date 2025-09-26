package uk.gov.justice.laa.portal.landingpage.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

/**
 * Custom error controller to handle HTTP errors and provide appropriate error pages
 */
@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

    /**
     * Handle all error requests and route to appropriate error page
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        log.debug("Error occurred - Status: {}, URI: {}, Exception: {}", status, requestUri, 
                exception != null ? exception.getClass().getSimpleName() : "None");

        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

            switch (httpStatus) {
                case NOT_FOUND:
                    model.addAttribute(ModelAttributes.PAGE_TITLE, "Page not found");
                    model.addAttribute("errorType", "404");
                    model.addAttribute("errorTitle", "Page not found");
                    model.addAttribute("errorMessage", "The page you were looking for could not be found.");
                    return "errors/error-404";

                case FORBIDDEN:
                    model.addAttribute(ModelAttributes.PAGE_TITLE, "Access forbidden");
                    model.addAttribute("errorType", "403");
                    model.addAttribute("errorTitle", "Access forbidden");
                    model.addAttribute("errorMessage", "You don't have permission to access this page.");
                    return "errors/error-403";

                case INTERNAL_SERVER_ERROR:
                    model.addAttribute(ModelAttributes.PAGE_TITLE, "Internal server error");
                    model.addAttribute("errorType", "500");
                    model.addAttribute("errorTitle", "Sorry, there is a problem with the service");
                    model.addAttribute("errorMessage", "Try again later.");
                    return "errors/error-500";

                default:
                    // For other HTTP status codes, use the generic error page
                    model.addAttribute(ModelAttributes.PAGE_TITLE, "Error");
                    model.addAttribute("errorType", statusCode.toString());
                    model.addAttribute("errorTitle", "Sorry, there is a problem with the service");
                    model.addAttribute("errorMessage", "An unexpected error occurred.");
                    return "errors/error-generic";
            }
        }

        // Default fallback if no status code is found
        model.addAttribute(ModelAttributes.PAGE_TITLE, "Error");
        model.addAttribute("errorType", "Unknown");
        model.addAttribute("errorTitle", "Sorry, there is a problem with the service");
        model.addAttribute("errorMessage", "An unexpected error occurred.");
        return "errors/error-generic";
    }
}
