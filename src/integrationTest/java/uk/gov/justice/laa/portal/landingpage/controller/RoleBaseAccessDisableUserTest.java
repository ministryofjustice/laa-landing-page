package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserReasonRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleBaseAccessDisableUserTest extends RoleBasedAccessIntegrationTest {

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private DisableUserReasonRepository disableUserReasonRepository;
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private UserAccountStatusAuditRepository userAccountStatusAuditRepository;

    @Test
    public void testGlobalAdminCanAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().isOk());
    }

    @Test
    public void testExternalUserAdminCanAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().isOk());
    }

    @Test
    public void testInformationAndAssuranceCanAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = informationAndAssuranceUsers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().isOk());
    }

    @Test
    public void testInternalUserManagerCannotAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().is3xxRedirection());
    }

    @Test
    public void testFirmUserManagerCanAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getLast();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().isOk());
    }

    @Test
    public void testFirmUserManagerCannotAccessDisableUserReasonPageIfNotSameFirm() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().is3xxRedirection());
    }

    @Test
    public void testExternalUserManagerCanAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().isOk());
    }

    @Test
    public void testInternalUserNoRolesCannotAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = internalUsersNoRoles.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().is4xxClientError());
    }

    @Test
    public void testExternalUserNoRolesCannotAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = externalUsersNoRoles.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().is4xxClientError());
    }

    @Test
    public void testInternalUserViewerCannotAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = internalUserViewers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().is3xxRedirection());
    }

    @Test
    public void testExternalUserViewerCannotAccessDisableUserReasonPage() throws Exception {
        EntraUser loggedInUser = externalUserViewers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        requestDisableUserReasonPage(loggedInUser, accessedUser, status().is3xxRedirection());
    }

    @Test
    public void testGlobalAdminCanDisableUser() throws Exception {
        EntraUser loggedInUser = globalAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        List<UserAccountStatusAudit> statusChanges = userAccountStatusAuditRepository.findAll();
        assertThat(statusChanges.size()).isEqualTo(1);
        UserAccountStatusAudit statusChange = statusChanges.getFirst();
        assertThat(statusChange.getEntraUser().getId()).isEqualTo(accessedUser.getId());
        assertThat(statusChange.getStatusChange()).isEqualTo(UserAccountStatus.DISABLED);
        assertThat(statusChange.getDisabledBy()).isEqualTo(loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
        userAccountStatusAuditRepository.delete(statusChange);
    }

    @Test
    public void testExternalUserAdminCanDisableUser() throws Exception {
        EntraUser loggedInUser = externalUserAdmins.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        List<UserAccountStatusAudit> statusChanges = userAccountStatusAuditRepository.findAll();
        assertThat(statusChanges.size()).isEqualTo(1);
        UserAccountStatusAudit statusChange = statusChanges.getFirst();
        assertThat(statusChange.getEntraUser().getId()).isEqualTo(accessedUser.getId());
        assertThat(statusChange.getStatusChange()).isEqualTo(UserAccountStatus.DISABLED);
        assertThat(statusChange.getDisabledBy()).isEqualTo(loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
        userAccountStatusAuditRepository.delete(statusChange);
    }

    @Test
    public void testInformationAndAssuranceCanDisableUser() throws Exception {
        EntraUser loggedInUser = informationAndAssuranceUsers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        List<UserAccountStatusAudit> statusChanges = userAccountStatusAuditRepository.findAll();
        assertThat(statusChanges.size()).isEqualTo(1);
        UserAccountStatusAudit statusChange = statusChanges.getFirst();
        assertThat(statusChange.getEntraUser().getId()).isEqualTo(accessedUser.getId());
        assertThat(statusChange.getStatusChange()).isEqualTo(UserAccountStatus.DISABLED);
        assertThat(statusChange.getDisabledBy()).isEqualTo(loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
        userAccountStatusAuditRepository.delete(statusChange);
    }

    @Test
    public void testInternalUserManagerCannotDisableUser() throws Exception {
        EntraUser loggedInUser = internalUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().is3xxRedirection());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testExternalUserManagerCanDisableUser() throws Exception {
        EntraUser loggedInUser = externalOnlyUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        List<UserAccountStatusAudit> statusChanges = userAccountStatusAuditRepository.findAll();
        assertThat(statusChanges.size()).isEqualTo(1);
        UserAccountStatusAudit statusChange = statusChanges.getFirst();
        assertThat(statusChange.getEntraUser().getId()).isEqualTo(accessedUser.getId());
        assertThat(statusChange.getStatusChange()).isEqualTo(UserAccountStatus.DISABLED);
        assertThat(statusChange.getDisabledBy()).isEqualTo(loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
        userAccountStatusAuditRepository.delete(statusChange);
    }

    @Test
    public void testFirmUserManagerCanDisableUser() throws Exception {
        EntraUser loggedInUser = firmUserManagers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getLast();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().isOk());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();
        List<UserAccountStatusAudit> statusChanges = userAccountStatusAuditRepository.findAll();
        assertThat(statusChanges.size()).isEqualTo(1);
        UserAccountStatusAudit statusChange = statusChanges.getFirst();
        assertThat(statusChange.getEntraUser().getId()).isEqualTo(accessedUser.getId());
        assertThat(statusChange.getStatusChange()).isEqualTo(UserAccountStatus.DISABLED);
        assertThat(statusChange.getDisabledBy()).isEqualTo(loggedInUser.getFirstName() + " " + loggedInUser.getLastName());
        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.saveAndFlush(accessedUser);
        userAccountStatusAuditRepository.delete(statusChange);
    }

    @Test
    public void testInternalUserViewerCannotDisableUser() throws Exception {
        EntraUser loggedInUser = internalUserViewers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().is3xxRedirection());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testExternalUserViewerCannotDisableUser() throws Exception {
        EntraUser loggedInUser = externalUserViewers.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().is3xxRedirection());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testInternalUserNoRolesCannotDisableUser() throws Exception {
        EntraUser loggedInUser = internalUsersNoRoles.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().is4xxClientError());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testExternalUserNoRolesCannotDisableUser() throws Exception {
        EntraUser loggedInUser = externalUsersNoRoles.getFirst();
        EntraUser accessedUser = externalUsersNoRoles.getFirst();
        assertThat(accessedUser.isEnabled()).isTrue();
        sendDisableUserPost(loggedInUser, accessedUser, status().is4xxClientError());
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    public void requestDisableUserReasonPage(EntraUser loggedInUser, EntraUser accessedUser, ResultMatcher expectedResult) throws Exception {
        this.mockMvc.perform(get(String.format("/admin/users/manage/%s/disable", accessedUser.getId()))
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult);
    }

    public MvcResult sendDisableUserPost(EntraUser loggedInUser, EntraUser accessedUser, ResultMatcher expectedResult) throws Exception {
        DisableUserReason reason = disableUserReasonRepository.findAll().getFirst();
        return this.mockMvc.perform(post(String.format("/admin/users/manage/%s/disable", accessedUser.getId()))
                        .param("reasonId", reason.getId().toString())
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(expectedResult)
                .andReturn();
    }
}
