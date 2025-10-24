package uk.gov.justice.laa.portal.landingpage.controller;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.view.RedirectView;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;


@ExtendWith(MockitoExtension.class)
public class MultiFirmUserControllerTest {

    private MultiFirmUserController controller;

    @Mock
    private UserService userService;
    @Mock
    private LoginService loginService;
    @Mock
    private Authentication authentication;

    private HttpSession session;
    private Model model;

    @BeforeEach
    public void setUp() {
        ModelMapper mapper = new ModelMapper();
        model = new ExtendedModelMap();
        session = new MockHttpSession();
        controller = new MultiFirmUserController(userService, loginService, mapper);
        enableMultiFirmUser(true);
    }

    private void enableMultiFirmUser(boolean enabled) {
        ReflectionTestUtils.setField(controller, "enableMultiFirmUser", enabled);
    }

    private MultiFirmUserForm createForm() {
        return MultiFirmUserForm.builder().email("test@email.com").build();
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
        assertThat(result).isEqualTo("multi-firm-user/add-profile-start");
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
        assertThat(result).isEqualTo("multi-firm-user/select-user");
        assertSessionAndModelPopulated(model, session);
    }

    @Test
    public void addUserProfile_multiFirmDisabled() {
        enableMultiFirmUser(false);
        RuntimeException rtEx = assertThrows(RuntimeException.class, () -> controller.addUserProfile(model, session));
        assertThat(rtEx.getMessage()).contains("The page you are trying to access is not available.");
    }

    @Test
    public void addUserProfilePost_NotaMultiFirmUser() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);

        Firm userFirm = Firm.builder().name("test").build();
        UserProfile userProfile = UserProfile.builder().firm(userFirm).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(false).email(form.getEmail())
                .userProfiles(Set.of(userProfile)).build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        verify(bindingResult).rejectValue("email", "error.email", "This user cannot be linked to another firm. Ask LAA to enable multi-firm for this user.");
    }

    @Test
    public void addUserProfilePost_AlreadyaMemberOfTheFirm() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);

        Firm userFirm = Firm.builder().name("test").build();
        UserProfile userProfile = UserProfile.builder().firm(userFirm).build();
        EntraUser entraUser = EntraUser.builder().multiFirmUser(true).email(form.getEmail())
                .userProfiles(Set.of(userProfile)).build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        UserProfile adminUserProfile = UserProfile.builder().firm(userFirm).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(adminUserProfile);

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        verify(bindingResult).rejectValue("email", "error.email", "This user already has access for your firm. Manage them from the Manage Your Users screen.");
    }

    @Test
    public void addUserProfilePost_validMultiFirmUser() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);

        Firm userFirm = Firm.builder().name("test").build();
        UserProfile userProfile = UserProfile.builder().firm(userFirm).build();
        EntraUser entraUser = EntraUser.builder().email(form.getEmail())
                .multiFirmUser(true).userProfiles(Set.of(userProfile)).build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        Firm adminFirm = Firm.builder().name("admin firm").build();
        UserProfile adminUserProfile = UserProfile.builder().firm(adminFirm).build();
        when(loginService.getCurrentProfile(authentication)).thenReturn(adminUserProfile);

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("redirect:/admin/users");
        assertThat(model.getAttribute("entraUser")).isNotNull();
        assertThat(session.getAttribute("entraUser")).isNotNull();
    }

    @Test
    public void addUserProfilePost_notMultiFirmUser() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);
        EntraUser entraUser = EntraUser.builder().multiFirmUser(false).email(form.getEmail()).build();
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.of(entraUser));

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        verify(bindingResult).rejectValue("email", "error.email", "This user cannot be linked to another firm. Ask LAA to enable multi-firm for this user.");
        assertSessionAndModelCleared(model, session);
    }

    @Test
    public void addUserProfilePost_userNotFound() {
        MultiFirmUserForm form = createForm();
        BindingResult bindingResult = mockBindingResult(false);
        when(userService.findEntraUserByEmail(form.getEmail())).thenReturn(Optional.empty());

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
        verify(bindingResult).rejectValue("email", "error.email", "We could not find this user. Ask LAA to create the account.");
        assertSessionAndModelCleared(model, session);
    }

    @Test
    public void addUserProfilePost_invalidForm() {
        MultiFirmUserForm form = MultiFirmUserForm.builder().build();
        BindingResult bindingResult = mockBindingResult(true);

        String result = controller.addUserProfilePost(form, bindingResult, model, session, authentication);

        assertThat(result).isEqualTo("multi-firm-user/select-user");
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
                controller.addUserProfilePost(form, bindingResult, model, session, authentication));
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

    @Test
    public void deleteFirmProfileConfirm_shouldReturnConfirmationView() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        java.util.UUID entraUserId = java.util.UUID.randomUUID();
        
        uk.gov.justice.laa.portal.landingpage.dto.FirmDto firmDto = uk.gov.justice.laa.portal.landingpage.dto.FirmDto.builder()
                .name("Test Law Firm")
                .code("12345")
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto.builder()
                .id(entraUserId.toString())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .multiFirmUser(true)
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto.builder()
                .entraUser(entraUserDto)
                .firm(firmDto)
                .activeProfile(true)
                .build();
        
        UserProfile profile1 = UserProfile.builder().build();
        UserProfile profile2 = UserProfile.builder().build();

        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));
        when(userService.getUserProfilesByEntraUserId(entraUserId)).thenReturn(List.of(profile1, profile2));

        // Act
        String result = controller.deleteFirmProfileConfirm(userProfileId, model);

        // Assert
        assertThat(result).isEqualTo("multi-firm-user/delete-profile-confirm");
        assertThat(model.getAttribute("userProfile")).isNotNull();
        assertThat(model.getAttribute("user")).isEqualTo(entraUserDto);
    }

    @Test
    public void deleteFirmProfileConfirm_featureFlagDisabled_shouldThrowException() {
        // Arrange
        enableMultiFirmUser(false);
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                controller.deleteFirmProfileConfirm(userProfileId, model));
    }

    @Test
    public void deleteFirmProfileConfirm_notMultiFirmUser_shouldThrowException() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        
        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto.builder()
                .id(java.util.UUID.randomUUID().toString())
                .multiFirmUser(false) // Not multi-firm
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto.builder()
                .entraUser(entraUserDto)
                .build();

        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                controller.deleteFirmProfileConfirm(userProfileId, model));
    }

    @Test
    public void deleteFirmProfileConfirm_lastProfile_shouldReturnRedirect() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        java.util.UUID entraUserId = java.util.UUID.randomUUID();
        
        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto.builder()
                .id(entraUserId.toString())
                .multiFirmUser(true)
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto.builder()
                .entraUser(entraUserDto)
                .build();

        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));
        when(userService.getUserProfilesByEntraUserId(entraUserId)).thenReturn(List.of(UserProfile.builder().build())); // Only one profile

        // Act
        String result = controller.deleteFirmProfileConfirm(userProfileId, model);

        // Assert
        assertThat(result).isEqualTo("redirect:/admin/users/manage/" + userProfileId);
        assertThat(model.getAttribute("errorMessage")).isNotNull();
    }

    @Test
    public void deleteFirmProfileExecute_withYes_shouldDeleteAndRedirect() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        final String confirm = "yes";
        java.util.UUID actorId = java.util.UUID.randomUUID();
        
        uk.gov.justice.laa.portal.landingpage.dto.FirmDto firmDto = uk.gov.justice.laa.portal.landingpage.dto.FirmDto.builder()
                .name("Test Law Firm")
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto.builder()
                .id(java.util.UUID.randomUUID().toString())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .multiFirmUser(true)
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto.builder()
                .entraUser(entraUserDto)
                .firm(firmDto)
                .build();

        final uk.gov.justice.laa.portal.landingpage.dto.DeleteFirmProfileAuditEvent auditEvent = 
                new uk.gov.justice.laa.portal.landingpage.dto.DeleteFirmProfileAuditEvent(
                        actorId,
                        java.util.UUID.fromString(userProfileId),
                        "john.doe@example.com",
                        "Test Law Firm",
                        "12345",
                        5,
                        2
                );

        uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto currentUserDto = new uk.gov.justice.laa.portal.landingpage.dto.CurrentUserDto();
        currentUserDto.setUserId(actorId);
        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));
        when(loginService.getCurrentUser(authentication)).thenReturn(currentUserDto);
        when(userService.deleteFirmProfile(Mockito.eq(userProfileId), Mockito.eq(actorId))).thenReturn(auditEvent);

        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes = 
                Mockito.mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        // Act
        String result = controller.deleteFirmProfileExecute(userProfileId, confirm, authentication, redirectAttributes, model);

        // Assert
        assertThat(result).isEqualTo("redirect:/admin/users");
        verify(userService).deleteFirmProfile(Mockito.eq(userProfileId), Mockito.eq(actorId));
        verify(redirectAttributes).addFlashAttribute(Mockito.eq("successMessage"), Mockito.anyString());
    }

    @Test
    public void deleteFirmProfileExecute_withNo_shouldRedirectToManageUser() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        String confirm = "no";
        
        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto.builder()
                .id(java.util.UUID.randomUUID().toString())
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto.builder()
                .entraUser(entraUserDto)
                .build();

        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));

        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes = 
                Mockito.mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        // Act
        String result = controller.deleteFirmProfileExecute(userProfileId, confirm, authentication, redirectAttributes, model);

        // Assert
        assertThat(result).isEqualTo("redirect:/admin/users/manage/" + userProfileId);
        verify(userService, Mockito.never()).deleteFirmProfile(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void deleteFirmProfileExecute_withNoSelection_shouldShowError() {
        // Arrange
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        String confirm = null; // No selection
        java.util.UUID entraUserId = java.util.UUID.randomUUID();
        
        uk.gov.justice.laa.portal.landingpage.dto.FirmDto firmDto = uk.gov.justice.laa.portal.landingpage.dto.FirmDto.builder()
                .name("Test Law Firm")
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto entraUserDto = uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto.builder()
                .id(entraUserId.toString())
                .firstName("John")
                .lastName("Doe")
                .multiFirmUser(true)
                .build();
        
        uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto userProfileDto = uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto.builder()
                .entraUser(entraUserDto)
                .firm(firmDto)
                .build();

        when(userService.getUserProfileById(userProfileId)).thenReturn(Optional.of(userProfileDto));

        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes = 
                Mockito.mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        // Act
        String result = controller.deleteFirmProfileExecute(userProfileId, confirm, authentication, redirectAttributes, model);

        // Assert
        assertThat(result).isEqualTo("multi-firm-user/delete-profile-confirm");
        assertThat(model.getAttribute("errorMessage")).isNotNull();
        assertThat(model.getAttribute("errorMessage")).asString().contains("Please select an option");
        verify(userService, Mockito.never()).deleteFirmProfile(Mockito.anyString(), Mockito.any());
    }

    @Test
    public void deleteFirmProfileExecute_featureFlagDisabled_shouldThrowException() {
        // Arrange
        enableMultiFirmUser(false);
        String userProfileId = "123e4567-e89b-12d3-a456-426614174000";
        
        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes = 
                Mockito.mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
                controller.deleteFirmProfileExecute(userProfileId, "yes", authentication, redirectAttributes, model));
    }
}
