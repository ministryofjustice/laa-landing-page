package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.AppRoleRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

class UserFirmReassignmentIntegrationTest extends BaseIntegrationTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected FirmRepository firmRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected AppRoleRepository appRoleRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected UserService userService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository officeRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected jakarta.persistence.EntityManager entityManager;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    protected org.springframework.transaction.PlatformTransactionManager transactionManager;

    private EntraUser externalUser;
    private UserProfile externalUserProfile;
    private Firm firm1;
    private Firm firm2;
    private EntraUser adminUser;

    @BeforeAll
    @Override
    public void beforeAll() {
        setupFirmsAndUsers();
    }

    private void setupFirmsAndUsers() {
        org.springframework.transaction.TransactionStatus txStatus = 
            transactionManager.getTransaction(new org.springframework.transaction.support.DefaultTransactionDefinition());
        try {
            // Clean up
            userProfileRepository.deleteAll();
            entraUserRepository.deleteAll();
            officeRepository.deleteAll();
            firmRepository.deleteAll();

            // Create firms with offices (firms must have at least one office)
            // Disable triggers temporarily to avoid constraint issues during setup
            entityManager.createNativeQuery("SET session_replication_role = replica").executeUpdate();
            
            firm1 = firmRepository.save(buildFirm("ABC Law Firm", "ABC001"));
            var office1 = officeRepository.save(buildOffice(firm1, "Main Office", "123 Main St", "555-0001", "ABC-MAIN"));
            firm1.getOffices().add(office1);

            firm2 = firmRepository.save(buildFirm("XYZ Legal Services", "XYZ001"));
            var office2 = officeRepository.save(buildOffice(firm2, "Main Office", "456 Legal Ave", "555-0002", "XYZ-MAIN"));
            firm2.getOffices().add(office2);
            
            firmRepository.flush();
            officeRepository.flush();
            
            // Re-enable triggers
            entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();

            // Create external user
            externalUser = buildEntraUser(generateEntraId(), "external.user@test.com", "External", "User");
            UserProfile profile = buildLaaUserProfile(externalUser, UserType.EXTERNAL, true);
            profile.setFirm(firm1);
            profile.setAppRoles(new HashSet<>());
            externalUser.setUserProfiles(Set.of(profile));
            profile.setEntraUser(externalUser);
            externalUser = entraUserRepository.saveAndFlush(externalUser);
            externalUserProfile = profile;

            // Create admin user with EDIT_USER_FIRM permission (External User Admin role has this permission)
            adminUser = buildEntraUser(generateEntraId(), "admin@test.com", "Admin", "User");
            UserProfile adminProfile = buildLaaUserProfile(adminUser, UserType.INTERNAL, true);
            AppRole externalUserAdminRole = appRoleRepository.findAllWithPermissions().stream()
                    .filter(AppRole::isAuthzRole)
                    .filter(role -> role.getName().equals("External User Admin"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find External User Admin role"));
            adminProfile.setAppRoles(Set.of(externalUserAdminRole));
            adminUser.setUserProfiles(Set.of(adminProfile));
            adminProfile.setEntraUser(adminUser);
            adminUser = entraUserRepository.saveAndFlush(adminUser);
            
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw new RuntimeException("Failed to setup test data", e);
        }
    }

    @AfterAll
    @Override
    protected void baseAfterAll() {
        org.springframework.transaction.TransactionStatus txStatus = 
            transactionManager.getTransaction(new org.springframework.transaction.support.DefaultTransactionDefinition());
        try {
            // Disable triggers temporarily during cleanup
            entityManager.createNativeQuery("SET session_replication_role = replica").executeUpdate();
            
            userProfileRepository.deleteAll();
            entraUserRepository.deleteAll();
            officeRepository.deleteAll();  // Delete offices before firms due to foreign key constraint
            firmRepository.deleteAll();
            
            // Re-enable triggers
            entityManager.createNativeQuery("SET session_replication_role = DEFAULT").executeUpdate();
            
            transactionManager.commit(txStatus);
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw new RuntimeException("Failed to cleanup test data", e);
        }
    }

    @Nested
    class ShowFirmReassignmentPage {

        @Test
        void shouldDisplayReassignmentPage_whenUserExists() throws Exception {
            MvcResult result = mockMvc.perform(get("/admin/users/reassign-firm/" + externalUserProfile.getId())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/select-firm"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attributeExists("firmReassignmentForm"))
                    .andReturn();

            assertThat(result.getModelAndView()).isNotNull();
        }

        @Test
        void shouldReturnError_whenUserNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/admin/users/reassign-firm/" + nonExistentId)
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void shouldReturnError_whenUserIsInternal() throws Exception {
            // Create internal user
            EntraUser internalUser = buildEntraUser(generateEntraId(), "internal.user@test.com", "Internal", "User");
            UserProfile internalProfile = buildLaaUserProfile(internalUser, UserType.INTERNAL, true);
            internalProfile.setAppRoles(new HashSet<>());
            internalUser.setUserProfiles(Set.of(internalProfile));
            internalProfile.setEntraUser(internalUser);
            internalUser = entraUserRepository.saveAndFlush(internalUser);

            mockMvc.perform(get("/admin/users/reassign-firm/" + internalProfile.getId())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        void shouldPrePopulateForm_whenComingFromChangeLinkWithParams() throws Exception {
            MvcResult result = mockMvc.perform(get("/admin/users/reassign-firm/" + externalUserProfile.getId())
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .param("reason", "Test reason")
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/select-firm"))
                    .andExpect(model().attributeExists("preservedReason"))
                    .andReturn();

            assertThat(result.getModelAndView()).isNotNull();
        }

        @Test
        void shouldRedirectToLogin_whenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/admin/users/reassign-firm/" + externalUserProfile.getId()))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        void shouldDenyAccess_whenUserLacksPermission() throws Exception {
            // Create a user without EDIT_USER_FIRM permission
            EntraUser unauthorizedUser = buildEntraUser(generateEntraId(), "unauthorized@test.com", "Unauth", "User");
            UserProfile unauthorizedProfile = buildLaaUserProfile(unauthorizedUser, UserType.INTERNAL, true);
            unauthorizedProfile.setAppRoles(new HashSet<>()); // No roles = no permissions
            unauthorizedUser.setUserProfiles(Set.of(unauthorizedProfile));
            unauthorizedProfile.setEntraUser(unauthorizedUser);
            unauthorizedUser = entraUserRepository.saveAndFlush(unauthorizedUser);

            mockMvc.perform(get("/admin/users/reassign-firm/" + externalUserProfile.getId())
                    .with(userOauth2Login(unauthorizedUser)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class ProcessFirmSelection {

        @Test
        void shouldRedirectToReasonPage_whenValidFirmSelected() throws Exception {
            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/select-firm")
                    .param("firmSearch", firm2.getName())
                    .param("selectedFirmId", firm2.getId().toString())
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/reason*"));
        }

        @Test
        void shouldReturnError_whenNoFirmSelected() throws Exception {
            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/select-firm")
                    .param("firmSearch", "Some Firm")
                    .param("selectedFirmId", "")  // Empty firm ID
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/select-firm"));
        }

        @Test
        void shouldReturnError_whenUserNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(post("/admin/users/reassign-firm/" + nonExistentId + "/select-firm")
                    .param("firmSearch", firm2.getName())
                    .param("selectedFirmId", firm2.getId().toString())
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users/manage/" + nonExistentId));
        }

        @Test
        void shouldReturnError_whenFirmNotFound() throws Exception {
            UUID nonExistentFirmId = UUID.randomUUID();

            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/select-firm")
                    .param("firmSearch", "Non-existent Firm")
                    .param("selectedFirmId", nonExistentFirmId.toString())
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/select-firm"));
        }
    }

    @Nested
    class ShowReasonPage {

        @Test
        void shouldDisplayReasonPage_whenValidParameters() throws Exception {
            mockMvc.perform(get("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/reason")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/reason"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attributeExists("selectedFirmId"))
                    .andExpect(model().attributeExists("selectedFirmName"))
                    .andExpect(model().attributeExists("reassignmentReasonForm"));
        }

        @Test
        void shouldPrePopulateReason_whenExistingReasonProvided() throws Exception {
            String existingReason = "Previous reason";

            MvcResult result = mockMvc.perform(get("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/reason")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .param("reason", existingReason)
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/reason"))
                    .andReturn();

            assertThat(result.getModelAndView()).isNotNull();
        }

        @Test
        void shouldReturnError_whenUserNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/admin/users/reassign-firm/" + nonExistentId + "/reason")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"));
        }
    }

    @Nested
    class ProcessReasonSubmission {

        @Test
        void shouldRedirectToCheckAnswers_whenValidReasonProvided() throws Exception {
            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/reason")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("firmSearch", firm2.getName())
                    .param("reason", "Correction of assignment error")
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/check-answers*"));
        }

        @Test
        void shouldReturnError_whenReasonIsEmpty() throws Exception {
            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/reason")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("firmSearch", firm2.getName())
                    .param("reason", "")
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/reason"));
        }

        @Test
        void shouldReturnError_whenUserNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(post("/admin/users/reassign-firm/" + nonExistentId + "/reason")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("firmSearch", firm2.getName())
                    .param("reason", "Test reason")
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users/manage/" + nonExistentId));
        }
    }

    @Nested
    class ShowCheckAnswersPage {

        @Test
        void shouldDisplayCheckAnswersPage_whenValidParameters() throws Exception {
            mockMvc.perform(get("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/check-answers")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .param("reason", "Test reason")
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/check-answers"))
                    .andExpect(model().attributeExists("user"))
                    .andExpect(model().attributeExists("selectedFirmId"))
                    .andExpect(model().attributeExists("selectedFirmName"))
                    .andExpect(model().attributeExists("reason"));
        }

        @Test
        void shouldReturnError_whenUserNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/admin/users/reassign-firm/" + nonExistentId + "/check-answers")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .param("reason", "Test reason")
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"));
        }
    }

    @Nested
    class ProcessFirmReassignment {

        @Test
        void shouldRedirectToConfirmation_whenReassignmentSuccessful() throws Exception {
            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/confirmation")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .param("reason", "Correction of firm assignment error")
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/confirmation"));

            // Verify the user was reassigned
            UserProfile updatedProfile = userProfileRepository.findById(externalUserProfile.getId()).orElseThrow();
            assertThat(updatedProfile.getFirm().getId()).isEqualTo(firm2.getId());
        }

        @Test
        void shouldReturnError_whenReasonIsEmpty() throws Exception {
            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/confirmation")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .param("reason", "")
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/check-answers"))
                    .andExpect(model().attributeExists("errorMessage"));
        }

        @Test
        @Transactional
        void shouldReturnError_whenUserAlreadyInTargetFirm() throws Exception {
            // First, verify the user's current firm assignment
            // Need to reload from DB as previous tests may have changed the firm
            UserProfile currentProfile = userProfileRepository.findById(externalUserProfile.getId()).orElseThrow();
            UUID currentFirmId = currentProfile.getFirm().getId();
            String currentFirmName = currentProfile.getFirm().getName();
            
            // Try to reassign to the same firm - the service will throw an exception which gets caught and shown in the view
            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/confirmation")
                    .param("selectedFirmId", currentFirmId.toString())
                    .param("selectedFirmName", currentFirmName)
                    .param("reason", "Test reason")
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/check-answers"));
        }

        @Test
        void shouldReturnError_whenUserNotFound() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(post("/admin/users/reassign-firm/" + nonExistentId + "/confirmation")
                    .param("selectedFirmId", firm2.getId().toString())
                    .param("selectedFirmName", firm2.getName())
                    .param("reason", "Test reason")
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/admin/users/manage/" + nonExistentId));
        }

        @Test
        void shouldReturnError_whenFirmNotFound() throws Exception {
            UUID nonExistentFirmId = UUID.randomUUID();

            mockMvc.perform(post("/admin/users/reassign-firm/" + externalUserProfile.getId() + "/confirmation")
                    .param("selectedFirmId", nonExistentFirmId.toString())
                    .param("selectedFirmName", "Non-existent Firm")
                    .param("reason", "Test reason")
                    .with(csrf())
                    .with(userOauth2Login(adminUser)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("reassign-firm/check-answers"))
                    .andExpect(model().attributeExists("errorMessage"));
        }
    }
}
