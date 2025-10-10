package uk.gov.justice.laa.portal.landingpage.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class MultiFirmUserControllerTest {

    private MultiFirmUserController controller;

    @Mock
    private UserService userService;

    private HttpSession session;
    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ExtendedModelMap();
        session = new MockHttpSession();
        controller = new MultiFirmUserController(userService);
        enableMultiFirmUser(true);
    }

    private void enableMultiFirmUser(boolean enabled) {
        ReflectionTestUtils.setField(controller, "enableMultiFirmUser", enabled);
    }

    private MultiFirmUserForm createForm(String email) {
        return MultiFirmUserForm.builder().email(email).build();
    }

    private BindingResult mockBindingResult(boolean hasErrors) {
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        when(bindingResult.hasErrors()).thenReturn(hasErrors);
        return bindingResult;
    }

    private void assertSessionAndModelCleared(Model model, HttpSession session) {
        assertThat(model.getAttribute("multiFirmUserForm")).isNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNull();
        assertThat(model.getAttribute("entraUser")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    private void assertSessionAndModelPopulated(Model model, HttpSession session) {
        assertThat(model.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNotNull();
    }

    @Test
    public void addUserProfileStart_shouldReturnViewName() {
        String result = controller.addUserProfileStart();
        assertThat(result).isEqualTo("add-multi-firm-user-profile-start");
    }

    @Test
    public void addUserProfileStart_multiFirmDisabled() {
        enableMultiFirmUser(false);
        RuntimeException rtEx = assertThrows(RuntimeException.class, () -> controller.addUserProfileStart());
        assertThat(rtEx.getMessage()).contains("The page you are trying to access is not available.");
    }

    @Test
    public void addUserProfile() {
        String result = controller.addUserProfile(model, session);
        assertThat(result).isEqualTo("add-multi-firm-user-profile");
        assertSessionAndModelPopulated(model, session);
    }

    @Test
    public void addUserProfile_multiFirmDisabled() {
        enableMultiFirmUser(false);
        RuntimeException rtEx = assertThrows(RuntimeException.class, () -> controller.addUserProfile(model, session));
        assertThat(rtEx.getMessage()).contains("The page you are trying to access is not available.");
    }

    @Test
    public void addUserProfilePost_validMultiFirmUser() {
        MultiFirmUserForm form = createForm("test@email.com");
        BindingResult bindingResult = mockBindingResult(false);
        EntraUserDto entraUser = EntraUserDto.builder().multiFirmUser(true).email(form.getEmail()).build();
        when(userService.getEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        String result = controller.addUserProfilePost(form, bindingResult, model, session);

        assertThat(result).isEqualTo("redirect:/admin/users");
        assertThat(model.getAttribute("entraUser")).isNotNull();
        assertThat(session.getAttribute("entraUser")).isNotNull();
    }

    @Test
    public void addUserProfilePost_notMultiFirmUser() {
        MultiFirmUserForm form = createForm("test@email.com");
        BindingResult bindingResult = mockBindingResult(false);
        EntraUserDto entraUser = EntraUserDto.builder().multiFirmUser(false).email(form.getEmail()).build();
        when(userService.getEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        String result = controller.addUserProfilePost(form, bindingResult, model, session);

        assertThat(result).isEqualTo("add-multi-firm-user-profile");
        assertSessionAndModelCleared(model, session);
    }

    @Test
    public void addUserProfilePost_userNotFound() {
        MultiFirmUserForm form = createForm("test@email.com");
        BindingResult bindingResult = mockBindingResult(false);
        when(userService.getEntraUserByEmail(form.getEmail())).thenReturn(Optional.empty());

        String result = controller.addUserProfilePost(form, bindingResult, model, session);

        assertThat(result).isEqualTo("add-multi-firm-user-profile");
        assertSessionAndModelCleared(model, session);
    }

    @Test
    public void addUserProfilePost_invalidForm() {
        MultiFirmUserForm form = MultiFirmUserForm.builder().build();
        BindingResult bindingResult = mockBindingResult(true);

        String result = controller.addUserProfilePost(form, bindingResult, model, session);

        assertThat(result).isEqualTo("add-multi-firm-user-profile");
        assertSessionAndModelPopulated(model, session);
        assertThat(model.getAttribute("entraUser")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    public void addUserProfilePost_multiFirmDisabled() {
        enableMultiFirmUser(false);
        MultiFirmUserForm form = MultiFirmUserForm.builder().build();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        RuntimeException rtEx = assertThrows(RuntimeException.class, () ->
                controller.addUserProfilePost(form, bindingResult, model, session));
        assertThat(rtEx.getMessage()).contains("The page you are trying to access is not available.");
    }

    @Test
    public void cancelUserProfileCreation_shouldClearAllSessionAttributes() {
        MockHttpSession testSession = new MockHttpSession();
        testSession.setAttribute("entraUser", new EntraUserDto());
        testSession.setAttribute("multiFirmUserForm", new MultiFirmUserForm());

        String view = controller.cancelUserProfileCreation(testSession);

        assertThat(view).isEqualTo("redirect:/admin/users");
        assertThat(testSession.getAttribute("user")).isNull();
        assertThat(testSession.getAttribute("userProfile")).isNull();
        assertThat(testSession.getAttribute("firm")).isNull();
        assertThat(testSession.getAttribute("isFirmAdmin")).isNull();
        assertThat(testSession.getAttribute("apps")).isNull();
        assertThat(testSession.getAttribute("roles")).isNull();
        assertThat(testSession.getAttribute("officeData")).isNull();
    }

    @Test
    public void handleAuthorizationException_withAuthorizationDeniedException_redirectsToNotAuthorized() {
        // Arrange
        MockHttpSession mockSession = new MockHttpSession();
        AuthorizationDeniedException authException = new AuthorizationDeniedException("Access denied");
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Act
        RedirectView result = controller.handleAuthorizationException(authException, mockSession, request);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void handleAuthorizationException_withAccessDeniedException_redirectsToNotAuthorized() {
        // Arrange
        MockHttpSession mockSession = new MockHttpSession();
        AccessDeniedException accessException = new AccessDeniedException("Access denied");
        MockHttpServletRequest request = new MockHttpServletRequest();

        // Act
        RedirectView result = controller.handleAuthorizationException(accessException, mockSession, request);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void handleAuthorizationException_logsWarningMessage() {
        // Arrange
        MockHttpSession mockSession = new MockHttpSession();
        AuthorizationDeniedException authException = new AuthorizationDeniedException("Test access denied");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/admin/users");

        Logger logger = (Logger) LoggerFactory.getLogger(MultiFirmUserController.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            // Act
            controller.handleAuthorizationException(authException, mockSession, request);

            // Assert
            List<ILoggingEvent> logsList = listAppender.list;
            assertThat(logsList).hasSize(1);
            ILoggingEvent log = logsList.getFirst();

            assertThat(log.getLevel()).isEqualTo(Level.WARN);
            assertThat(log.getMessage()).isEqualTo(
                    "Authorization denied while accessing user: reason='{}', method='{}', uri='{}', referer='{}', savedRequest='{}'"
            );
            assertThat(log.getArgumentArray()).containsExactly("Test access denied", "GET", "/admin/users", null, null);
        } finally {
            logger.detachAppender(listAppender);
        }
    }

    @Test
    public void whenHandleException_thenRedirectToErrorPage() {

        // Arrange & Act
        RedirectView result = controller.handleException(new Exception());

        // Assert
        assertThat(result.getUrl()).isEqualTo("/error");
    }
}
