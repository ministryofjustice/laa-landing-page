package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpSession;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.dto.SwitchProfileAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileStatus;
import uk.gov.justice.laa.portal.landingpage.model.UserSessionData;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.FirmService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private LoginService loginService;
    @Mock
    private FirmService firmService;
    @Mock
    private UserService userService;
    @Mock
    private EventService eventService;

    @Mock
    private HttpSession session;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2AuthorizedClient authClient;

    @InjectMocks
    private LoginController controller;

    @Test
    void givenEmptyEmail_whenLoginGet_thenReturnsIndexView() {

        // Arrange
        Model model = new ConcurrentModel();

        // Act
        String viewIndex = controller.index();

        // Assert
        assertThat(viewIndex).isEqualTo("redirect:/home");
        assertThat(model.getAttribute("successMessage")).isNull();
    }

    @Test
    void givenLogout_whenLoginGet_thenDisplayLogoutOkView() {

        // Arrange
        Model model = new ConcurrentModel();

        // Act
        String viewIndex = controller.index();

        // Assert
        assertThat(viewIndex).isEqualTo("redirect:/home");
        // Since we're redirecting to home, no model attributes are set in the index method
        assertThat(model.getAttribute("successMessage")).isNull();
    }

    @Test
    void givenBlankEmail_whenHandleLoginPost_thenRedirectsWithErrorFlash() {

        // Arrange
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        // Act
        RedirectView result = controller.handleLogin("   ", attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/");
        assertThat(attrs.getFlashAttributes().get("errorMessage"))
                .isEqualTo("An incorrect Username or Password was specified");
    }

    @Test
    void givenNullEmail_whenHandleLoginPost_thenRedirectsWithErrorFlash() {

        // Arrange
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        // Act
        RedirectView result = controller.handleLogin(null, attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/");
        assertThat(attrs.getFlashAttributes().get("errorMessage"))
                .isEqualTo("An incorrect Username or Password was specified");
    }

    @Test
    void givenValidEmail_whenHandleLoginPost_thenRedirectsToAzure() {

        // Arrange
        String email = "foo@bar.com";
        String expectedUrl = "https://login.test/?hint=foo%40bar.com";
        when(loginService.buildAzureLoginUrl(email)).thenReturn(expectedUrl);
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        // Act
        RedirectView result = controller.handleLogin(email, attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo(expectedUrl);
        verify(loginService).buildAzureLoginUrl(email);
        assertThat(attrs.getFlashAttributes()).isEmpty();
    }

    @Test
    void givenLoginServiceThrowsException_whenHandleLoginPost_thenRedirectsWithError() {

        // Arrange
        String email = "exception@test.com";
        when(loginService.buildAzureLoginUrl(email)).thenThrow(new RuntimeException("Azure URL build failed"));
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        // Act
        RedirectView result = controller.handleLogin(email, attrs);

        // Assert
        assertThat(result.getUrl()).isEqualTo("/");
    }

    @Test
    void givenAuthenticatedUser_whenHomeGet_thenPopulatesModelAndReturnsHomeView() {

        // Arrange
        Model model = new ConcurrentModel();
        UserSessionData mockSessionData = UserSessionData.builder()
                .name("Test User")
                .build();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class),
                any(HttpSession.class)))
                .thenReturn(mockSessionData);

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.getAttribute("name")).isEqualTo("Test User");
        verify(loginService).processUserSession(authentication, authClient, session);
    }

    @Test
    void givenAuthenticatedUser_whenHomeGet_thenPopulatesModelAndReturnsHomeViewWithAdmin() {

        // Arrange
        Model model = new ConcurrentModel();
        UUID userId = UUID.randomUUID();
        EntraUserDto user = EntraUserDto.builder().id(userId.toString()).build();
        UserSessionData mockSessionData = UserSessionData.builder()
                .name("Test User")
                .user(user)
                .build();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class),
                any(HttpSession.class)))
                .thenReturn(mockSessionData);
        when(userService.getUserPermissionsByUserId(user.getId())).thenReturn(Set.of(Permission.VIEW_EXTERNAL_USER));

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.getAttribute("name")).isEqualTo("Test User");
        assertThat(model.getAttribute("isAdminUser")).isEqualTo(true);
        verify(loginService).processUserSession(authentication, authClient, session);
    }

    @Test
    void givenAuthenticatedUser_whenHomeGet_thenPopulatesModelAndReturnsHomeViewWithoutAdmin() {

        // Arrange
        Model model = new ConcurrentModel();
        UUID userId = UUID.randomUUID();
        EntraUserDto user = EntraUserDto.builder().id(userId.toString()).build();
        UserSessionData mockSessionData = UserSessionData.builder()
                .name("Test User")
                .user(user)
                .build();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class),
                any(HttpSession.class)))
                .thenReturn(mockSessionData);
        when(userService.getUserPermissionsByUserId(user.getId())).thenReturn(Set.of());

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.getAttribute("name")).isEqualTo("Test User");
        assertThat(model.getAttribute("isAdminUser")).isEqualTo(false);
        verify(loginService).processUserSession(authentication, authClient, session);
    }

    @Test
    void givenAuthenticatedUser_whenHomeGet_thenPopulatesModelAndReturnsHomeViewWithoutUser() {

        // Arrange
        Model model = new ConcurrentModel();
        UUID userId = UUID.randomUUID();
        UserSessionData mockSessionData = UserSessionData.builder()
                .name("Test User")
                .build();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class),
                any(HttpSession.class)))
                .thenReturn(mockSessionData);

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.getAttribute("name")).isEqualTo("Test User");
        assertThat(model.getAttribute("isAdminUser")).isEqualTo(false);
        verify(loginService).processUserSession(authentication, authClient, session);
    }

    @Test
    void givenNullSessionData_whenHomeGet_thenReturnsHomeViewWithoutData() {

        // Arrange
        Model model = new ConcurrentModel();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class),
                any(HttpSession.class)))
                .thenReturn(null);

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.asMap()).doesNotContainKey("name");
        verify(loginService).processUserSession(authentication, authClient, session);
    }

    @Test
    void givenLoginServiceThrowsException_whenHomeGet_thenReturnsHomeView() {

        // Arrange
        Model model = new ConcurrentModel();
        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class),
                any(HttpSession.class)))
                .thenThrow(new RuntimeException("Error processing session"));

        // Act
        String viewName = controller.home(model, authentication, session, authClient);

        // Assert
        assertThat(viewName).isEqualTo("home");
        assertThat(model.asMap()).doesNotContainKey("name");
    }

    @Test
    void whenHandleException_thenRedirectToErrorPage() {

        // Arrange & Act
        RedirectView result = controller.handleException(new Exception());

        // Assert
        assertThat(result.getUrl()).isEqualTo("/error");
    }

    @Test
    void whenLogoutSuccess_thenReturnsLogoutTemplate() {
        // Arrange & Act
        String result = controller.logoutSuccess();

        // Assert
        assertThat(result).isEqualTo("logout");
    }

    @Test
    void switchFirm_get_active() {
        UUID firmId = UUID.randomUUID();
        UserProfile up = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().id(firmId).name("name").build()).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().isMultiFirmUser(true).userProfiles(Set.of(up)).build());
        when(firmService.getUserAllFirms(any()))
                .thenReturn(List.of(FirmDto.builder().id(firmId).name("name").build()));
        Model model = new ConcurrentModel();
        String view = controller.userFirmsPage(null, model, authentication);
        assertThat(view).isEqualTo("switch-firm");
        List<FirmDto> firmDtoList = (List<FirmDto>) model.getAttribute("firmDtoList");
        assertThat(firmDtoList).hasSize(1);
        assertThat(firmDtoList.getFirst().getName()).isEqualTo("name - Active");
    }

    @Test
    void switchFirm_get_no_active() {
        UUID firm1Id = UUID.randomUUID();
        UserProfile up1 = UserProfile.builder().activeProfile(false).userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().id(firm1Id).name("name").build()).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().isMultiFirmUser(true).userProfiles(Set.of(up1)).build());
        when(firmService.getUserAllFirms(any()))
                .thenReturn(List.of(FirmDto.builder().id(firm1Id).name("name").build()));
        Model model = new ConcurrentModel();
        String view = controller.userFirmsPage(null, model, authentication);
        assertThat(view).isEqualTo("switch-firm");
        List<FirmDto> firmDtoList = (List<FirmDto>) model.getAttribute("firmDtoList");
        assertThat(firmDtoList).hasSize(1);
        assertThat(firmDtoList.getFirst().getName()).isEqualTo("name");
    }

    @Test
    void switchFirm_single_firm_user() {
        UUID firmId = UUID.randomUUID();
        UserProfile up = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().id(firmId).name("name").build()).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().isMultiFirmUser(false).userProfiles(Set.of(up)).build());
        Model model = new ConcurrentModel();
        String view = controller.userFirmsPage(null, model, authentication);
        assertThat(view).isEqualTo("redirect:/home");
    }

    @Test
    void switchFirm_post_ok() throws IOException {
        UUID firm1Id = UUID.randomUUID();
        UUID firm2Id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserProfile up1 = UserProfile.builder().activeProfile(false).userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().id(firm1Id).name("name").build()).build();
        UserProfile up2 = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().id(firm2Id).name("name").build()).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().id(userId).userProfiles(Set.of(up1, up2)).isMultiFirmUser(true).build());
        RedirectView view = controller.switchFirm(firm1Id.toString(), authentication);

        verify(loginService).getCurrentEntraUser(any());
        verify(userService).setDefaultActiveProfile(any(), any());
        ArgumentCaptor<SwitchProfileAuditEvent> eventCaptor = ArgumentCaptor.forClass(SwitchProfileAuditEvent.class);
        verify(eventService).logEvent(eventCaptor.capture());
        SwitchProfileAuditEvent auditEvent = eventCaptor.getValue();
        assertThat(view.getUrl()).isEqualTo("/switchfirm?message=Switch firm successful");
        assertThat(auditEvent.getDescription()).contains("User firm switched, user id " + userId + ", from firm " + firm2Id + " to firm " + firm1Id);
    }

    @Test
    void switchFirm_post_not_multiFirmUser() throws IOException {
        UUID firm1Id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserProfile up1 = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().id(firm1Id).name("name").build()).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().id(userId).userProfiles(Set.of(up1)).isMultiFirmUser(false).build());
        RedirectView view = controller.switchFirm(firm1Id.toString(), authentication);

        verify(loginService).getCurrentEntraUser(any());
        verify(userService, never()).setDefaultActiveProfile(any(), any());
        assertThat(view.getUrl()).isEqualTo("/switchfirm?message=Apply to multi firm user only");
    }

    @Test
    void switchFirm_post_same_firm() throws IOException {
        UUID firm1Id = UUID.randomUUID();
        UUID firm2Id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserProfile up1 = UserProfile.builder().activeProfile(false).userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().id(firm1Id).name("name").build()).build();
        UserProfile up2 = UserProfile.builder().activeProfile(true).userProfileStatus(UserProfileStatus.COMPLETE)
                .firm(Firm.builder().id(firm2Id).name("name").build()).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(EntraUser.builder().id(userId).userProfiles(Set.of(up1, up2)).isMultiFirmUser(true).build());
        RedirectView view = controller.switchFirm(firm2Id.toString(), authentication);

        verify(loginService).getCurrentEntraUser(any());
        assertThat(view.getUrl()).isEqualTo("/switchfirm?message=Can not switch to the same Firm");
    }

    @Test
    void home_userWithNoRoles_internalUser() throws IOException {
        // Given
        Model model = new ConcurrentModel();
        String userId = UUID.randomUUID().toString();
        UserSessionData userSessionDataWithNoRoles = UserSessionData.builder()
                .laaApplications(null) // No roles assigned
                .user(EntraUserDto.builder().id(userId).build())
                .name("Test User")
                .build();

        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class), any(HttpSession.class)))
                .thenReturn(userSessionDataWithNoRoles);
        when(userService.isInternal(userId)).thenReturn(true);

        // When
        String viewName = controller.home(model, authentication, session, authClient);

        // Then
        assertThat(viewName).isEqualTo("home");
        assertThat(model.getAttribute("userHasNoRoles")).isEqualTo(true);
        assertThat(model.getAttribute("isInternalUser")).isEqualTo(true);
    }

    @Test
    void home_userWithNoRoles_externalUser() throws IOException {
        // Given
        Model model = new ConcurrentModel();
        String userId = UUID.randomUUID().toString();
        UserSessionData userSessionDataWithNoRoles = UserSessionData.builder()
                .laaApplications(Set.of()) // No roles assigned
                .user(EntraUserDto.builder().id(userId).build())
                .name("Test User")
                .build();

        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class), any(HttpSession.class)))
                .thenReturn(userSessionDataWithNoRoles);
        when(userService.isInternal(userId)).thenReturn(false);

        // When
        String viewName = controller.home(model, authentication, session, authClient);

        // Then
        assertThat(viewName).isEqualTo("home");
        assertThat(model.getAttribute("userHasNoRoles")).isEqualTo(true);
        assertThat(model.getAttribute("isInternalUser")).isEqualTo(false);
    }

    @Test
    void home_userWithRoles_shouldNotSetNoRolesFlags() throws IOException {
        // Given
        Model model = new ConcurrentModel();
        UserSessionData userSessionDataWithRoles = UserSessionData.builder()
                .laaApplications(Set.of(
                    uk.gov.justice.laa.portal.landingpage.model.LaaApplication.builder()
                        .name("Test App")
                        .title("Test Application")
                        .build()))
                .user(EntraUserDto.builder().id(UUID.randomUUID().toString()).build())
                .name("Test User")
                .build();

        when(loginService.processUserSession(any(Authentication.class), any(OAuth2AuthorizedClient.class), any(HttpSession.class)))
                .thenReturn(userSessionDataWithRoles);

        // When
        String viewName = controller.home(model, authentication, session, authClient);

        // Then
        assertThat(viewName).isEqualTo("home");
        assertThat(model.getAttribute("userHasNoRoles")).isEqualTo(false);
        assertThat(model.getAttribute("isInternalUser")).isNull();
    }
}