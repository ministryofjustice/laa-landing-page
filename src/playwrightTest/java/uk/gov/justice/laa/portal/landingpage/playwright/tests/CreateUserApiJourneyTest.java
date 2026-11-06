package uk.gov.justice.laa.portal.landingpage.playwright.tests;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.portal.landingpage.client.CreateExternalUserResult;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.playwright.common.BaseFrontEndTest;
import uk.gov.justice.laa.portal.landingpage.playwright.common.TestUser;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserCreationApiService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateUserApiJourneyTest extends BaseFrontEndTest {

    @MockitoBean private DataApiFeatureFlagService flags;
    @MockitoBean private ExternalUserCreationApiService api;
    @MockitoBean private EmailValidationService emailValidation;

    @BeforeEach
    void configureApi() {
        when(flags.isCreateUserDataApiEnabled()).thenReturn(true);
        when(api.findByEmail(anyString())).thenReturn(Optional.empty());
        when(emailValidation.isValidEmailDomain(anyString())).thenReturn(true);
        loginAs(TestUser.GLOBAL_ADMIN.email);
        page.navigate("http://localhost:" + port + "/admin/user/create/details");
        page.locator("#email").fill("api-browser-user@example.org");
        page.locator("#firstName").fill("Browser");
        page.locator("#lastName").fill("User");
        page.locator("input[name=userManager][value=true]").check();
        assertThat(page.getByText("This answer does not grant access.", new Page.GetByTextOptions().setExact(false))).isVisible();
        continueJourney();
        page.locator("input[name=multiFirmUser][value=false]").check();
        continueJourney();
        assertThat(page.locator("input[name=selectedFirmCode]")).hasCount(1);
    }

    @Test
    void selectsStringFirmCodeAndShowsApiConfirmationWithDeferredAccess() {
        FirmDto firm = FirmDto.builder().code("00123").name("API Test Firm").build();
        when(api.searchFirms(anyString(), anyInt())).thenReturn(List.of(firm));
        when(api.selectFirm("00123", "API Test Firm")).thenReturn(Optional.of(firm));
        when(api.create(any(), any(), anyBoolean())).thenReturn(
                new CreateExternalUserResult(true, UUID.randomUUID(), UUID.randomUUID()));
        page.locator("#firmSearch").fill("API");
        page.getByRole(AriaRole.OPTION).first().click();
        assertThat(page.locator("#selectedFirmId")).hasValue("00123");
        continueJourney();
        assertThat(page.getByText("Requested access", new Page.GetByTextOptions().setExact(true))).isVisible();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create new user")).click();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("User created"))).isVisible();
        assertThat(page.getByText("No roles or office access have been assigned.", new Page.GetByTextOptions().setExact(false)))
                .isVisible();
    }

    @Test
    void searchBadRequestShowsTopOfScreenNonTechnicalValidation() {
        when(api.searchFirms(anyString(), anyInt())).thenThrow(new UserDataApiClientException("RAW PRIVATE", 400));
        page.locator("#firmSearch").fill("API");
        assertThat(page.locator("#firm-search-error")).isVisible();
        assertThat(page.locator("#firm-search-error-message")).hasText("Check the firm search and try again.");
        assertThat(page.locator("#firm-search-error")).isFocused();
        assertThat(page.locator("body")).not().containsText("RAW PRIVATE");
        assertThat(page).hasURL("http://localhost:" + port + "/admin/user/create/firm");
        verify(api, never()).create(any(), any(), anyBoolean());
    }

    @Test
    void autocompleteDebouncesTypingAndClearsThePreviousSelection() {
        FirmDto firm = FirmDto.builder().code("00123").name("API Test Firm").build();
        when(api.searchFirms(anyString(), anyInt())).thenReturn(List.of(firm));
        page.waitForResponse(response -> response.url().contains("/firm/search?q=API&"), () -> {
            page.locator("#firmSearch").fill("A");
            page.locator("#firmSearch").fill("AP");
            page.locator("#firmSearch").fill("API");
        });
        verify(api).searchFirms("API", 10);
        verify(api, never()).searchFirms("A", 10);
        verify(api, never()).searchFirms("AP", 10);
        page.getByRole(AriaRole.OPTION).first().click();
        assertThat(page.locator("#selectedFirmId")).hasValue("00123");
        page.locator("#firmSearch").fill("Different firm");
        assertThat(page.locator("#selectedFirmId")).hasValue("");
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 404, 502, 503, 504})
    void searchFailuresNavigateTheWholePageWithoutTechnicalDetails(int status) {
        when(api.searchFirms(anyString(), anyInt())).thenThrow(new UserDataApiClientException("RAW PRIVATE", status));
        page.locator("#firmSearch").fill("API");
        String path = switch (status) {
            case 403 -> "/not-authorised";
            case 404 -> "/admin/user/create/firm/error?status=404";
            default -> "/admin/user/create/firm/error";
        };
        assertThat(page).hasURL("http://localhost:" + port + path);
        assertThat(page.locator("body")).not().containsText("RAW PRIVATE");
        verify(api, never()).create(any(), any(), anyBoolean());
    }

    @Test
    void expiredTokenNavigatesTopLevelToReauthentication() {
        when(api.searchFirms(anyString(), anyInt())).thenThrow(new ClientAuthorizationRequiredException("azure"));
        page.route("**/oauth2/authorization/azure", route -> route.fulfill(
                new Route.FulfillOptions().setContentType("text/html").setBody("<h1>Sign in</h1>")));
        page.locator("#firmSearch").fill("API");
        assertThat(page).hasURL("http://localhost:" + port + "/oauth2/authorization/azure");
        assertThat(page.getByRole(AriaRole.HEADING)).hasText("Sign in");
    }

    private void continueJourney() {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Continue").setExact(true)).click();
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }
}
