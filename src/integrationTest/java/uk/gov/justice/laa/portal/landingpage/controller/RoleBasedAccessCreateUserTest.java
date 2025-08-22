package uk.gov.justice.laa.portal.landingpage.controller;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBasedAccessCreateUserTest extends RoleBasedAccessIntegrationTest {

    @Test
    @Transactional
    public void testGlobalAdminCanCreateExternalUser() throws Exception {
        String email = "globalAdmin@creatingexternal.com";
        MvcResult result = createUser(globalAdmins.getFirst(), email, status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/user/create/confirmation");

        // Teardown
        Optional<UserProfile> optionalCreatedUserProfile = userProfileRepository.findAll().stream()
                .filter(profile -> profile.getEntraUser().getEmail().equalsIgnoreCase(email))
                .findFirst();
        assertThat(optionalCreatedUserProfile).isNotEmpty();
        UserProfile createdUserProfile = optionalCreatedUserProfile.get();
        EntraUser createdUser = createdUserProfile.getEntraUser();
        userProfileRepository.delete(createdUserProfile);
        entraUserRepository.delete(createdUser);
    }

    @Test
    @Transactional
    public void testGlobalAdminCannotCreateInternalUser() throws Exception {
        String email = "globalAdmin@creatinginternal.com";
        MvcResult result = createUser(globalAdmins.getFirst(), email, UserType.INTERNAL, status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/user/create/confirmation");

        // Teardown
        Optional<UserProfile> optionalCreatedUserProfile = userProfileRepository.findAll().stream()
                .filter(profile -> profile.getEntraUser().getEmail().equalsIgnoreCase(email))
                .findFirst();
        assertThat(optionalCreatedUserProfile).isNotEmpty();
        UserProfile createdUserProfile = optionalCreatedUserProfile.get();
        assertThat(createdUserProfile.getUserType()).isNotEqualTo(UserType.INTERNAL);
        EntraUser createdUser = createdUserProfile.getEntraUser();
        userProfileRepository.delete(createdUserProfile);
        entraUserRepository.delete(createdUser);
    }

    @Test
    @Transactional
    public void testExternalUserAdminCanCreateExternalUser() throws Exception {
        String email = "externalUserAdmin@creatingexternal.com";
        MvcResult result = createUser(externalUserAdmins.getFirst(), email, status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/user/create/confirmation");

        // Teardown
        Optional<UserProfile> optionalCreatedUserProfile = userProfileRepository.findAll().stream()
                .filter(profile -> profile.getEntraUser().getEmail().equalsIgnoreCase(email))
                .findFirst();
        assertThat(optionalCreatedUserProfile).isNotEmpty();
        UserProfile createdUserProfile = optionalCreatedUserProfile.get();
        EntraUser createdUser = createdUserProfile.getEntraUser();
        userProfileRepository.delete(createdUserProfile);
        entraUserRepository.delete(createdUser);
    }

    @Test
    @Transactional
    public void testExternalUserAdminCannotCreateInternalUser() throws Exception {
        String email = "externalUserAdmin@creatinginternal.com";
        MvcResult result = createUser(externalUserAdmins.getFirst(), email, UserType.INTERNAL, status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/admin/user/create/confirmation");

        // Teardown
        Optional<UserProfile> optionalCreatedUserProfile = userProfileRepository.findAll().stream()
                .filter(profile -> profile.getEntraUser().getEmail().equalsIgnoreCase(email))
                .findFirst();
        assertThat(optionalCreatedUserProfile).isNotEmpty();
        UserProfile createdUserProfile = optionalCreatedUserProfile.get();
        assertThat(createdUserProfile.getUserType()).isNotEqualTo(UserType.INTERNAL);
        EntraUser createdUser = createdUserProfile.getEntraUser();
        userProfileRepository.delete(createdUserProfile);
        entraUserRepository.delete(createdUser);
    }

    @Test
    public void testInternalUserManagerCannotCreateUser() throws Exception {
        String email = "internalUserManager@creatingexternal.com";
        MvcResult result = createUser(internalUserManagers.getFirst(), email, status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testInternalUserWithExternalUserManagerCannotCreateUser() throws Exception {
        String email = "internalUserWithExternalUserManager@creatingexternal.com";
        MvcResult result = createUser(internalWithExternalOnlyUserManagers.getFirst(), email, status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testExternalUserWithExternalUserManagerCannotCreateUser() throws Exception {
        String email = "externalUserWithExternalUserManager@creatingexternal.com";
        MvcResult result = createUser(externalOnlyUserManagers.getFirst(), email, status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testInternalUserWithInternalAndExternalUserManagerCannotCreateUser() throws Exception {
        String email = "internalUserWithInternalAndExternalUserManager@creatingexternal.com";
        MvcResult result = createUser(internalAndExternalUserManagers.getFirst(), email, status().is3xxRedirection());
        assertThat(result.getResponse()).isNotNull();
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/not-authorised");
    }

    @Test
    public void testInternalUserWithNoRolesCannotCreateUser() throws Exception {
        String email = "internalUserNoRoles@creatingexternal.com";
        createUser(internalUsersNoRoles.getFirst(), email, status().isForbidden());
    }

    @Test
    public void testExternalUserWithNoRolesCannotCreateUser() throws Exception {
        String email = "externalUserNoRoles@creatingexternal.com";
        createUser(externalUsersNoRoles.getFirst(), email, status().isForbidden());
    }

    private MvcResult createUser(EntraUser loggedInUser, String email, ResultMatcher expectedResult) throws Exception {
        return createUser(loggedInUser, email, UserType.EXTERNAL_SINGLE_FIRM, expectedResult);
    }

    private MvcResult createUser(EntraUser loggedInUser, String email, UserType userType, ResultMatcher expectedResult) throws Exception {
        MockHttpSession session = new MockHttpSession();
        // Create session user
        EntraUserDto user = EntraUserDto.builder()
                .email(email)
                .firstName("Test")
                .lastName("User")
                .build();
        session.setAttribute("user", user);

        // Create session firm
        FirmDto firmDto = FirmDto.builder()
                .code(testFirm1.getCode())
                .id(testFirm1.getId())
                .name(testFirm1.getName())
                .build();
        session.setAttribute("firm", firmDto);

        // Create session userType
        session.setAttribute("selectedUserType", userType);

        return this.mockMvc.perform(post("/admin/user/create/check-answers")
                        .with(userOauth2Login(loggedInUser))
                        .session(session)
                        .with(csrf()))
                .andExpect(expectedResult)
                .andReturn();
    }
}
