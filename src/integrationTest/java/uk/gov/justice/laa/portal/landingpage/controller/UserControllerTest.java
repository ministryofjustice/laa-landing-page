package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.service.EmailService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;

import java.util.Set;


class UserControllerTest extends BaseIntegrationTest {

    @MockitoBean
    private GraphServiceClient graphServiceClient;
    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private LoginService loginService;

    @Test
    void shouldRedirectAnonymousUser() throws Exception {
        this.mockMvc
                .perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void shouldRedirectAnonymousUserList() throws Exception {
        this.mockMvc.perform(get("/admin/userlist"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void shouldRedirectAnonymousHome() throws Exception {
        this.mockMvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void redirectToLoginPage() throws Exception {
        this.mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminPageSuccess() throws Exception {
        Authentication authentication = new TestingAuthenticationToken("user", "password", "INTERNAL");
        Set<UserProfile> userProfiles = Set.of(UserProfile.builder().userType(UserType.INTERNAL).build());
        EntraUser entraUser = EntraUser.builder().userProfiles(userProfiles).build();
        when(loginService.getCurrentEntraUser(any())).thenReturn(entraUser);
        mockMvc.perform(get("/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(authentication)))
                .andExpect(status().isOk());
    }
}
