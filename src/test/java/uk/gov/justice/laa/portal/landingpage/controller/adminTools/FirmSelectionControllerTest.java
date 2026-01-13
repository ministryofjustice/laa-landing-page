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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import uk.gov.justice.laa.portal.landingpage.config.MapperConfig;
import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;
import uk.gov.justice.laa.portal.landingpage.controller.FirmController;
import uk.gov.justice.laa.portal.landingpage.controller.MultiFirmUserController;
import uk.gov.justice.laa.portal.landingpage.controller.UserController;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FirmSelectionControllerTest {

    public static final String ADMIN_TOOLS_SELECT_USER = "admin-tools/select-user";
    public static final String REDIRECT_ADMIN_FIRM_SELECTION_SELECT_FIRM = "redirect:/adminFirmSelection/selectFirm";
    public static final String ADMIN_USERS = "/admin/users";
    public static final String ADMIN_TOOLS_ADD_USER_FIRM = "admin-tools/add-user-firm";
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
    private FirmSearchForm firmSearchForm;

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
        firmSearchForm = FirmSearchForm.builder()
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
        assertThat(model.getAttribute("backUrl")).isEqualTo(ADMIN_USERS);
        verify(bindingResult).rejectValue("email",
                "error.email",
                "This user already has a profile for this firm. You can amend their access from the Manage your users table.");
    }

    @Test
    void selectUserPostWithErrorWhenTheUserAlreadyHasAccessForTheFirmWithSession() {
        //Arrange
        UUID firmId = UUID.randomUUID();
        Firm firm = Firm.builder()
                .name("Firm1")
                .id(firmId)
                .build();
        Optional<EntraUser> entraUser = getEntraUser(true, firm);
        when(userService.findEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(entraUser);
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder()
                .firm(firm)
                .build());
        when(firmService.getById(firmId)).thenReturn(firm);

        session.setAttribute("delegateTargetFirmId", firmId.toString());

        //act
        String view = firmSelectionController.selectUserPost(multiFirmUserForm, bindingResult, model, session, authentication);
        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);
        assertThat(model.getAttribute("backUrl")).isEqualTo(ADMIN_USERS);
        verify(bindingResult).rejectValue("email",
                "error.email",
                "This user already has a profile for this firm. You can amend their access from the Manage your users table.");
    }

    @Test
    void selectUserPostWithErrorWhenTheUserParentFirm() {
        //Arrange
        UUID firmId = UUID.randomUUID();
        UUID differentFirmId = UUID.randomUUID();
        Firm firm = Firm.builder()
                .name("Parent firm")
                .id(firmId)
                .build();

        Firm differentFirm = Firm.builder()
                .name("Child firm")
                .id(firmId)
                .build();

        Firm otherFirm = Firm.builder()
                .name("otherFirm")
                .id(differentFirmId)
                .parentFirm(firm)
                .build();

        Optional<EntraUser> entraUser = getEntraUserProfileWithParents(firm, differentFirm);
        when(userService.findEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(entraUser);
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder()
                .firm(firm)
                .build());
        when(firmService.getById(differentFirmId)).thenReturn(otherFirm);

        session.setAttribute("delegateTargetFirmId", differentFirmId.toString());

        //act
        String view = firmSelectionController.selectUserPost(multiFirmUserForm, bindingResult, model, session, authentication);
        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);
        assertThat(model.getAttribute("backUrl")).isEqualTo(ADMIN_USERS);
        verify(bindingResult).rejectValue("email",
                "error.email",
                "This user already belongs to a parent firm in this hierarchy and cannot be assigned to a child firm.");
    }

    @Test
    void selectUserPostWithErrorWhenTheUserChildrenFirm() {
        //Arrange
        UUID firmId = UUID.randomUUID();
        Firm children = Firm.builder()
                .name("Child firm")
                .id(firmId)
                .build();

        Firm firmParent = Firm.builder()
                .name("Parent firm")
                .id(firmId)
                .childFirms(Set.of(children))
                .parentFirm(Firm.builder()
                        .name("Parent firm")
                        .id(firmId)
                        .build())
                .build();

        Optional<EntraUser> entraUser = getEntraUser(true, firmParent);
        when(userService.findEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(entraUser);
        when(loginService.getCurrentProfile(authentication)).thenReturn(UserProfile.builder()
                .firm(firmParent)
                .build());
        when(firmService.getById(children.getId())).thenReturn(children);
        session.setAttribute("delegateTargetFirmId", children.getId().toString());

        //act
        String view = firmSelectionController.selectUserPost(multiFirmUserForm, bindingResult, model, session, authentication);
        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);
        assertThat(model.getAttribute("backUrl")).isEqualTo(ADMIN_USERS);
        verify(bindingResult).rejectValue("email",
                "error.email",
                "This user already belongs to a child firm in this hierarchy and cannot be assigned to a parent firm.");
    }

    @Test
    void selectUserPostWithErrorWhenNotFoundUser() {
        //Arrange
        when(userService.findEntraUserByEmail(multiFirmUserForm.getEmail())).thenReturn(Optional.empty());
        //act
        String view = firmSelectionController.selectUserPost(multiFirmUserForm, bindingResult, model, session, authentication);
        // Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_SELECT_USER);
        assertThat(model.getAttribute("backUrl")).isEqualTo(ADMIN_USERS);
        verify(bindingResult).rejectValue("email",
                "error.email",
                "We could not find this user. Try again or ask the Legal Aid Agency to create a new account for them.");
    }


    @Test
    void selectFirmGetWithoutSessionInformationIsNotMultiFirm() {
        //Arrange
        session.setAttribute("isMultiFirmUser", false);
        //act
        String view = firmSelectionController.selectFirmGet(firmSearchForm, session, model, 10);
        //Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_ADD_USER_FIRM);

        assertThat(model.getAttribute("firmSearchForm")).isEqualTo(firmSearchForm);
        assertThat(model.getAttribute("firmSearchResultCount")).isEqualTo(10);
        assertFalse((Boolean) model.getAttribute("showSkipFirmSelection"));

    }

    @Test
    void selectFirmGetWithoutSessionInformationIsMultiFirm() {
        //Arrange
        session.setAttribute("isMultiFirmUser", true);

        //act
        String view = firmSelectionController.selectFirmGet(firmSearchForm, session, model, 10);
        //Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_ADD_USER_FIRM);

        assertThat(model.getAttribute("firmSearchForm")).isEqualTo(firmSearchForm);
        assertThat(model.getAttribute("firmSearchResultCount")).isEqualTo(10);
        assertTrue((Boolean) model.getAttribute("showSkipFirmSelection"));

    }

    @Test
    void selectFirmGetWithoutSessionInformationFirmSearchFormFromSession() {
        //Arrange
        session.setAttribute("isMultiFirmUser", true);
        session.setAttribute("firmSearchForm", firmSearchForm);

        //act
        String view = firmSelectionController.selectFirmGet(firmSearchForm, session, model, 10);
        //Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_ADD_USER_FIRM);

        assertThat(model.getAttribute("firmSearchForm")).isEqualTo(firmSearchForm);
        assertThat(session.getAttribute("firmSearchForm")).isEqualTo(firmSearchForm);
        assertThat(model.getAttribute("firmSearchResultCount")).isEqualTo(10);
        assertTrue((Boolean) model.getAttribute("showSkipFirmSelection"));

    }

    @Test
    void selectFirmGetWithoutSessionInformationFirmFromSession() {
        //Arrange
        UUID firmId = UUID.randomUUID();
        FirmDto firm = FirmDto.builder()
                .name("Firm")
                .id(firmId)
                .build();

        session.setAttribute("isMultiFirmUser", true);
        session.setAttribute("firm",firm);
        FirmSearchForm expectedForm = FirmSearchForm.builder()
                .selectedFirmId(firm.getId())
                .firmSearch(firm.getName())
                .build();
        //act
        String view = firmSelectionController.selectFirmGet(firmSearchForm, session, model, 10);
        //Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_ADD_USER_FIRM);

        assertThat(model.getAttribute("firmSearchForm")).isEqualTo(expectedForm);
        assertThat(session.getAttribute("firmSearchForm")).isEqualTo(null);
        assertThat(session.getAttribute("firm")).isEqualTo(firm);
        assertThat(model.getAttribute("firmSearchResultCount")).isEqualTo(10);
        assertTrue((Boolean) model.getAttribute("showSkipFirmSelection"));

    }

    @Test
    void selectFirmPostWithErrors() {
        //Arrange
        when(bindingResult.hasErrors()).thenReturn(true);
        //act
        String view = firmSelectionController.selectFirmPost(firmSearchForm, bindingResult, session, model);
        //Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_ADD_USER_FIRM);
        assertThat(session.getAttribute("firmSearchForm")).isEqualTo(firmSearchForm);
        assertFalse((Boolean) model.getAttribute("showSkipFirmSelection"));
    }

    @Test
    void selectFirmPostNoFirmFound() {
        UUID firmId = UUID.randomUUID();
        //Arrange
        firmSearchForm.setFirmSearch("firm");
        when(firmService.getAllFirmsFromCache()).thenReturn(List.of());
        //act
        String view = firmSelectionController.selectFirmPost(firmSearchForm, bindingResult, session, model);
        //Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_ADD_USER_FIRM);
        assertThat(session.getAttribute("firmSearchForm")).isNull();
        assertThat(session.getAttribute("firm")).isNull();
        assertFalse((Boolean) model.getAttribute("showSkipFirmSelection"));
        verify(bindingResult).rejectValue("firmSearch",
                "error.firm",
                "No firm found with that name. Please select from the dropdown.");
    }

    @Test
    void selectFirmPostWithoutError() {
        //Arrange
        UUID firmId = UUID.randomUUID();

        List<FirmDto> firmDtos = List.of(FirmDto.builder()
                .name("firm")
                .id(firmId)
                .build());
        firmSearchForm.setFirmSearch("firm");
        session.setAttribute("multiFirmUserForm", multiFirmUserForm);
        when(firmService.getAllFirmsFromCache()).thenReturn(firmDtos);
        when(userService.hasUserFirmAlreadyAssigned(multiFirmUserForm.getEmail(), firmId)).thenReturn(false);
        //act
        String view = firmSelectionController.selectFirmPost(firmSearchForm, bindingResult, session, model);
        //Assert
        assertThat(view).isEqualTo("redirect:/adminFirmSelection/check-answers");
        assertThat(session.getAttribute("firmSearchForm")).isEqualTo(firmSearchForm);
        assertThat(session.getAttribute("delegateTargetFirmId")).isEqualTo(firmSearchForm.getSelectedFirmId().toString());
    }

    @Test
    void selectFirmPostWithErrorUserProfileAlreadyExists () {
        //Arrange
        UUID firmId = UUID.randomUUID();

        List<FirmDto> firmDtos = List.of(FirmDto.builder()
                .name("firm")
                .id(firmId)
                .build());
        firmSearchForm.setFirmSearch("firm");
        session.setAttribute("multiFirmUserForm", multiFirmUserForm);
        when(firmService.getAllFirmsFromCache()).thenReturn(firmDtos);
        when(userService.hasUserFirmAlreadyAssigned(multiFirmUserForm.getEmail(), firmId)).thenReturn(true);
        //act
        String view = firmSelectionController.selectFirmPost(firmSearchForm, bindingResult, session, model);
        //Assert
        assertThat(view).isEqualTo(ADMIN_TOOLS_ADD_USER_FIRM);
        verify(bindingResult).rejectValue("firmSearch",
                "error.firm",
                "User profile already exists for this firm.");
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

    private static Optional<EntraUser> getEntraUserProfileWithParents(Firm parent, Firm child) {

        Set<UserProfile> profiles = new LinkedHashSet<>();
        profiles.add(UserProfile.builder().firm(parent).build());
        profiles.add(UserProfile.builder().firm(child).build());

        return Optional.ofNullable(EntraUser.builder()
                .multiFirmUser(true)
                .email("test@test.com")
                .firstName("test")
                .lastName("test")
                .userProfiles(Collections.unmodifiableSet(profiles))
                .build());
    }
}