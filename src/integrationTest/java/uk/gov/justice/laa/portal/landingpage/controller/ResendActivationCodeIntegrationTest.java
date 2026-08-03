package uk.gov.justice.laa.portal.landingpage.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.InvitationStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ResendActivationCodeIntegrationTest extends RoleBasedAccessIntegrationTest {

    @Test
    public void testResendActivationCodeInAuditDetailIsShownWhenUserIsDisabled() throws Exception {
        testResendVerificationLink(this::userAuditDetailsGet);
    }

    @Test
    public void testResendActivationCodeInManageUserIsShownWhenUserIsDisabled() throws Exception {
        testResendVerificationLink(this::manageUserGet);
    }

    private void testResendVerificationLink(BiFunction<EntraUser, EntraUser, MvcResult> pageGetSupplier) {
        EntraUser accessedUser = externalUsersNoRoles.getFirst();

        // Disable User and set invitation status to INVITE_SENT.
        accessedUser.setEnabled(false);
        accessedUser.setInvitationStatus(InvitationStatus.INVITE_SENT);
        accessedUser = entraUserRepository.save(accessedUser);

        // Fetch audit details page.
        EntraUser loggedInUser = globalAdmins.getFirst();
        MvcResult result = pageGetSupplier.apply(loggedInUser, accessedUser);

        // Assert response params are populated.
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView).isNotNull();
        Map<String, Object> model = modelAndView.getModel();
        assertThat(model).isNotNull();

        // Assert resend link is visible.
        boolean showResendVerificationLink = (boolean) model.get("showResendVerificationLink");
        assertThat(showResendVerificationLink).isTrue();

        // Teardown - re-enable user and change invitation status back.
        accessedUser.setEnabled(true);
        accessedUser.setInvitationStatus(InvitationStatus.VERIFICATION_SUCCESS);
        entraUserRepository.save(accessedUser);
    }


    private MvcResult userAuditDetailsGet(EntraUser loggedInUser, EntraUser accessedUser) {
        try {
            return this.mockMvc.perform(get(String.format("/admin/users/audit/%s", accessedUser.getId()))
                            .with(userOauth2Login(loggedInUser)))
                            .andExpect(status().isOk())
                            .andReturn();
        } catch (Exception e) {
            return null;
        }
    }

    private MvcResult manageUserGet(EntraUser loggedInUser, EntraUser accessedUser) {
        UserProfile accessedUserProfile = accessedUser.getUserProfiles().stream().findFirst().orElseThrow();
        try {
            return this.mockMvc.perform(get(String.format("/admin/users/manage/%s", accessedUserProfile.getId()))
                            .with(userOauth2Login(loggedInUser)))
                    .andExpect(status().isOk())
                    .andReturn();
        } catch (Exception e) {
            return null;
        }
    }


}
