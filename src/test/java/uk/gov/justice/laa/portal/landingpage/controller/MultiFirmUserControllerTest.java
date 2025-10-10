package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

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
        ReflectionTestUtils.setField(controller, "enableMultiFirmUser", true);
    }

    @Test
    public void addUserProfileStart_shouldReturnViewName() {
        String result = controller.addUserProfileStart();
        assertThat(result).isEqualTo("add-multi-firm-user-profile-start");
    }

    @Test
    public void addUserProfileStart_multiFirmDisabled() {
        ReflectionTestUtils.setField(controller, "enableMultiFirmUser", false);
        RuntimeException rtEx = assertThrows(RuntimeException.class, () -> controller.addUserProfileStart());
        assertThat(rtEx.getMessage()).contains("The page you are trying to access is not available.");
    }

    @Test
    public void addUserProfile() {
        String result = controller.addUserProfile(model, session);
        assertThat(result).isEqualTo("add-multi-firm-user-profile");
        assertThat(model.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNotNull();
    }

    @Test
    public void addUserProfile_multiFirmDisabled() {
        ReflectionTestUtils.setField(controller, "enableMultiFirmUser", false);
        RuntimeException rtEx = assertThrows(RuntimeException.class, () -> controller.addUserProfile(model, session));
        assertThat(rtEx.getMessage()).contains("The page you are trying to access is not available.");
    }

    @Test
    public void addUserProfilePost() {
        MultiFirmUserForm multiFirmUserForm = MultiFirmUserForm.builder().email("test@email.com").build();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        EntraUserDto entraUserDto = EntraUserDto.builder().multiFirmUser(true).email("test@email.com").build();

        when(userService.getEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(Optional.of(entraUserDto));

        String result = controller.addUserProfilePost(multiFirmUserForm, bindingResult, model, session);

        assertThat(result).isEqualTo("redirect:/admin/users");
        assertThat(model.getAttribute("entraUser")).isNotNull();
        assertThat(session.getAttribute("entraUser")).isNotNull();
    }

    @Test
    public void addUserProfilePost_NotA_MultiFirmUser() {
        MultiFirmUserForm multiFirmUserForm = MultiFirmUserForm.builder().email("test@email.com").build();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        EntraUserDto entraUserDto = EntraUserDto.builder().multiFirmUser(false).email("test@email.com").build();

        when(userService.getEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(Optional.of(entraUserDto));

        String result = controller.addUserProfilePost(multiFirmUserForm, bindingResult, model, session);

        assertThat(result).isEqualTo("add-multi-firm-user-profile");
        assertThat(model.getAttribute("multiFirmUserForm")).isNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNull();
        assertThat(model.getAttribute("entraUser")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    public void addUserProfilePost_UserNotFound() {
        MultiFirmUserForm multiFirmUserForm = MultiFirmUserForm.builder().email("test@email.com").build();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        when(userService.getEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(Optional.empty());

        String result = controller.addUserProfilePost(multiFirmUserForm, bindingResult, model, session);

        assertThat(result).isEqualTo("add-multi-firm-user-profile");
        assertThat(model.getAttribute("multiFirmUserForm")).isNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNull();
        assertThat(model.getAttribute("entraUser")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    public void addUserProfilePost_InvalidMultiFirmUserForm() {
        MultiFirmUserForm multiFirmUserForm = MultiFirmUserForm.builder().build();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);

        when(bindingResult.hasErrors()).thenReturn(true);

        String result = controller.addUserProfilePost(multiFirmUserForm, bindingResult, model, session);

        assertThat(result).isEqualTo("add-multi-firm-user-profile");
        assertThat(model.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(session.getAttribute("multiFirmUserForm")).isNotNull();
        assertThat(model.getAttribute("entraUser")).isNull();
        assertThat(session.getAttribute("entraUser")).isNull();
    }

    @Test
    public void addUserProfilePost_multiFirmDisabled() {
        MultiFirmUserForm multiFirmUserForm = MultiFirmUserForm.builder().build();
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        ReflectionTestUtils.setField(controller, "enableMultiFirmUser", false);
        RuntimeException rtEx = assertThrows(RuntimeException.class, () -> controller.addUserProfilePost(multiFirmUserForm, bindingResult, model, session));
        assertThat(rtEx.getMessage()).contains("The page you are trying to access is not available.");
    }
}
