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

    @Test
    public void testInternalTypes() {
        assertThat(UserType.INTERNAL_TYPES).hasSize(2);
        assertThat(UserType.INTERNAL_TYPES).contains(UserType.INTERNAL);
        assertThat(UserType.INTERNAL_TYPES).contains(UserType.GLOBAL_ADMIN);
    }

    @Test
    public void testExternalTypes() {
        assertThat(UserType.EXTERNAL_TYPES).hasSize(3);
        assertThat(UserType.EXTERNAL_TYPES).contains(UserType.EXTERNAL_SINGLE_FIRM);
        assertThat(UserType.EXTERNAL_TYPES).contains(UserType.EXTERNAL_SINGLE_FIRM_ADMIN);
        assertThat(UserType.EXTERNAL_TYPES).contains(UserType.EXTERNAL_MULTI_FIRM);
    }
}
