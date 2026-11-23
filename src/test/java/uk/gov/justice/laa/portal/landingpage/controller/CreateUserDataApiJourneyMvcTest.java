package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import uk.gov.justice.laa.datauserapi.contracts.response.CreateUserAccountResponse;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.config.UiLabelsProperties;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.AppRoleService;
import uk.gov.justice.laa.portal.landingpage.service.AppService;
import uk.gov.justice.laa.portal.landingpage.service.CreateUserApiService;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.NotificationService;
import uk.gov.justice.laa.portal.landingpage.service.OfficeService;
import uk.gov.justice.laa.portal.landingpage.service.RoleAssignmentService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;
import uk.gov.justice.laa.portal.landingpage.service.UserReactivationRequestService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;
import uk.gov.justice.laa.portal.landingpage.utils.StatusTagClassUtil;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Drives the create user journey through Spring MVC with the data API flag on and the API stubbed,
 * so form binding, routing and exception handler selection are exercised together.
 * Method-level security is not applied in standalone mode and is covered by the integration tests.
 */
@ExtendWith(MockitoExtension.class)
class CreateUserDataApiJourneyMvcTest {

    private static final String BASE = "/admin/user/create";

    @Mock
    private LoginService loginService;
    @Mock
    private UserService userService;
    @Mock
    private OfficeService officeService;
    @Mock
    private EventService eventService;
    @Mock
    private FirmService firmService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RoleAssignmentService roleAssignmentService;
    @Mock
    private EmailValidationService emailValidationService;
    @Mock
    private AppRoleService appRoleService;
    @Mock
    private AppService appService;
    @Mock
    private UserAccountStatusService userAccountStatusService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserReactivationRequestService userReactivationRequestService;
    @Mock
    private UiLabelsProperties uiLabelsProperties;
    @Mock
    private DataApiFeatureFlagService dataApiFeatureFlagService;
    @Mock
    private CreateUserApiService createUserApiService;
    private final Principal principal = new TestingAuthenticationToken("user", "password");
    private MockMvc mockMvc;
    private MockHttpSession session;
    private final UUID firmId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(loginService, userService, officeService, eventService,
                firmService, new MapperConfig().modelMapper(), accessControlService, roleAssignmentService,
                emailValidationService, appRoleService, appService, userAccountStatusService, notificationService,
                userReactivationRequestService, uiLabelsProperties, new StatusTagClassUtil(),
                dataApiFeatureFlagService, createUserApiService);
        FirmSearchController firmSearchController = new FirmSearchController(loginService, firmService,
                accessControlService, dataApiFeatureFlagService, createUserApiService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController, firmSearchController)
                .setViewResolvers(new InternalResourceViewResolver("/", ".html"))
                .build();
        session = new MockHttpSession();
        lenient().when(dataApiFeatureFlagService.isCreateUserDataApiEnabled()).thenReturn(true);
        lenient().when(emailValidationService.isValidEmailDomain(anyString())).thenReturn(true);
    }

    private void givenCompletedJourney() {
        session.setAttribute("user", EntraUserDto.builder().email("a@b.com").firstName("First")
                .lastName("Last").build());
        session.setAttribute("isUserManager", false);
        session.setAttribute("isMultiFirmUser", false);
        session.setAttribute("firm", FirmDto.builder().id(firmId).name("Firm").code("111").build());
        lenient().when(loginService.getCurrentUser(any())).thenReturn(new CurrentUserDto());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder submitCheckAnswers() {
        return post(BASE + "/check-answers").session(session).principal(principal);
    }

    @Test
    void journeyFromDetailsToConfirmationCreatesUserThroughTheApiOnly() throws Exception {
        when(createUserApiService.findUserByEmail("a@b.com")).thenReturn(Optional.empty());
        mockMvc.perform(post(BASE + "/details").session(session)
                        .param("firstName", "First").param("lastName", "Last")
                        .param("email", "a@b.com").param("userManager", "false"))
                .andExpect(redirectedUrl(BASE + "/multi-firm"));

        mockMvc.perform(post(BASE + "/multi-firm").session(session).param("multiFirmUser", "true"))
                .andExpect(redirectedUrl(BASE + "/firm"));

        FirmDto firm = FirmDto.builder().id(firmId).name("Firm").code("111").build();
        when(firmService.getFirm(firmId)).thenReturn(firm);
        mockMvc.perform(post(BASE + "/firm").session(session)
                        .param("firmSearch", "Firm").param("selectedFirmId", firmId.toString()))
                .andExpect(redirectedUrl(BASE + "/check-answers"));

        when(loginService.getCurrentUser(any())).thenReturn(new CurrentUserDto());
        when(createUserApiService.createUser(any(EntraUserDto.class), eq(firm), eq(true)))
                .thenReturn(new CreateUserAccountResponse(true, "ok", UUID.randomUUID().toString(), UUID.randomUUID()));
        mockMvc.perform(submitCheckAnswers()).andExpect(redirectedUrl(BASE + "/confirmation"));

        verify(userService, never()).createUser(any(), any(), anyBoolean(), anyString(), anyBoolean());
        verify(userService, never()).userExistsByEmail(anyString());
        verify(eventService).logEvent(any());
    }

    @Test
    void detailsPostRejectsExistingEmailFoundByTheApi() throws Exception {
        session.setAttribute("createUserDetailsModel", new org.springframework.ui.ExtendedModelMap());
        when(createUserApiService.findUserByEmail("a@b.com")).thenReturn(Optional.of(
                new uk.gov.justice.laa.datauserapi.contracts.response.UserAccountSummaryView(
                        UUID.randomUUID().toString(), "a@b.com", "F", "L", null, false, false, null, null, null, 1)));

        mockMvc.perform(post(BASE + "/details").session(session)
                        .param("firstName", "First").param("lastName", "Last")
                        .param("email", "a@b.com").param("userManager", "false"))
                .andExpect(view().name("add-user-details"))
                .andExpect(model().attributeHasFieldErrors("userDetailsForm", "email"));
    }

    @Test
    void conflictReturnsToDetailsShowsEmailErrorOnceAndCancelClearsIt() throws Exception {
        givenCompletedJourney();
        when(createUserApiService.createUser(any(), any(), anyBoolean()))
                .thenThrow(new UserDataApiClientException("Data API returned status 409", 409));

        mockMvc.perform(submitCheckAnswers()).andExpect(redirectedUrl(BASE + "/details"));

        mockMvc.perform(get(BASE + "/details").session(session))
                .andExpect(view().name("add-user-details"))
                .andExpect(model().attributeHasFieldErrors("userDetailsForm", "email"));
        mockMvc.perform(get(BASE + "/details").session(session))
                .andExpect(model().attributeHasNoErrors("userDetailsForm"));

        mockMvc.perform(submitCheckAnswers()).andExpect(redirectedUrl(BASE + "/details"));
        mockMvc.perform(get(BASE + "/cancel").session(session));
        givenCompletedJourney();
        mockMvc.perform(get(BASE + "/details").session(session))
                .andExpect(model().attributeHasNoErrors("userDetailsForm"));
    }

    @Test
    void serviceFailuresShowTheGenericErrorPage() throws Exception {
        givenCompletedJourney();
        for (int status : new int[] {502, 503, 504}) {
            doThrow(new UserDataApiClientException("raw https://internal", status))
                    .when(createUserApiService).createUser(any(), any(), anyBoolean());

            mockMvc.perform(submitCheckAnswers())
                    .andExpect(status().isInternalServerError())
                    .andExpect(view().name("errors/error-generic"));
        }
        verify(eventService, never()).logEvent(any());
    }

    @Test
    void forbiddenRedirectsToNotAuthorised() throws Exception {
        givenCompletedJourney();
        when(createUserApiService.createUser(any(), any(), anyBoolean()))
                .thenThrow(new UserDataApiClientException("x", 403));

        mockMvc.perform(submitCheckAnswers()).andExpect(redirectedUrl("/not-authorised"));
    }

    @Test
    void expiredTokenRedirectsToSignInInsteadOfTheGeneralErrorPage() throws Exception {
        givenCompletedJourney();
        when(createUserApiService.createUser(any(), any(), anyBoolean()))
                .thenThrow(new ClientAuthorizationRequiredException("azure"));

        mockMvc.perform(submitCheckAnswers()).andExpect(redirectedUrl("/oauth2/authorization/azure"));
    }

    @Test
    void apiFirmSearchReturnsSuggestions() throws Exception {
        when(createUserApiService.searchFirms("Firm", 10))
                .thenReturn(List.of(FirmDto.builder().id(firmId).name("Firm A").code("111").build()));

        mockMvc.perform(get(BASE + "/firm/api-search").param("q", "Firm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(firmId.toString()))
                .andExpect(jsonPath("$[0].name").value("Firm A"));
    }

    @Test
    void apiFirmSearchAsksTheScriptToSignInAgainWhenTheTokenHasExpired() throws Exception {
        when(createUserApiService.searchFirms(anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new ClientAuthorizationRequiredException("azure"));

        mockMvc.perform(get(BASE + "/firm/api-search").param("q", "Firm"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.redirectUrl").value("/oauth2/authorization/azure"));
    }

    @Test
    void apiFirmSearchIsNotAvailableWhenTheFlagIsOff() throws Exception {
        when(dataApiFeatureFlagService.isCreateUserDataApiEnabled()).thenReturn(false);

        mockMvc.perform(get(BASE + "/firm/api-search").param("q", "Firm"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        verifyNoInteractions(createUserApiService);
    }

    @Test
    void sharedFirmSearchStillUsesTheExistingServiceWhenTheFlagIsOn() throws Exception {
        when(accessControlService.authenticatedUserIsInternal()).thenReturn(true);
        when(firmService.searchFirms("Firm")).thenReturn(List.of(
                FirmDto.builder().id(firmId).name("Firm A").code("111").build()));

        mockMvc.perform(get(BASE + "/firm/search").param("q", "Firm").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("111"));
        verifyNoInteractions(createUserApiService);
        assertThat(session.getAttributeNames().hasMoreElements()).isFalse();
    }
}
