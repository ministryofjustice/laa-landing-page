package uk.gov.justice.laa.portal.landingpage.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class RoleAssignmentTest extends BaseEntityTest {
    @Test
    public void roleAssignmentTest() {
        AppRole role1 = AppRole.builder().name("Test Role 1").description("description 1").build();
        AppRole role2 = AppRole.builder().name("Test Role 2").description("description 2").build();

        RoleAssignment roleAssignment = new RoleAssignment(role1, role2);

        Assertions.assertThat(roleAssignment).isNotNull();
        Assertions.assertThat(roleAssignment.getAssigningRole()).isNotNull();
        Assertions.assertThat(roleAssignment.getAssigningRole()).isEqualTo(role1);
        Assertions.assertThat(roleAssignment.getAssignableRole()).isNotNull();
        Assertions.assertThat(roleAssignment.getAssignableRole()).isEqualTo(role2);
    }
}
