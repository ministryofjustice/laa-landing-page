package uk.gov.justice.laa.portal.landingpage.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class RoleAssignmentIdTest extends BaseEntityTest {
    @Test
    public void roleAssignmentIdTest() {

        RoleAssignmentId roleAssignmentId = new RoleAssignmentId(UUID.fromString("f2e86ffa-1223-467a-9f5a-93c9da337a6d"),
                UUID.fromString("0e1ea524-16d0-4fb4-b909-86d03873353f"));

        Assertions.assertThat(roleAssignmentId).isNotNull();
        Assertions.assertThat(roleAssignmentId.getAssigningRole()).isNotNull();
        Assertions.assertThat(roleAssignmentId.getAssigningRole()).isEqualTo(UUID.fromString("f2e86ffa-1223-467a-9f5a-93c9da337a6d"));
        Assertions.assertThat(roleAssignmentId.getAssignableRole()).isNotNull();
        Assertions.assertThat(roleAssignmentId.getAssignableRole()).isEqualTo(UUID.fromString("0e1ea524-16d0-4fb4-b909-86d03873353f"));
    }

}
