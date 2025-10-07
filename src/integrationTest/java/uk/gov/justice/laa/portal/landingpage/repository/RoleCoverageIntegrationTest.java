package uk.gov.justice.laa.portal.landingpage.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import uk.gov.justice.laa.portal.landingpage.controller.RoleBasedAccessIntegrationTest;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

public class RoleCoverageIntegrationTest extends RoleBasedAccessIntegrationTest {
    @Test
    void findGlobalAdmins() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<UserProfile> globalAdmins = userProfileRepository.findInternalUserByAuthzRole("Global Admin", pageRequest);
        Assertions.assertThat(globalAdmins).isNotNull();
        Assertions.assertThat(globalAdmins).hasSize(1);
    }

    @Test
    void findFirmExternalUserManagers() {
        PageRequest pageRequest = PageRequest.of(0, 20);
        Page<UserProfile> firm1ExternalManager = userProfileRepository.findFirmUserByAuthzRoleAndFirm(testFirm1.getId(), "Firm User Manager", pageRequest);
        Assertions.assertThat(firm1ExternalManager).hasSize(5);
    }
}
