package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUserApiExceptionHandlerTest {

    private final CreateUserApiExceptionHandler handler = new CreateUserApiExceptionHandler();

    @ParameterizedTest
    @CsvSource({"401,/oauth2/authorization/azure", "403,/not-authorised",
        "404,/admin/user/create/firm/error?status=404", "502,/admin/user/create/firm/error",
        "503,/admin/user/create/firm/error", "504,/admin/user/create/firm/error"})
    void autocompleteFailuresGiveSafeNavigationAndNeverSuccessfulEmptyResults(int status, String redirect) {
        Object response = handler.handleApiError(new UserDataApiClientException("RAW PRIVATE", status),
                new MockHttpServletRequest("GET", "/admin/user/create/firm/search"));
        assertThat(response).isInstanceOfSatisfying(ResponseEntity.class, entity -> {
            assertThat(entity.getStatusCode().value()).isEqualTo(status);
            assertThat(entity.getBody().toString()).contains(redirect).doesNotContain("RAW", "PRIVATE");
        });
    }

    @Test
    void autocompleteBadRequestProvidesNonTechnicalValidationMessage() {
        Object response = handler.handleApiError(new UserDataApiClientException("RAW PRIVATE", 400),
                new MockHttpServletRequest("GET", "/admin/user/create/firm/search"));
        assertThat(response).isInstanceOfSatisfying(ResponseEntity.class, entity -> {
            assertThat(entity.getStatusCode().value()).isEqualTo(400);
            assertThat(entity.getBody().toString()).contains("Check the firm search").doesNotContain("RAW");
        });
    }

    @ParameterizedTest
    @CsvSource({"404,404", "502,500", "503,500", "504,500"})
    void htmlErrorsShowExistingErrorPageWithNoRawDetails(int upstream, int expected) {
        Object response = handler.handleApiError(new UserDataApiClientException("RAW PRIVATE", upstream),
                new MockHttpServletRequest("POST", "/admin/user/create/check-answers"));
        assertThat(response).isInstanceOfSatisfying(ModelAndView.class, page -> {
            assertThat(page.getViewName()).isEqualTo("errors/error-generic");
            assertThat(page.getStatus().value()).isEqualTo(expected);
            assertThat(page.getModel().toString()).doesNotContain("RAW", "PRIVATE");
        });
    }

    @ParameterizedTest
    @CsvSource({"401,/oauth2/authorization/azure", "403,/not-authorised"})
    void htmlAuthenticationErrorsRedirect(int status, String url) {
        Object response = handler.handleApiError(new UserDataApiClientException("private", status),
                new MockHttpServletRequest("POST", "/admin/user/create/details"));
        assertThat(response).isInstanceOfSatisfying(RedirectView.class, view -> assertThat(view.getUrl()).isEqualTo(url));
    }

    @Test
    void expiredTokenOnAutocompleteDoesNotRedirectFetchToLoginHtml() {
        Object response = handler.reauthenticate(new ClientAuthorizationRequiredException("azure"),
                new MockHttpServletRequest("GET", "/admin/user/create/firm/search"));
        assertThat(response).isInstanceOfSatisfying(ResponseEntity.class, entity -> {
            assertThat(entity.getStatusCode().value()).isEqualTo(401);
            assertThat(entity.getBody().toString()).contains("/oauth2/authorization/azure");
        });
    }
}
