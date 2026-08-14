package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.entity.DisableType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class UserActivationTrackingIntegrationTest extends RoleBasedAccessIntegrationTest {

    @AfterEach
    void tearDown() {
        userActivationRequestRepository.deleteAll();
    }

    @Nested
    public class ProviderAdminScenarios {

        /**
         * FUM Can Enable the user so no access to delegate screens
         */
        @Test
        public void cannotAccessDelegateReactivationForUserDisabledBySameFirm() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.FIRM, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isForbidden());
        }

        /**
         * FUM Can not access user from different firm, so no access to delegate activate page
         */
        @Test
        public void cannotAccessDelegateReactivationForDifferentFirm() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.FIRM, testFirm1);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationForSameFirmUserDisabledByPriv() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.PRIVILEGED, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void canAccessDelegateReactivationForSameFirmUserDisabledByEum() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void canAccessDelegateReactivationForSameFirmUserDisabledByNone() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.NONE, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void canAccessDelegateReactivationForSameFirmUserDisabledBySync() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.SYNC, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void canAccessDelegateReactivationForSameFirmUserDisabledByLaa() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        /**
         * FUM Can Enable the user so no access to delegate screens
         */
        @Test
        public void canAccessDelegateReactivationForSameFirmDisabledByEum() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void canAccessTrackRequestedByProviderAdminSameFirm() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);
            UserProfile activeProfile = externalUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElseThrow();
            createReactivateRequest(activeProfile.getId(), providerAdmin.getEntraOid(), ReactivationRoleType.PROVIDER_ADMIN);

            mockMvc.perform(get("/admin/user/delegate-reactivate/track/" + externalUser.getId())
                            .param("profileId", activeProfile.getId().toString())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user-tracking"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void canAccessTrackRequestedByExternalUserManager() throws Exception {
            EntraUser eum = externalOnlyUserManagers.getFirst();
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);
            UserProfile activeProfile = externalUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElseThrow();
            createReactivateRequest(activeProfile.getId(), eum.getEntraOid(), ReactivationRoleType.LAA_OST);

            mockMvc.perform(get("/admin/user/delegate-reactivate/track/" + externalUser.getId())
                            .param("profileId", activeProfile.getId().toString())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user-tracking"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void cannotAccessTrackRequestedByProviderAdminDifferentFirm() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm1);
            UserProfile activeProfile = externalUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElseThrow();
            createReactivateRequest(activeProfile.getId(), providerAdmin.getEntraOid(), ReactivationRoleType.PROVIDER_ADMIN);

            mockMvc.perform(get("/admin/user/delegate-reactivate/track/" + externalUser.getId())
                            .param("profileId", activeProfile.getId().toString())
                            .with(userOauth2Login(providerAdmin)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    public class ExternalUserManagerScenarios {

        @Test
        public void canAccessDelegateReactivationDisabledByFirm() throws Exception {
            EntraUser eum = internalWithExternalOnlyUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.FIRM, testFirm2);
            UserProfile activeProfile = externalUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElseThrow();

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .param("profileId", activeProfile.getId().toString())
                            .with(userOauth2Login(eum)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void cannotAccessDelegateReactivationDisabledByPriv() throws Exception {
            EntraUser eum = internalWithExternalOnlyUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.PRIVILEGED, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eum)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void canAccessDelegateReactivationDisabledByNone() throws Exception {
            EntraUser eum = internalWithExternalOnlyUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.NONE, testFirm2);
            UserProfile activeProfile = externalUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElseThrow();

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .param("profileId", activeProfile.getId().toString())
                            .with(userOauth2Login(eum)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void canAccessDelegateReactivationUserDisabledBySync() throws Exception {
            EntraUser eum = internalWithExternalOnlyUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.SYNC, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eum)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void canAccessDelegateReactivationUserDisabledByLaa() throws Exception {
            EntraUser eum = internalWithExternalOnlyUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eum)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void canAccessDelegateReactivationDisabledByEum() throws Exception {
            EntraUser eum = internalWithExternalOnlyUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eum)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }

        @Test
        public void cannotAccessTrackRequestedByProviderAdminSameFirm() throws Exception {
            EntraUser providerAdmin = firmUserManagers.getFirst();
            EntraUser eum = internalWithExternalOnlyUserManagers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);
            UserProfile activeProfile = externalUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElseThrow();
            createReactivateRequest(activeProfile.getId(), providerAdmin.getEntraOid(), ReactivationRoleType.PROVIDER_ADMIN);

            mockMvc.perform(get("/admin/user/delegate-reactivate/track/" + externalUser.getId())
                            .param("profileId", activeProfile.getId().toString())
                            .with(userOauth2Login(eum)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void canAccessTrackRequestedByExternalUserManager() throws Exception {
            EntraUser eum1 = internalWithExternalOnlyUserManagers.getFirst();
            EntraUser eum2 = internalWithExternalOnlyUserManagers.getLast();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);
            UserProfile activeProfile = externalUser.getUserProfiles().stream().filter(UserProfile::isActiveProfile).findFirst().orElseThrow();
            createReactivateRequest(activeProfile.getId(), eum1.getEntraOid(), ReactivationRoleType.LAA_OST);

            mockMvc.perform(get("/admin/user/delegate-reactivate/track/" + externalUser.getId())
                            .param("profileId", activeProfile.getId().toString())
                            .with(userOauth2Login(eum2)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("delegate-reactivate-user-tracking"))
                    .andExpect(model().attribute("pageTitle", "Delegate Reactivate User"));
        }
    }

    @Nested
    public class ExternalUserAdminScenarios {

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByFirm() throws Exception {
            EntraUser eua = externalUserAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.FIRM, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eua)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByPriv() throws Exception {
            EntraUser eua = externalUserAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.PRIVILEGED, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eua)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByNone() throws Exception {
            EntraUser eua = externalUserAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.NONE, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eua)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledBySync() throws Exception {
            EntraUser eua = externalUserAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.SYNC, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eua)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByLaa() throws Exception {
            EntraUser eua = externalUserAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eua)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationDisabledByEum() throws Exception {
            EntraUser eua = externalUserAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(eua)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    public class SecurityResponseScenarios {

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByFirm() throws Exception {
            EntraUser sr = securityResponseUsers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.FIRM, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(sr)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByPriv() throws Exception {
            EntraUser sr = securityResponseUsers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.PRIVILEGED, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(sr)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByNone() throws Exception {
            EntraUser sr = securityResponseUsers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.NONE, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(sr)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledBySync() throws Exception {
            EntraUser sr = securityResponseUsers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.SYNC, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(sr)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByLaa() throws Exception {
            EntraUser sr = securityResponseUsers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(sr)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationDisabledByEum() throws Exception {
            EntraUser sr = securityResponseUsers.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(sr)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    public class GlobalAdminScenarios {

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByFirm() throws Exception {
            EntraUser ga = globalAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.FIRM, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(ga)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByPriv() throws Exception {
            EntraUser ga = globalAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.PRIVILEGED, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(ga)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByNone() throws Exception {
            EntraUser ga = globalAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.NONE, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(ga)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledBySync() throws Exception {
            EntraUser ga = globalAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.SYNC, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(ga)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationUserDisabledByLaa() throws Exception {
            EntraUser ga = globalAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(ga)))
                    .andExpect(status().isForbidden());
        }

        @Test
        public void cannotAccessDelegateReactivationDisabledByEum() throws Exception {
            EntraUser ga = globalAdmins.getFirst();
            EntraUser externalUser = getEntraUserWith(DisableType.LAA, testFirm2);

            mockMvc.perform(get("/admin/user/delegate-reactivate/" + externalUser.getId())
                            .with(userOauth2Login(ga)))
                    .andExpect(status().isForbidden());
        }

    }

    private EntraUser getEntraUserWith(DisableType disableType, Firm firm) {
        List<UserProfile> allProfiles = disabledExternalUsersNoRoles.stream()
                .map(EntraUser::getUserProfiles)
                .flatMap(Collection::stream)
                .toList();

        return allProfiles.stream()
                .filter(u -> u.getEntraUser().getDisableType() == disableType)
                .filter(u -> u.getFirm().getId() == firm.getId()).findFirst()
                .map(UserProfile::getEntraUser)
                .orElseThrow();
    }

    private void createReactivateRequest(UUID userProfileId,
                                         String actorOid, ReactivationRoleType actorRoleType) {
        UserActivationRequest requestByEum = UserActivationRequest
                .builder()
                .requestId(UUID.randomUUID())
                .userProfileId(userProfileId)
                .version(1)
                .status(ReactivationRequestStatus.IN_REVIEW)
                .comments("Integration test reactivation request")
                .actorEntraOid(actorOid)
                .actorRoleType(actorRoleType)
                .createdAt(Instant.now())
                .build();
        userActivationRequestRepository.saveAndFlush(requestByEum);
    }

}
