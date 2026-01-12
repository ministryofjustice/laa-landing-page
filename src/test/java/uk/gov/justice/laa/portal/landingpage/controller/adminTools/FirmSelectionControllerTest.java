package uk.gov.justice.laa.portal.landingpage.controller.adminTools;

import jakarta.servlet.http.HttpSession;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.controller.FirmController;
import uk.gov.justice.laa.portal.landingpage.controller.MultiFirmUserController;
import uk.gov.justice.laa.portal.landingpage.controller.UserController;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;
import uk.gov.justice.laa.portal.landingpage.forms.MultiFirmUserForm;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmSelectionControllerTest {

    public static final String ADMIN_TOOLS_SELECT_USER = "admin-tools/select-user";
    public static final String REDIRECT_ADMIN_FIRM_SELECTION_SELECT_FIRM = "redirect:/adminFirmSelection/selectFirm";
    public static final String ADMIN_USERS = "/admin/users";
    private FirmSelectionController firmSelectionController;

    @Mock
    UserService userService;

    @Mock
    LoginService loginService;

    @Mock
    EventService eventService;

    @Mock
    FirmService firmService;

    @Mock
    BindingResult bindingResult;

    @Mock
    Authentication authentication;

    private Model model;

    private HttpSession session;

    private MultiFirmUserForm multiFirmUserForm;

    private ModelMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MapperConfig().modelMapper();
        firmSelectionController = new FirmSelectionController(userService, loginService, eventService,  mapper, firmService);
        model = new ExtendedModelMap();
        session = new MockHttpSession();
        multiFirmUserForm = MultiFirmUserForm.builder()
                .email("test@test.com")
                .build();
    }

    @Test
    void selectUserGetWithoutInformationFromSession() {
        //Act
        String view = firmSelectionController.selectUserGet(model, session, authentication);
        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);
        assertThat(model.getAttribute("multiFirmUserForm")).isEqualTo(new MultiFirmUserForm());
        assertThat(model.getAttribute("email")).isEqualTo(null);
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Add profile");
    }

    @Test
    void selectUserGetWithInformationFromSession() {
        //Arrange
        session.setAttribute("multiFirmUserForm", multiFirmUserForm);

        //act
        String view = firmSelectionController.selectUserGet(model, session, authentication);

        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);
        assertThat(model.getAttribute("multiFirmUserForm")).isEqualTo(multiFirmUserForm);
        assertThat(model.getAttribute("email")).isEqualTo(multiFirmUserForm.getEmail());
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Add profile");
    }

    @Test
    void selectUserPostWithoutError() {
        //Arrange
        Optional<EntraUser> entraUser = getEntraUser(true, null);
        EntraUserDto entraUserDtoResult = mapper.map(entraUser, EntraUserDto.class);

        when(userService.findEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(entraUser);

        //act
        String view = firmSelectionController.selectUserPost(multiFirmUserForm, bindingResult, model, session, authentication);

        // Assert
        assertThat(view).isEqualTo(REDIRECT_ADMIN_FIRM_SELECTION_SELECT_FIRM);
        assertThat(model.getAttribute("entraUser")).isEqualTo(entraUserDtoResult);
        assertThat(session.getAttribute("entraUser")).isEqualTo(entraUserDtoResult);
        assertThat(session.getAttribute("multiFirmUserForm")).isEqualTo(multiFirmUserForm);
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo(String.format("Add profile - %s", entraUserDtoResult.getFullName()));
    }

    @Test
    void selectUserPostWithValidationError() {
        //Arrange
        when(bindingResult.hasErrors()).thenReturn(true);

        //act
        String view = firmSelectionController.selectUserPost(multiFirmUserForm, bindingResult, model, session, authentication);

        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);

        assertThat(model.getAttribute("multiFirmUserForm")).isEqualTo(multiFirmUserForm);
        assertThat(model.getAttribute("backUrl")).isEqualTo(ADMIN_USERS);
        assertThat(session.getAttribute("multiFirmUserForm")).isEqualTo(multiFirmUserForm);
    }

    @Test
    void selectUserPostWithErrorWhenUserIsNotMultiFirm() {
        //Arrange
        Optional<EntraUser> entraUser = getEntraUser(false, null);
        when(userService.findEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(entraUser);

        //act
        String view = firmSelectionController.selectUserPost(multiFirmUserForm, bindingResult, model, session, authentication);

        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);
        assertThat(session.getAttribute("multiFirmUserForm")).isEqualTo(multiFirmUserForm);
        assertThat(model.getAttribute("multiFirmUserForm")).isEqualTo(multiFirmUserForm);

        assertThat(model.getAttribute("backUrl")).isEqualTo(ADMIN_USERS);
        verify(bindingResult).rejectValue("email",
                "error.email",
                "This user cannot be added at this time. Contact your Contract Manager to check their access permissions.");
    }

    @Test
    void selectUserPostWithErrorWhenTheUserAlreadyHasAccessForTheFirmWithoutSession() {
        //Arrange
        Firm firm = Firm.builder()
                .name("Firm1")
                .build();

        Optional<EntraUser> entraUser = getEntraUser(true, firm);

        when(userService.findEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(entraUser);

        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder()
                        .firm(firm)
                .build());

        //act
        String view = firmSelectionController.selectUserPost(multiFirmUserForm, bindingResult, model, session, authentication);

        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);
        assertThat(session.getAttribute("multiFirmUserForm")).isEqualTo(multiFirmUserForm);
        assertThat(model.getAttribute("multiFirmUserForm")).isEqualTo(multiFirmUserForm);

        assertThat(model.getAttribute("backUrl")).isEqualTo(ADMIN_USERS);
        verify(bindingResult).rejectValue("email",
                "error.email",
                "This user already has a profile for this firm. You can amend their access from the Manage your users table.");
    }


    @Test
    void selectFirmGet() {
    }

    @Test
    void selectFirmPost() {
    }

    @Test
    void checkAnswerGet() {
    }

    @Test
    void checkAnswerPost() {
    }

    @Test
    void confirmation() {
    }

    @Test
    void cancel() {
    }

    @Test
    void handleAuthorizationException() {
    }

    @Test
    void handleException() {
    }

    private static Optional<EntraUser> getEntraUser(boolean isMultiFirm, Firm firm) {
        return Optional.ofNullable(EntraUser.builder()
                .multiFirmUser(isMultiFirm)
                .email("test@test.com")
                .firstName("test")
                .lastName("test")
                        .userProfiles(Set.of(UserProfile.builder()
                                        .firm(firm)
                                .build()))
                .build());
    }
}