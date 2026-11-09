package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.EntraAuthenticationContext;
import uk.gov.justice.laa.portal.landingpage.dto.EntraClaimData;
import uk.gov.justice.laa.portal.landingpage.dto.EntraServicePrincipalDto;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserPayloadDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClaimEnrichmentControllerIntegrationTest extends RoleBasedAccessIntegrationTest {

    private final List<EntraUser> createdUsers = new ArrayList<>();
    private final List<UserProfile> createdProfiles = new ArrayList<>();
    private final List<Office> createdOffices = new ArrayList<>();
    private final List<Firm> createdFirms = new ArrayList<>();

    private AppRole crownDefenseRole;

    @BeforeAll
    public void setupAll() {
        App app = appRepository.saveAndFlush(buildLaaApp("Claim for Crown Court Defence", "claim-app-entra-id", "claim-security-group"));
        AppRole appRole = buildLaaAppRole(app, "CrownDefenseAppRole");
        crownDefenseRole = appRoleRepository.saveAndFlush(appRole);
    }

    @Test
    void testCrownDefenseClaimIsPresentWhenUserHasLspFirmAndAdvocateExists() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Legal Services Provider Firm", "firmcode1", FirmType.LEGAL_SERVICES_PROVIDER, "Users Firm Office", "UsersFirmOfficeCode");
        buildAndPersistFirmAndOffice("Legal Services Provider Firm-ADVOCATE", "firmcode2", FirmType.ADVOCATE, "Advocate office", "AdvocateOfficeCode");
        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        EntraUser loggedInUser = userProfile.getEntraUser();
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).containsKey("CCCD_AGFS_ACCOUNTS");
        List<String> claim = (List<String>) claims.get("CCCD_AGFS_ACCOUNTS");
        assertThat(claim.size()).isEqualTo(1);
        assertThat(claim.getFirst()).isEqualTo("AdvocateOfficeCode");
    }

    @Test
    void testCrownDefenseClaimIsNotPresentWhenAppIsNotCccd() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Legal Services Provider Firm", "firmcode1", FirmType.LEGAL_SERVICES_PROVIDER, "Users Firm Office", "UsersFirmOfficeCode");
        buildAndPersistFirmAndOffice("Legal Services Provider Firm-ADVOCATE", "firmcode2", FirmType.ADVOCATE, "Advocate office", "AdvocateOfficeCode");
        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        // Set app to non-CCCD app.
        App app = appRepository.saveAndFlush(buildLaaApp("TestApp", "non-cccd-app-entra-id", "non-cccd-app-security-group"));
        AppRole appRole = buildLaaAppRole(app, "TestAppRole");
        appRole = appRoleRepository.saveAndFlush(appRole);
        userProfile.setAppRoles(Set.of(appRole));
        userProfileRepository.saveAndFlush(userProfile);
        EntraUser loggedInUser = userProfile.getEntraUser();
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, appRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).doesNotContainKey("CCCD_AGFS_ACCOUNTS");
    }

    @Test
    void testCrownDefenseClaimIsNotPresentWhenUserHasLspFirmAndAdvocateDoesNotExist() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Legal Services Provider Firm", "firmcode1", FirmType.LEGAL_SERVICES_PROVIDER, "Users Firm Office", "UsersFirmOfficeCode");
        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        EntraUser loggedInUser = userProfile.getEntraUser();
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).doesNotContainKey("CCCD_AGFS_ACCOUNTS");
    }

    @Test
    void testCrownDefenseClaimIsNotPresentWhenUserHasLspFirmAndAdvocateExistsButIsDisabled() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Legal Services Provider Firm", "firmcode1", FirmType.LEGAL_SERVICES_PROVIDER, "Users Firm Office", "UsersFirmOfficeCode");
        Firm advocateFirm = buildAndPersistFirmAndOffice("Legal Services Provider Firm-ADVOCATE", "firmcode2", FirmType.ADVOCATE, "Advocate office", "AdvocateOfficeCode");
        // Disable advocate firm
        advocateFirm.setEnabled(false);
        firmRepository.saveAndFlush(advocateFirm);
        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        EntraUser loggedInUser = userProfile.getEntraUser();
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).doesNotContainKey("CCCD_AGFS_ACCOUNTS");
    }

    @Test
    void testCrownDefenseClaimIsNotPresentWhenUserHasLspFirmAndAdvocateExistsButIsWrongFirmType() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Legal Services Provider Firm", "firmcode1", FirmType.LEGAL_SERVICES_PROVIDER, "Users Firm Office", "UsersFirmOfficeCode");
        buildAndPersistFirmAndOffice("Legal Services Provider Firm-ADVOCATE", "firmcode2", FirmType.LEGAL_SERVICES_PROVIDER, "Advocate office", "AdvocateOfficeCode");
        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        EntraUser loggedInUser = userProfile.getEntraUser();
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).doesNotContainKey("CCCD_AGFS_ACCOUNTS");
    }

    @Test
    void testCrownDefenseClaimIsNotPresentWhenUserHasLspFirmAndAdvocateExistsButHasWrongName() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Legal Services Provider Firm", "firmcode1", FirmType.LEGAL_SERVICES_PROVIDER, "Users Firm Office", "UsersFirmOfficeCode");
        buildAndPersistFirmAndOffice("Legal Services Provider Firm-NOTADVOCATE", "firmcode2", FirmType.ADVOCATE, "Advocate office", "AdvocateOfficeCode");
        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        EntraUser loggedInUser = userProfile.getEntraUser();
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).doesNotContainKey("CCCD_AGFS_ACCOUNTS");
    }

    @Test
    void testCrownDefenseClaimIsPresentWhenUserHasChambersFirmAndValidChildFirms() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Chambers Firm", "firmcode1", FirmType.CHAMBERS, "Chambers Firm Office", "ChambersFirmOfficeCode");
        // Valid child firm, will be in claim
        Firm childFirm1 = buildAndPersistFirmAndOffice("Chambers Child Firm 1", "firmcode2", FirmType.ADVOCATE, "Chambers Firm Child office 1", "ChambersChildOfficeCode1");
        childFirm1.setParentFirm(usersFirm);
        // Disabled child firm, will NOT be in claim.
        Firm childFirm2 = buildAndPersistFirmAndOffice("Chambers Child Firm 2", "firmcode3", FirmType.ADVOCATE, "Chambers Firm Child office 2", "ChambersChildOfficeCode2");
        childFirm2.setEnabled(false);
        childFirm2.setParentFirm(usersFirm);
        // Non-advocate child firm, will NOT be in claim.
        Firm childFirm3 = buildAndPersistFirmAndOffice("Chambers Child Firm 3", "firmcode4", FirmType.LEGAL_SERVICES_PROVIDER, "Chambers Firm Child office 3", "ChambersChildOfficeCode3");
        childFirm3.setParentFirm(usersFirm);
        // Another valid child firm, will be in claim.
        Firm childFirm4 = buildAndPersistFirmAndOffice("Chambers Child Firm 4", "firmcode5", FirmType.ADVOCATE, "Chambers Firm Child office 4", "ChambersChildOfficeCode4");
        childFirm4.setParentFirm(usersFirm);

        Set<Firm> children = Set.of(childFirm1, childFirm2, childFirm3, childFirm4);
        firmRepository.saveAll(children);
        usersFirm.setChildFirms(Set.of(childFirm1, childFirm2, childFirm3, childFirm4));
        firmRepository.saveAndFlush(usersFirm);

        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        EntraUser loggedInUser = userProfile.getEntraUser();
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).containsKey("CCCD_AGFS_ACCOUNTS");
        List<String> claim = (List<String>) claims.get("CCCD_AGFS_ACCOUNTS");
        assertThat(claim).doesNotContain("ChambersFirmOfficeCode");
        assertThat(claim).contains("ChambersChildOfficeCode1");
        assertThat(claim).doesNotContain("ChambersChildOfficeCode2");
        assertThat(claim).doesNotContain("ChambersChildOfficeCode3");
        assertThat(claim).contains("ChambersChildOfficeCode4");
    }

    @Test
    void testCrownDefenseClaimIsPresentWhenUserHasAdvocateFirmAndUnrestrictedOfficeAccess() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Advocate Firm", "firmcode1", FirmType.ADVOCATE, "Advocate Firm Office", "AdvocateFirmOfficeCode");

        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        userProfile.setUnrestrictedOfficeAccess(true);
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        EntraUser loggedInUser = userProfile.getEntraUser();
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).containsKey("CCCD_AGFS_ACCOUNTS");
        List<String> claim = (List<String>) claims.get("CCCD_AGFS_ACCOUNTS");
        assertThat(claim).contains("AdvocateFirmOfficeCode");
    }

    @Test
    void testCrownDefenseClaimIsNotPresentWhenUserHasAdvocateFirmButNoAssignedOffices() throws Exception {
        Firm usersFirm = buildAndPersistFirmAndOffice("Advocate Firm", "firmcode1", FirmType.ADVOCATE, "Advocate Firm Office", "AdvocateFirmOfficeCode");

        UserProfile userProfile = buildAndPersistUserAndProfile(usersFirm, UserType.EXTERNAL);
        userProfile.setUnrestrictedOfficeAccess(false);
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        EntraUser loggedInUser = userProfile.getEntraUser();
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).doesNotContainKey("CCCD_AGFS_ACCOUNTS");
    }

    @Test
    void testCrownDefenseClaimIsNotPresentWhenUserIsInternal() throws Exception {
        UserProfile userProfile = buildAndPersistUserAndProfile(null, UserType.INTERNAL);
        EntraUser loggedInUser = userProfile.getEntraUser();
        userProfile.setAppRoles(Set.of(crownDefenseRole));
        userProfileRepository.saveAndFlush(userProfile);
        final ClaimEnrichmentRequest request = buildClaimEnrichmentRequest(loggedInUser, crownDefenseRole.getApp());

        final MvcResult result = mockMvc.perform(post("/api/v1/claims/enrich")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request))
                        .with(csrf())
                        .with(userOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();

        final ClaimEnrichmentResponse response = mapper.readValue(result.getResponse().getContentAsString(), ClaimEnrichmentResponse.class);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getActions()).hasSize(1);
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().getFirst();
        Map<String, Object> claims = action.getClaims();
        assertThat(claims).doesNotContainKey("CCCD_AGFS_ACCOUNTS");
    }

    @AfterEach
    public void teardownOne() {
        userProfileRepository.deleteAll(createdProfiles);
        createdProfiles.clear();
        entraUserRepository.deleteAll(createdUsers);
        createdUsers.clear();
        for (Firm firm : createdFirms) {
            firm.setEnabled(false);
            firm.setOffices(new HashSet<>());
            firmRepository.saveAndFlush(firm);
        }
        officeRepository.deleteAll(createdOffices);
        createdOffices.clear();
        firmRepository.deleteAll(createdFirms);
        createdFirms.clear();
    }

    @AfterAll
    public void teardownAll() {
        App crownDefenseApp = crownDefenseRole.getApp();
        appRoleRepository.delete(crownDefenseRole);
        appRepository.delete(crownDefenseApp);
    }

    private UserProfile buildAndPersistUserAndProfile(Firm firm, UserType userType) {
        EntraUser user = buildEntraUser(UUID.randomUUID().toString(), "testclaimenrichment@test.com", "Test", "User");
        user = entraUserRepository.saveAndFlush(user);
        UserProfile userProfile = buildLaaUserProfile(user, userType, true);
        userProfile.setFirm(firm);
        userProfile = userProfileRepository.saveAndFlush(userProfile);
        createdUsers.add(user);
        createdProfiles.add(userProfile);
        return userProfile;
    }

    private Firm buildAndPersistFirmAndOffice(String firmName, String firmCode, FirmType firmType, String officeAddress, String officeCode) {
        Firm firm = buildFirm(firmName, firmCode);
        firm.setType(firmType);
        firm.setEnabled(false);
        firm = firmRepository.saveAndFlush(firm);
        Office office = buildOffice(firm, officeAddress, officeCode);
        office = officeRepository.saveAndFlush(office);
        firm.setOffices(new HashSet<>(Set.of(office)));
        firm.setEnabled(true);
        firm = firmRepository.saveAndFlush(firm);
        createdFirms.add(firm);
        createdOffices.add(office);
        return firm;
    }

    private ClaimEnrichmentRequest buildClaimEnrichmentRequest(EntraUser targetUser, App app) {
        return ClaimEnrichmentRequest.builder()
                .type("microsoft.graph.tokenIssuanceStart")
                .source("azure")
                .data(EntraClaimData.builder()
                        .tenantId("tenant-123")
                        .authenticationContext(EntraAuthenticationContext.builder()
                                .user(EntraUserPayloadDto.builder()
                                        .id(targetUser.getEntraOid())
                                        .userPrincipalName(targetUser.getEmail())
                                        .mail(targetUser.getEmail())
                                        .displayName(targetUser.getFirstName() + " " + targetUser.getLastName())
                                        .build())
                                .clientServicePrincipal(EntraServicePrincipalDto.builder()
                                        .appDisplayName(app.getName())
                                        .appId(app.getEntraAppId())
                                        .build())
                                .build())
                        .build())
                .build();
    }
}
