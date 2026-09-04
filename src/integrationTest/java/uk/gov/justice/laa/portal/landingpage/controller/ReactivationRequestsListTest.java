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
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestListItem;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;

public class ReactivationRequestsListTest extends RoleBasedAccessIntegrationTest {

    private UserActivationRequest seedActivationRequest(UUID userId, UUID userProfileId) {
        UserActivationRequest request = new UserActivationRequest();
        request.setRequestId(UUID.randomUUID());
        request.setUserEntraId(userId);
        request.setUserProfileId(userProfileId);
        request.setVersion(1);
        request.setStatus(ReactivationRequestStatus.IN_REVIEW);
        request.setComments("Integration test reactivation request");
        request.setActorEntraOid(UUID.randomUUID().toString());
        request.setActorRoleType(ReactivationRoleType.PROVIDER_ADMIN);
        request.setCreatedAt(Instant.now());
        return userActivationRequestRepository.saveAndFlush(request);
    }

    @Test
    public void testProviderAdminGetsTrackHeading() throws Exception {
        EntraUser providerAdmin = firmUserManagers.getFirst();

        mockMvc.perform(get("/admin/users/reactivation-requests")
                        .with(userOauth2Login(providerAdmin)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/reactivation-requests?size=10&page=1&sort=dateSubmitted&direction=desc&defaultStatusApplied=true&selectedRequestStatuses=IN_REVIEW"));

        mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .with(userOauth2Login(providerAdmin)))
                .andExpect(status().isOk())
                .andExpect(view().name("reactivation-requests"))
                .andExpect(model().attribute("pageHeading", "Track reactivation requests"))
                .andExpect(model().attribute("manageMode", false))
                .andExpect(content().string(containsString("You can search by user name or email.")));
    }

    @Test
    public void testProviderAdminResultsAreRestrictedToOwnFirm() throws Exception {
        EntraUser providerAdmin = firmUserManagers.getFirst();

        UUID providerAdminProfileId = providerAdmin.getUserProfiles().stream()
                .filter(profile -> profile != null && profile.isActiveProfile() && profile.getFirm() != null)
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();
        seedActivationRequest(providerAdmin.getId(), providerAdminProfileId);

        EntraUser otherFirmUser = createExternalUserAtFirm("other-firm-user@test.com", testFirm1);
        UUID otherFirmProfileId = otherFirmUser.getUserProfiles().stream()
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();
        seedActivationRequest(otherFirmUser.getId(), otherFirmProfileId);

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
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
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "You can search by user name or email.")))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ReactivationRequestStatus> statuses =
                (List<ReactivationRequestStatus>) result.getModelAndView().getModel().get("selectedRequestStatuses");

        assertThat(statuses).containsExactly(ReactivationRequestStatus.IN_REVIEW);
    }

    @Test
    public void testExternalUserAdminCanSeeRequestsOutsideTheirFirmAndMultiFirm() throws Exception {
        EntraUser eua = externalUserAdmins.getFirst();

        // 1. Multi-firm user request
        EntraUser multiFirmUser = multiFirmUsers.getFirst();
        UUID multiFirmProfileId = multiFirmUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();
        seedActivationRequest(multiFirmUser.getId(), multiFirmProfileId);

        // 2. Different firm request
        EntraUser otherFirmUser = createExternalUserAtFirm("other-firm-user2@test.com", testFirm2);
        UUID otherFirmProfileId = otherFirmUser.getUserProfiles().stream()
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();
        seedActivationRequest(otherFirmUser.getId(), otherFirmProfileId);

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .with(userOauth2Login(eua)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ReactivationRequestListItem> requests =
                (List<ReactivationRequestListItem>) result.getModelAndView().getModel().get("requests");

        assertThat(requests).hasSizeGreaterThanOrEqualTo(2);
        assertThat(requests).extracting(ReactivationRequestListItem::userProfileId)
                .contains(multiFirmProfileId, otherFirmProfileId);
    }

    @Test
    public void testExternalUserSupportGetsTrackHeadingAndDefaultInReviewFilter() throws Exception {
        EntraUser externalUserSupport = externalUserSupportUsers.getFirst();

        mockMvc.perform(get("/admin/users/reactivation-requests")
                        .with(userOauth2Login(externalUserSupport)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/reactivation-requests?size=10&page=1&sort=dateSubmitted&direction=desc&defaultStatusApplied=true&selectedRequestStatuses=IN_REVIEW"));

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .with(userOauth2Login(externalUserSupport)))
                .andExpect(status().isOk())
                .andExpect(view().name("reactivation-requests"))
                .andExpect(model().attribute("pageHeading", "Track reactivation requests"))
                .andExpect(model().attribute("manageMode", false))
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
    public void testManageUsersPageContainsReactivationRequestsButtonForExternalUserSupport() throws Exception {
        EntraUser externalUserSupport = externalUserSupportUsers.getFirst();

        mockMvc.perform(get("/admin/users")
                        .with(userOauth2Login(externalUserSupport)))
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

        UserActivationRequest v1 = new UserActivationRequest();
        v1.setRequestId(requestId);
        v1.setUserEntraId(providerAdmin.getId());
        v1.setUserProfileId(providerAdminProfileId);
        v1.setVersion(1);
        v1.setStatus(ReactivationRequestStatus.IN_REVIEW);
        v1.setComments("Original submission");
        v1.setActorEntraOid(UUID.randomUUID().toString());
        v1.setActorRoleType(ReactivationRoleType.PROVIDER_ADMIN);
        v1.setCreatedAt(originalSubmittedAt);
        userActivationRequestRepository.saveAndFlush(v1);

        Instant latestActivityAt = Instant.now();
        UserActivationRequest v2 = new UserActivationRequest();
        v2.setRequestId(requestId);
        v2.setUserEntraId(providerAdmin.getId());
        v2.setUserProfileId(providerAdminProfileId);
        v2.setVersion(2);
        v2.setStatus(ReactivationRequestStatus.INFORMATION_REQUIRED);
        v2.setComments("Follow up on original submission");
        v2.setActorEntraOid(UUID.randomUUID().toString());
        v2.setActorRoleType(ReactivationRoleType.PROVIDER_ADMIN);
        v2.setCreatedAt(latestActivityAt);
        userActivationRequestRepository.saveAndFlush(v2);

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "INFORMATION_REQUIRED")
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
    public void testShowMultiFirmUsersFiltersResultsByThirdPartyTargetUser() throws Exception {
        EntraUser globalAdmin = globalAdmins.getFirst();
        EntraUser multiFirmUser = multiFirmUsers.getFirst();
        UUID multiFirmProfileId = multiFirmUser.getUserProfiles().stream()
                .map(profile -> profile.getId())
                .findFirst()
                .orElseThrow();

        UUID providerAdminProfileId = firmUserManagers.getFirst().getUserProfiles().stream()
                .map(profile -> profile.getId())
                .findFirst()
                .orElseThrow();
        seedActivationRequest(multiFirmUser.getId(), providerAdminProfileId);

        UserActivationRequest thirdPartyRequest = seedActivationRequest(multiFirmUser.getId(), multiFirmProfileId);

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .param("selectedRequestStatuses", "IN_REVIEW")
                        .param("showMultiFirmUsers", "true")
                        .with(userOauth2Login(globalAdmin)))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getContentAsString())
                .contains("3rd Party")
                .doesNotContain("value=\"PRIVILEGED\"");

        @SuppressWarnings("unchecked")
        List<ReactivationRequestListItem> requests =
                (List<ReactivationRequestListItem>) result.getModelAndView().getModel().get("requests");

        assertThat(requests).hasSize(2);
        assertThat(requests.getFirst().requestId()).isEqualTo(thirdPartyRequest.getRequestId());
        assertThat(requests.getFirst().userType()).isEqualTo("3rd Party");
    }

    @Test
    public void testExternalUserSupportSeesOnlyLaaOstAndLaaSupportRequests() throws Exception {
        EntraUser eus = externalUserSupportUsers.getFirst();
        UUID profileId = eus.getUserProfiles().stream()
                .map(UserProfile::getId)
                .findFirst()
                .orElseThrow();

        // 1. EUM Request (should be visible)
        UserActivationRequest eumReq = new UserActivationRequest();
        eumReq.setRequestId(UUID.randomUUID());
        eumReq.setUserEntraId(eus.getId());
        eumReq.setUserProfileId(profileId);
        eumReq.setVersion(1);
        eumReq.setStatus(ReactivationRequestStatus.IN_REVIEW);
        eumReq.setActorEntraOid(UUID.randomUUID().toString());
        eumReq.setActorRoleType(ReactivationRoleType.LAA_OST);
        eumReq.setComments("EUM Request");
        eumReq.setCreatedAt(Instant.now());
        userActivationRequestRepository.saveAndFlush(eumReq);

        // 2. EUS Request (should be visible)
        UserActivationRequest eusReq = new UserActivationRequest();
        eusReq.setRequestId(UUID.randomUUID());
        eusReq.setUserEntraId(eus.getId());
        eusReq.setUserProfileId(profileId);
        eusReq.setVersion(1);
        eusReq.setStatus(ReactivationRequestStatus.IN_REVIEW);
        eusReq.setActorEntraOid(UUID.randomUUID().toString());
        eusReq.setActorRoleType(ReactivationRoleType.LAA_SUPPORT);
        eusReq.setComments("EUS Request");
        eusReq.setCreatedAt(Instant.now());
        userActivationRequestRepository.saveAndFlush(eusReq);

        // 3. Provider Request (should NOT be visible)
        seedActivationRequest(eus.getId(), profileId);

        var result = mockMvc.perform(get("/admin/users/reactivation-requests")
                        .param("defaultStatusApplied", "true")
                        .with(userOauth2Login(eus)))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<ReactivationRequestListItem> requests =
                (List<ReactivationRequestListItem>) result.getModelAndView().getModel().get("requests");

        assertThat(requests).hasSize(2);
        assertThat(requests).extracting(ReactivationRequestListItem::actorRoleType)
                .containsExactlyInAnyOrder(ReactivationRoleType.LAA_OST.getDisplayName(), ReactivationRoleType.LAA_SUPPORT.getDisplayName());
    }
}
