package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

import static org.assertj.core.api.Assertions.assertThat;

class UserDeletionApiErrorHandlerTest {

    @Test
    void handle_whenBadRequestShowsValidationMessageOnConfirmationPage() {
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = UserDeletionApiErrorHandler.handle(
                new UserDataApiClientException("internal response", 400),
                "delete-user-check-answer", model, response);

        assertThat(view).isEqualTo("delete-user-check-answer");
        assertThat(model.getAttribute("globalErrorMessage"))
                .isEqualTo("The user could not be deleted. Check the deletion reason and try again.");
        assertThat(model.asMap().toString()).doesNotContain("internal response");
    }

    @Test
    void handle_whenUnauthorizedRedirectsToNotAuthorisedPage() {
        String view = UserDeletionApiErrorHandler.handle(
                new UserDataApiClientException("forbidden", 403),
                "delete-user-check-answer", new ExtendedModelMap(), new MockHttpServletResponse());

        assertThat(view).isEqualTo("redirect:/not-authorised");
    }

    @Test
    void handle_whenNotFoundShowsExistingNotFoundPage() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = UserDeletionApiErrorHandler.handle(
                new UserDataApiClientException("missing", 404),
                "delete-user-check-answer", new ExtendedModelMap(), response);

        assertThat(view).isEqualTo("errors/error-404");
        assertThat(response.getStatus()).isEqualTo(404);
    }

    @Test
    void handle_whenApiFailsShowsGenericErrorWithoutExposingDetails() {
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletResponse response = new MockHttpServletResponse();

        String view = UserDeletionApiErrorHandler.handle(
                new UserDataApiClientException("raw API payload", 503),
                "delete-user-check-answer", model, response);

        assertThat(view).isEqualTo("errors/error-generic");
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(model.getAttribute("errorMessage"))
                .isEqualTo("Unable to complete your request. Please try again later.");
        assertThat(model.asMap().toString()).doesNotContain("raw API payload");
    }
}
