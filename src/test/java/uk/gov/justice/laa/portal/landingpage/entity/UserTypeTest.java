package uk.gov.justice.laa.portal.landingpage.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UserTypeTest {

    @Test
    public void testIsAdminUserType() {
        assertThat(UserType.ADMIN_TYPES).hasSize(2);
        assertThat(UserType.INTERNAL.isAdmin()).isTrue();
        assertThat(UserType.EXTERNAL_SINGLE_FIRM_ADMIN.isAdmin()).isTrue();
        assertThat(UserType.EXTERNAL_SINGLE_FIRM.isAdmin()).isFalse();
        assertThat(UserType.EXTERNAL_MULTI_FIRM.isAdmin()).isFalse();
    }
}
