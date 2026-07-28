package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.service.TechServicesClient;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesUser;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserAccountStatusRefreshIntegrationTest extends RoleBasedAccessIntegrationTest {

    @MockitoBean
    private TechServicesClient techServicesClient;

    @Test
    public void testUserEnabledStatusIsChangedToDisabledWhenEntraSaysDisabled() throws Exception {
        EntraUser accessedUser = externalUsersNoRoles.getFirst();

        // Setup Mock
        when(techServicesClient.getUser(any())).thenReturn(buildGetUserResponse(false));

        // Fetch audit user drilldown.
        EntraUser loggedInUser = globalAdmins.getFirst();
        auditUserDetailsGet(loggedInUser, accessedUser);

        // Get latest user details from DB
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isFalse();

        // Teardown
        accessedUser.setEnabled(true);
        entraUserRepository.save(accessedUser);
    }

    @Test
    public void testUserEnabledStatusIsChangedToEnabledWhenEntraSaysEnabled() throws Exception {
        EntraUser accessedUser = externalUsersNoRoles.getFirst();

        // Disable accessed user in DB
        accessedUser.setEnabled(false);
        accessedUser = entraUserRepository.save(accessedUser);

        // Setup Mock
        when(techServicesClient.getUser(any())).thenReturn(buildGetUserResponse(true));

        // Fetch audit user drilldown.
        EntraUser loggedInUser = globalAdmins.getFirst();
        auditUserDetailsGet(loggedInUser, accessedUser);

        // Get latest user details from DB
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    @Test
    public void testUserEnabledStatusIsNotChangedWhenEntraMatchesSilas() throws Exception {
        EntraUser accessedUser = externalUsersNoRoles.getFirst();

        // Setup Mock
        when(techServicesClient.getUser(any())).thenReturn(buildGetUserResponse(true));

        // Fetch audit user drilldown.
        EntraUser loggedInUser = globalAdmins.getFirst();
        auditUserDetailsGet(loggedInUser, accessedUser);

        // Get latest user details from DB
        accessedUser = entraUserRepository.findById(accessedUser.getId()).orElseThrow();
        assertThat(accessedUser.isEnabled()).isTrue();
    }

    private TechServicesApiResponse<GetUserResponse> buildGetUserResponse(boolean enabled) {
        TechServicesUser techServicesUser = TechServicesUser.builder().accountEnabled(enabled).build();
        GetUserResponse getUserResponse = GetUserResponse.builder().success(true).message("").user(techServicesUser).build();
        return TechServicesApiResponse.success(getUserResponse);
    }

    private MvcResult auditUserDetailsGet(EntraUser loggedInUser, EntraUser accessedUser) throws Exception {
        return mockMvc.perform(get(String.format("/admin/users/audit/%s?isEntraId=true", accessedUser.getId()))
                .with(defaultOauth2Login(loggedInUser)))
                .andExpect(status().isOk())
                .andReturn();
    }
}
