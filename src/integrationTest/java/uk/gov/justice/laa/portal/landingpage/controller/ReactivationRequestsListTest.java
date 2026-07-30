package uk.gov.justice.laa.portal.landingpage.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import uk.gov.justice.laa.portal.landingpage.entity.AuthzRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestListItem;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;

public class ReactivationRequestsListTest extends RoleBasedAccessIntegrationTest {

    private UserActivationRequest seedActivationRequest(UUID userProfileId) {
        UserActivationRequest request = new UserActivationRequest();
        request.setRequestId(UUID.randomUUID());
        request.setUserProfileId(userProfileId);
        request.setVersion(1);
        request.setStatus(uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus.IN_REVIEW);
        request.setComments("Integration test reactivation request");
        request.setActorEntraOid(UUID.randomUUID().toString());
        request.setActorRoleType(AuthzRoleType.PROVIDER_ADMIN);
        request.setCreatedAt(Instant.now());
        return userActivationRequestRepository.saveAndFlush(request);
    }

    @Test
    public void testProviderAdminGetsTrackHeading() throws Exception {
        EntraUser providerAdmin = firmUserManagers.getFirst();

        mockMvc.perform(get("/admin/users/reactivation-requests")
                        .with(userOauth2Login(providerAdmin)))
                .andExpect(status().isOk())
                .andExpect(view().name("reactivation-requests"))
                .andExpect(model().attribute("pageHeading", "Track reactivation requests"))
                .andExpect(model().attribute("manageMode", false));
    }

    @Test
    public void testProviderAdminResultsAreRestrictedToOwnFirm() throws Exception {
        EntraUser providerAdmin = firmUserManagers.getFirst();

        UUID providerAdminProfileId = providerAdmin.getUserProfiles().stream()
                .filter(profile -> profile != null && profile.isActiveProfile() && profile.getFirm() != null)
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();
        seedActivationRequest(providerAdminProfileId);

        EntraUser otherFirmUser = createExternalUserAtFirm("other-firm-user@test.com", testFirm1);
        UUID otherFirmProfileId = otherFirmUser.getUserProfiles().stream()
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();
        seedActivationRequest(otherFirmProfileId);

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .with(userOauth2Login(providerAdmin)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ReactivationRequestListItem> requests =
                (List<ReactivationRequestListItem>) result.getModelAndView().getModel().get("requests");

        Set<UUID> providerAdminFirmIds = providerAdmin.getUserProfiles().stream()
                .filter(profile -> profile != null && profile.isActiveProfile() && profile.getFirm() != null)
                .map(profile -> profile.getFirm().getId())
                .collect(Collectors.toSet());

        assertThat(requests).isNotEmpty();
        assertThat(requests).allMatch(request -> providerAdminFirmIds.contains(request.firmId()));
    }

    @Test
    public void testExternalUserAdminGetsManageHeadingAndDefaultInReviewFilter() throws Exception {
        EntraUser externalUserAdmin = externalUserAdmins.getFirst();

        mockMvc.perform(get("/admin/users/reactivation-requests")
                        .with(userOauth2Login(externalUserAdmin)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/reactivation-requests?size=10&page=1&sort=dateSubmitted&direction=desc&defaultStatusApplied=true&selectedRequestStatuses=IN_REVIEW"));

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .with(userOauth2Login(externalUserAdmin)))
                .andExpect(status().isOk())
                .andExpect(view().name("reactivation-requests"))
                .andExpect(model().attribute("pageHeading", "Manage reactivation requests"))
                .andExpect(model().attribute("manageMode", true))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ReactivationRequestStatus> statuses =
                (List<ReactivationRequestStatus>) result.getModelAndView().getModel().get("selectedRequestStatuses");

        assertThat(statuses).containsExactly(ReactivationRequestStatus.IN_REVIEW);
    }

    @Test
    public void testGlobalAdminAndSecurityResponseGetManageHeading() throws Exception {
        EntraUser globalAdmin = globalAdmins.getFirst();
        EntraUser securityResponse = securityResponseUsers.getFirst();

        mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .with(userOauth2Login(globalAdmin)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageHeading", "Manage reactivation requests"));

        mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .with(userOauth2Login(securityResponse)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageHeading", "Manage reactivation requests"));
    }

    @Test
    public void testUserWithoutPermissionCannotAccessReactivationRequestsPage() throws Exception {
        EntraUser userWithoutRoles = internalUsersNoRoles.getFirst();

        mockMvc.perform(get("/admin/users/reactivation-requests")
                        .with(userOauth2Login(userWithoutRoles)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testManageUsersPageContainsReactivationRequestsButton() throws Exception {
        EntraUser globalAdmin = globalAdmins.getFirst();

        mockMvc.perform(get("/admin/users")
                        .with(userOauth2Login(globalAdmin)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Reactivation requests")));
    }

    @Test
    public void testEmptyResultsReturnZeroTotalPagesAndNoPaginationControls() throws Exception {
        EntraUser globalAdmin = globalAdmins.getFirst();

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .with(userOauth2Login(globalAdmin)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("totalPages", 0))
                .andExpect(model().attribute("page", 0))
                .andExpect(model().attribute("totalRequests", 0L))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ReactivationRequestListItem> requests =
                (List<ReactivationRequestListItem>) result.getModelAndView().getModel().get("requests");
        assertThat(requests).isEmpty();

        String content = result.getResponse().getContentAsString();
        assertThat(content).doesNotContain("govuk-pagination__link");
    }

    @Test
    public void testDateSubmittedComesFromFirstVersionAndLastActivityFromLatestVersion() throws Exception {
        EntraUser providerAdmin = firmUserManagers.getFirst();
        UUID providerAdminProfileId = providerAdmin.getUserProfiles().stream()
                .filter(profile -> profile != null && profile.isActiveProfile() && profile.getFirm() != null)
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();

        UUID requestId = UUID.randomUUID();
        Instant originalSubmittedAt = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant latestActivityAt = Instant.now();

        UserActivationRequest v1 = new UserActivationRequest();
        v1.setRequestId(requestId);
        v1.setUserProfileId(providerAdminProfileId);
        v1.setVersion(1);
        v1.setStatus(uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus.IN_REVIEW);
        v1.setComments("Original submission");
        v1.setActorEntraOid(UUID.randomUUID().toString());
        v1.setActorRoleType(AuthzRoleType.PROVIDER_ADMIN);
        v1.setCreatedAt(originalSubmittedAt);
        userActivationRequestRepository.saveAndFlush(v1);

        UserActivationRequest v2 = new UserActivationRequest();
        v2.setRequestId(requestId);
        v2.setUserProfileId(providerAdminProfileId);
        v2.setVersion(2);
        v2.setStatus(uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus.INFORMATION_REQUIRED);
        v2.setComments("Follow up on original submission");
        v2.setActorEntraOid(UUID.randomUUID().toString());
        v2.setActorRoleType(AuthzRoleType.PROVIDER_ADMIN);
        v2.setCreatedAt(latestActivityAt);
        userActivationRequestRepository.saveAndFlush(v2);

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .with(userOauth2Login(providerAdmin)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ReactivationRequestListItem> requests =
                (List<ReactivationRequestListItem>) result.getModelAndView().getModel().get("requests");

        assertThat(requests).hasSize(1);
        ReactivationRequestListItem item = requests.getFirst();
        assertThat(item.dateSubmitted()).isEqualTo(LocalDate.ofInstant(originalSubmittedAt, ZoneId.systemDefault()));
        assertThat(item.lastActivity()).isEqualTo(LocalDate.ofInstant(latestActivityAt, ZoneId.systemDefault()));
        assertThat(item.dateSubmitted()).isNotEqualTo(item.lastActivity());
        assertThat(item.requestStatus()).isEqualTo(ReactivationRequestStatus.INFORMATION_REQUIRED);
    }

    @Test
    public void testSelectedUserTypesFiltersResultsByActorRoleType() throws Exception {
        EntraUser globalAdmin = globalAdmins.getFirst();
        UUID globalAdminProfileId = globalAdmin.getUserProfiles().stream()
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();

        seedActivationRequest(globalAdminProfileId);

        UserActivationRequest laaActorRequest = new UserActivationRequest();
        laaActorRequest.setRequestId(UUID.randomUUID());
        laaActorRequest.setUserProfileId(globalAdminProfileId);
        laaActorRequest.setVersion(1);
        laaActorRequest.setStatus(uk.gov.justice.laa.portal.landingpage.entity.ReactivationRequestStatus.IN_REVIEW);
        laaActorRequest.setComments("Raised by an LAA actor");
        laaActorRequest.setActorEntraOid(UUID.randomUUID().toString());
        laaActorRequest.setActorRoleType(AuthzRoleType.LAA);
        laaActorRequest.setCreatedAt(Instant.now());
        userActivationRequestRepository.saveAndFlush(laaActorRequest);

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .param("selectedUserTypes", "LAA")
                        .with(userOauth2Login(globalAdmin)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ReactivationRequestListItem> requests =
                (List<ReactivationRequestListItem>) result.getModelAndView().getModel().get("requests");

        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().actorRoleType()).isEqualTo(AuthzRoleType.LAA.getLabel());
    }
}
