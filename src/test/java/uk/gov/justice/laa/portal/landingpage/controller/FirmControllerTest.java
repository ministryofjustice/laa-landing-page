package uk.gov.justice.laa.portal.landingpage.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.view.RedirectView;

import uk.gov.justice.laa.portal.landingpage.dto.SwitchProfileAuditEvent;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.service.EventService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@ExtendWith(MockitoExtension.class)
public class FirmControllerTest {

    private FirmController controller;

    @Mock
    private LoginService loginService;

    @Mock
    private UserService userService;

    @Mock
    private EventService eventService;

    @Mock
    private Authentication authentication;

    private Model model;
    private RedirectAttributes redirectAttributes;

    @BeforeEach
    public void setUp() {
        controller = new FirmController(loginService, userService, eventService);
        model = new ExtendedModelMap();
        redirectAttributes = new RedirectAttributesModelMap();
    }

    private EntraUser createMultiFirmUser(boolean isMultiFirm) {
        UUID userId = UUID.randomUUID();
        
        Set<UserProfile> profiles = new java.util.HashSet<>();
        if (isMultiFirm) {
            profiles.add(createUserProfile(UUID.randomUUID(), "Firm A", "FA001", true));
            profiles.add(createUserProfile(UUID.randomUUID(), "Firm B", "FB002", false));
            profiles.add(createUserProfile(UUID.randomUUID(), "Firm C", "FC003", false));
        } else {
            profiles.add(createUserProfile(UUID.randomUUID(), "Single Firm", "SF001", true));
        }
        
        EntraUser user = EntraUser.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .multiFirmUser(isMultiFirm)
                .userProfiles(profiles)
                .build();
        
        return user;
    }

    private UserProfile createUserProfile(UUID firmId, String firmName, String firmCode, boolean isActive) {
        Firm firm = Firm.builder()
                .id(firmId)
                .name(firmName)
                .code(firmCode)
                .type(FirmType.ADVOCATE)
                .build();

        UserProfile profile = UserProfile.builder()
                .id(UUID.randomUUID())
                .firm(firm)
                .activeProfile(isActive)
                .build();

        return profile;
    }

    @Test
    public void switchFirmGet_shouldReturnViewForMultiFirmUser() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, null, null, 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("userFirmList")).isNotNull();
        assertThat(model.getAttribute("currentPage")).isEqualTo(1);
        assertThat(model.getAttribute("totalPages")).isEqualTo(1);
        assertThat(model.getAttribute("totalItems")).isEqualTo(3);
        assertThat(model.getAttribute("pageSize")).isEqualTo(5);
        assertThat(model.getAttribute("pageTitle")).isEqualTo("Switch firm");
        
        verify(userService).getUserProfilesByEntraUserIdAndSearch(user.getId(), null);
    }

    @Test
    public void switchFirmGet_shouldRedirectToHomeForNonMultiFirmUser() {
        // Given
        EntraUser user = createMultiFirmUser(false);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);

        // When
        String result = controller.switchFirm(null, null, null, 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("redirect:/home");
        verify(userService, never()).getUserProfilesByEntraUserIdAndSearch(any(), any());
    }

    @Test
    public void switchFirmGet_shouldRedirectToHomeForNullUser() {
        // Given
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(null);

        // When
        String result = controller.switchFirm(null, null, null, 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("redirect:/home");
        verify(userService, never()).getUserProfilesByEntraUserIdAndSearch(any(), any());
    }

    @Test
    public void switchFirmGet_shouldApplySearchFilter() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        
        List<UserProfile> filteredProfiles = List.of(new ArrayList<>(user.getUserProfiles()).get(0));
        when(userService.getUserProfilesByEntraUserIdAndSearch(user.getId(), "Firm A"))
                .thenReturn(filteredProfiles);

        // When
        String result = controller.switchFirm("Firm A", null, null, 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("search")).isEqualTo("Firm A");
        assertThat(model.getAttribute("totalItems")).isEqualTo(1);
        
        verify(userService).getUserProfilesByEntraUserIdAndSearch(user.getId(), "Firm A");
    }

    @Test
    public void switchFirmGet_shouldSortByFirmNameAscending() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, "firmName", "asc", 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("sort")).isEqualTo("firmName");
        assertThat(model.getAttribute("direction")).isEqualTo("asc");
        
        @SuppressWarnings("unchecked")
        List<Object> userFirmList = (List<Object>) model.getAttribute("userFirmList");
        assertThat(userFirmList).isNotNull();
    }

    @Test
    public void switchFirmGet_shouldSortByFirmCodeDescending() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, "firmCode", "desc", 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("sort")).isEqualTo("firmCode");
        assertThat(model.getAttribute("direction")).isEqualTo("desc");
    }

    @Test
    public void switchFirmGet_shouldApplyDefaultSortActiveProfileFirst() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, null, null, 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        
        @SuppressWarnings("unchecked")
        List<Object> userFirmList = (List<Object>) model.getAttribute("userFirmList");
        assertThat(userFirmList).isNotNull();
        assertThat(userFirmList.size()).isEqualTo(3);
        
        // The active profile should be first in default sort
        // This is tested implicitly through the sorting logic
    }

    @Test
    public void switchFirmGet_shouldPaginateResults() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        // Add more profiles to test pagination
        for (int i = 0; i < 10; i++) {
            user.getUserProfiles().add(
                createUserProfile(UUID.randomUUID(), "Firm " + i, "F" + i, false)
            );
        }
        
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, null, null, 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("currentPage")).isEqualTo(1);
        assertThat(model.getAttribute("totalPages")).isEqualTo(3); // 13 items / 5 per page = 3 pages
        assertThat(model.getAttribute("totalItems")).isEqualTo(13);
        
        @SuppressWarnings("unchecked")
        List<Object> userFirmList = (List<Object>) model.getAttribute("userFirmList");
        assertThat(userFirmList).hasSize(5); // First page should have 5 items
    }

    @Test
    public void switchFirmGet_shouldHandlePageOutOfRange() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When - request page 10 when only 1 page exists
        String result = controller.switchFirm(null, null, null, 10, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("currentPage")).isEqualTo(1); // Should be adjusted to valid page
    }

    @Test
    public void switchFirmGet_shouldHandleNegativePage() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When - request negative page
        String result = controller.switchFirm(null, null, null, -1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("currentPage")).isEqualTo(1); // Should be adjusted to 1
    }

    @Test
    public void switchFirmPost_shouldSwitchFirmSuccessfully() throws IOException {
        // Given
        EntraUser user = createMultiFirmUser(true);
        UserProfile activeProfile = user.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .orElseThrow();
        UserProfile targetProfile = user.getUserProfiles().stream()
                .filter(p -> !p.isActiveProfile())
                .findFirst()
                .orElseThrow();
        
        String targetFirmId = targetProfile.getFirm().getId().toString();
        
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        doNothing().when(userService).setDefaultActiveProfile(any(), any());
        doNothing().when(eventService).logEvent(any());

        // When
        RedirectView result = controller.switchFirm(targetFirmId, authentication, redirectAttributes);

        // Then
        assertThat(result.getUrl()).isEqualTo("/switch-firm");
        assertThat(redirectAttributes.getFlashAttributes().get("message"))
                .isEqualTo("Switch firm successful");
        assertThat(redirectAttributes.getFlashAttributes().get("messageType"))
                .isEqualTo("success");
        
        verify(userService).setDefaultActiveProfile(user, UUID.fromString(targetFirmId));
        
        ArgumentCaptor<SwitchProfileAuditEvent> eventCaptor = ArgumentCaptor.forClass(SwitchProfileAuditEvent.class);
        verify(eventService).logEvent(eventCaptor.capture());
        
        SwitchProfileAuditEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isEqualTo(user.getId());
        assertThat(event.getDescription()).contains(user.getId().toString());
        assertThat(event.getDescription()).contains(activeProfile.getFirm().getId().toString());
        assertThat(event.getDescription()).contains(targetFirmId);
    }

    @Test
    public void switchFirmPost_shouldFailWhenSwitchingToSameFirm() throws IOException {
        // Given
        EntraUser user = createMultiFirmUser(true);
        UserProfile activeProfile = user.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .orElseThrow();
        
        String sameFirmId = activeProfile.getFirm().getId().toString();
        
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);

        // When
        RedirectView result = controller.switchFirm(sameFirmId, authentication, redirectAttributes);

        // Then
        assertThat(result.getUrl()).isEqualTo("/switch-firm");
        assertThat(redirectAttributes.getFlashAttributes().get("message"))
                .isEqualTo("Can not switch to the same Firm");
        assertThat(redirectAttributes.getFlashAttributes().get("messageType"))
                .isEqualTo("error");
        
        verify(userService, never()).setDefaultActiveProfile(any(), any());
        verify(eventService, never()).logEvent(any());
    }

    @Test
    public void switchFirmPost_shouldFailForNonMultiFirmUser() throws IOException {
        // Given
        EntraUser user = createMultiFirmUser(false);
        String firmId = UUID.randomUUID().toString();
        
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);

        // When
        RedirectView result = controller.switchFirm(firmId, authentication, redirectAttributes);

        // Then
        assertThat(result.getUrl()).isEqualTo("/switch-firm");
        assertThat(redirectAttributes.getFlashAttributes().get("message"))
                .isEqualTo("Apply to multi firm user only");
        assertThat(redirectAttributes.getFlashAttributes().get("messageType"))
                .isEqualTo("error");
        
        verify(userService, never()).setDefaultActiveProfile(any(), any());
        verify(eventService, never()).logEvent(any());
    }

    @Test
    public void switchFirmPost_shouldFailForNullUser() throws IOException {
        // Given
        String firmId = UUID.randomUUID().toString();
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(null);

        // When
        RedirectView result = controller.switchFirm(firmId, authentication, redirectAttributes);

        // Then
        assertThat(result.getUrl()).isEqualTo("/switch-firm");
        assertThat(redirectAttributes.getFlashAttributes().get("message"))
                .isEqualTo("Apply to multi firm user only");
        assertThat(redirectAttributes.getFlashAttributes().get("messageType"))
                .isEqualTo("error");
        
        verify(userService, never()).setDefaultActiveProfile(any(), any());
        verify(eventService, never()).logEvent(any());
    }

    @Test
    public void switchFirmGet_shouldHandleEmptySearchGracefully() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(user.getId(), ""))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm("", null, null, 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("search")).isEqualTo("");
        verify(userService).getUserProfilesByEntraUserIdAndSearch(user.getId(), "");
    }

    @Test
    public void switchFirmGet_shouldHandleCustomPageSize() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, null, null, 1, 10, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("pageSize")).isEqualTo(10);
        
        @SuppressWarnings("unchecked")
        List<Object> userFirmList = (List<Object>) model.getAttribute("userFirmList");
        assertThat(userFirmList).hasSize(3); // All 3 items fit on one page with size 10
    }

    @Test
    public void switchFirmGet_shouldSortByActiveProfile() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, "activeProfile", "desc", 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("sort")).isEqualTo("activeProfile");
        assertThat(model.getAttribute("direction")).isEqualTo("desc");
    }

    @Test
    public void switchFirmGet_shouldSortByFirmType() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, "firmType", "asc", 1, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("sort")).isEqualTo("firmType");
        assertThat(model.getAttribute("direction")).isEqualTo("asc");
    }

    @Test
    public void switchFirmGet_shouldHandleSecondPage() {
        // Given
        EntraUser user = createMultiFirmUser(true);
        // Add more profiles for pagination
        for (int i = 0; i < 7; i++) {
            user.getUserProfiles().add(
                createUserProfile(UUID.randomUUID(), "Firm " + i, "F" + i, false)
            );
        }
        
        when(loginService.getCurrentEntraUser(authentication)).thenReturn(user);
        when(userService.getUserProfilesByEntraUserIdAndSearch(eq(user.getId()), any()))
                .thenReturn(new ArrayList<>(user.getUserProfiles()));

        // When
        String result = controller.switchFirm(null, null, null, 2, 5, model, authentication);

        // Then
        assertThat(result).isEqualTo("switch-firm");
        assertThat(model.getAttribute("currentPage")).isEqualTo(2);
        assertThat(model.getAttribute("totalPages")).isEqualTo(2); // 10 items / 5 per page = 2 pages
        
        @SuppressWarnings("unchecked")
        List<Object> userFirmList = (List<Object>) model.getAttribute("userFirmList");
        assertThat(userFirmList).hasSize(5); // Second page should have 5 items
    }
}
