package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import uk.gov.justice.laa.portal.landingpage.client.CreateExternalUserResult;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.dto.CreateUserAuditEvent;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmForm;
import uk.gov.justice.laa.portal.landingpage.forms.UserDetailsForm;
import uk.gov.justice.laa.portal.landingpage.service.EmailValidationService;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserCreationApiService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiBackedCreateUserTest {

    @Mock private DataApiFeatureFlagService flags;
    @Mock private ExternalUserCreationApiService api;
    @Mock private UserService userService;
    @Mock private FirmService firmService;
    @Mock private LoginService loginService;
    @Mock private EmailValidationService emailValidationService;
    @Mock private EventService eventService;
    @Mock private Authentication authentication;
    @InjectMocks private UserController controller;
    private MockHttpSession session;
    private ExtendedModelMap model;
    private EntraUserDto user;
    private FirmDto firm;

    @BeforeEach
    void setUp() {
        lenient().when(flags.isCreateUserDataApiEnabled()).thenReturn(true);
        session = new MockHttpSession();
        model = new ExtendedModelMap();
        user = EntraUserDto.builder().email("test@example.org").firstName("Test").lastName("User").build();
        firm = FirmDto.builder().code("00123").name("Firm").build();
        session.setAttribute("user", user);
        session.setAttribute("firm", firm);
        session.setAttribute("isMultiFirmUser", false);
        session.setAttribute("isUserManager", true);
    }

    @Test
    void successUsesApiResultAndNeverCallsLegacyCreationOrFirmLookup() {
        UUID oid = UUID.randomUUID();
        CreateExternalUserResult created = new CreateExternalUserResult(true, oid, UUID.randomUUID());
        when(api.create(user, firm, false)).thenReturn(created);
        when(loginService.getCurrentUser(authentication)).thenReturn(new CurrentUserDto());
        assertThat(controller.addUserCheckAnswers(session, authentication, model))
                .isEqualTo("redirect:/admin/user/create/confirmation");
        assertThat(session.getAttribute("createdUserResult")).isEqualTo(created);
        assertThat(user.getEntraOid()).isEqualTo(oid.toString());
        assertThat(controller.addUserCheckAnswers(session, authentication, model))
                .isEqualTo("redirect:/admin/user/create/confirmation");
        verify(api).create(user, firm, false);
        verify(eventService).logEvent(any(CreateUserAuditEvent.class));
        assertThat(controller.addUserCreated(model, session)).isEqualTo("add-user-created");
        assertThat(model.get("createUserDataApiEnabled")).isEqualTo(true);
        assertThat(session.getAttribute("createdUserResult")).isNull();
        assertThat(session.getAttribute("user")).isNull();
        verifyNoInteractions(userService, firmService);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 409})
    void submissionValidationRetainsJourneyAndNeverShowsSuccess(int status) {
        when(api.create(user, firm, false)).thenThrow(new UserDataApiClientException("RAW PRIVATE PAYLOAD", status));
        assertThat(controller.addUserCheckAnswers(session, authentication, model)).isEqualTo("add-user-check-answers");
        assertThat(model.get("errorMessage").toString()).doesNotContain("RAW", "PRIVATE");
        if (status == 409) {
            assertThat(model.get("errorMessage").toString()).contains("already exists", "Change the email");
        }
        assertThat(model.get("isMultiFirmUser")).isEqualTo(false);
        assertThat(session.getAttribute("user")).isSameAs(user);
        assertThat(session.getAttribute("firm")).isSameAs(firm);
        assertThat(session.getAttribute("createdUserResult")).isNull();
        verifyNoInteractions(eventService, userService, firmService);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 502, 503, 504})
    void upstreamFailuresPropagateToPageHandlerWithoutSuccessState(int status) {
        when(api.create(user, firm, false)).thenThrow(new UserDataApiClientException("upstream", status));
        assertThatThrownBy(() -> controller.addUserCheckAnswers(session, authentication, model))
                .isInstanceOf(UserDataApiClientException.class);
        assertThat(session.getAttribute("createdUserResult")).isNull();
        verifyNoInteractions(eventService, userService, firmService);
    }

    @Test
    void confirmationCannotBeVisitedBeforeSuccessfulApiSubmission() {
        assertThat(controller.addUserCreated(model, session)).isEqualTo("redirect:/admin/user/create/details");
        assertThat(model).doesNotContainKey("user");
    }

    @Test
    void multiFirmSelectionStillSkipsFirmAndMakesNoApiCall() {
        MultiFirmForm form = new MultiFirmForm();
        form.setMultiFirmUser(true);
        assertThat(controller.postUserMultiFirm(form, new BeanPropertyBindingResult(form, "multiFirmForm"), session, model))
                .isEqualTo("redirect:/admin/user/create/check-answers");
        assertThat(session.getAttribute("firm")).isNull();
        verifyNoInteractions(api, userService, firmService);
    }

    @Test
    void cancellationDoesNotCreateAndClearsJourney() {
        assertThat(controller.confirmCancelCreateUser(session, model)).isEqualTo("cancel-add-user-confirmation");
        assertThat(session.getAttribute("user")).isSameAs(user);
        assertThat(controller.cancelUserCreation(session)).isEqualTo("redirect:/admin/users");
        assertThat(session.getAttribute("user")).isNull();
        assertThat(session.getAttribute("firm")).isNull();
        assertThat(session.getAttribute("isUserManager")).isNull();
        verifyNoInteractions(api, userService, firmService, eventService);
    }

    @Test
    void selectedApiCodeSurvivesFirmPageAndBackNavigation() {
        FirmSearchForm form = new FirmSearchForm();
        form.setFirmSearch("Firm");
        form.setSelectedFirmCode("00123");
        when(api.selectFirm("00123", "Firm")).thenReturn(Optional.of(firm));
        assertThat(controller.postUserFirm(form, new BeanPropertyBindingResult(form, "firmSearchForm"), session, model))
                .isEqualTo("redirect:/admin/user/create/check-answers");
        assertThat(session.getAttribute("firm")).isSameAs(firm);
        controller.createUserFirm(new FirmSearchForm(), session, model, 10);
        assertThat(((FirmSearchForm) model.get("firmSearchForm")).getSelectedFirmCode()).isEqualTo("00123");
        verifyNoInteractions(userService, firmService);
    }

    @Test
    void switchingProvidersRequiresReselectingFirmBeforeSubmission() {
        firm.setId(UUID.randomUUID());
        assertThat(controller.addUserCheckAnswers(session, authentication, model))
                .isEqualTo("redirect:/admin/user/create/firm");
        verifyNoInteractions(api, userService, firmService);
    }

    @Test
    void detailsCheckUsesApiAndPreservesProviderAdminAnswer() {
        UserDetailsForm form = details();
        when(api.findByEmail("test@example.org")).thenReturn(Optional.empty());
        when(emailValidationService.isValidEmailDomain("test@example.org")).thenReturn(true);
        assertThat(controller.postUser(form, new BeanPropertyBindingResult(form, "userDetailsForm"), session, model))
                .isEqualTo("redirect:/admin/user/create/multi-firm");
        assertThat(session.getAttribute("isUserManager")).isEqualTo(true);
        verifyNoInteractions(userService);
    }

    @Test
    void detailsBadRequestAppearsInlineWithoutDroppingInput() {
        UserDetailsForm form = details();
        when(api.findByEmail("test@example.org")).thenThrow(new UserDataApiClientException("private", 400));
        when(emailValidationService.isValidEmailDomain("test@example.org")).thenReturn(true);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "userDetailsForm");
        assertThat(controller.postUser(form, errors, session, model)).isEqualTo("add-user-details");
        assertThat(errors.getFieldError("email").getDefaultMessage()).contains("Check the email");
        assertThat(model.get("userDetailsForm")).isSameAs(form);
    }

    private UserDetailsForm details() {
        UserDetailsForm form = new UserDetailsForm();
        form.setEmail("test@example.org");
        form.setFirstName("Test");
        form.setLastName("User");
        form.setUserManager(true);
        return form;
    }
}
