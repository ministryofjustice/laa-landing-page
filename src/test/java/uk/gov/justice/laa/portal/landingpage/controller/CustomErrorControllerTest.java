package uk.gov.justice.laa.portal.landingpage.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;

import jakarta.servlet.RequestDispatcher;

@ExtendWith(MockitoExtension.class)
class CustomErrorControllerTest {

    @InjectMocks
    private CustomErrorController customErrorController;

    private Model model;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        model = new BindingAwareModelMap();
        request = new MockHttpServletRequest();
    }

    @Test
    void handleError_404_returnsNotFoundPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-404", viewName);
        assertEquals("404", model.getAttribute("errorType"));
        assertEquals("Page not found", model.getAttribute("errorTitle"));
        assertEquals("The page you were looking for could not be found.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_403_returnsForbiddenPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.FORBIDDEN.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-403", viewName);
        assertEquals("403", model.getAttribute("errorType"));
        assertEquals("Access forbidden", model.getAttribute("errorTitle"));
        assertEquals("You don't have permission to access this page.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_500_returnsInternalServerErrorPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-500", viewName);
        assertEquals("500", model.getAttribute("errorType"));
        assertEquals("Sorry, there is a problem with the service", model.getAttribute("errorTitle"));
        assertEquals("Try again later.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_otherStatusCode_returnsGenericErrorPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.BAD_REQUEST.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-generic", viewName);
        assertEquals("400", model.getAttribute("errorType"));
        assertEquals("Sorry, there is a problem with the service", model.getAttribute("errorTitle"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_noStatusCode_returnsGenericErrorPage() {
        // No status code attribute set
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-generic", viewName);
        assertEquals("Unknown", model.getAttribute("errorType"));
        assertEquals("Sorry, there is a problem with the service", model.getAttribute("errorTitle"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_401_returnsGenericErrorPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.UNAUTHORIZED.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-generic", viewName);
        assertEquals("401", model.getAttribute("errorType"));
        assertEquals("Sorry, there is a problem with the service", model.getAttribute("errorTitle"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_405_returnsGenericErrorPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.METHOD_NOT_ALLOWED.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-generic", viewName);
        assertEquals("405", model.getAttribute("errorType"));
        assertEquals("Sorry, there is a problem with the service", model.getAttribute("errorTitle"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_502_returnsGenericErrorPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.BAD_GATEWAY.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-generic", viewName);
        assertEquals("502", model.getAttribute("errorType"));
        assertEquals("Sorry, there is a problem with the service", model.getAttribute("errorTitle"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_503_returnsGenericErrorPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.SERVICE_UNAVAILABLE.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-generic", viewName);
        assertEquals("503", model.getAttribute("errorType"));
        assertEquals("Sorry, there is a problem with the service", model.getAttribute("errorTitle"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }

    @Test
    void handleError_504_returnsGenericErrorPage() {
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.GATEWAY_TIMEOUT.value());
        
        String viewName = customErrorController.handleError(request, model);
        
        assertEquals("errors/error-generic", viewName);
        assertEquals("504", model.getAttribute("errorType"));
        assertEquals("Sorry, there is a problem with the service", model.getAttribute("errorTitle"));
        assertEquals("An unexpected error occurred.", model.getAttribute("errorMessage"));
    }
}
