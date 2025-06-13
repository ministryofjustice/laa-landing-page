package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AppRoleDtoTest {

    @Test
    public void testEqualsIsTrueWhenIdsAreSame() {
        AppRoleDto appRoleDto1 = new AppRoleDto();
        appRoleDto1.setId("testId");
        AppRoleDto appRoleDto2 = new AppRoleDto();
        appRoleDto2.setId("testId");
        assertThat(appRoleDto1.equals(appRoleDto2)).isTrue();
    }

    @Test
    public void testEqualsIsFalseWhenIdsAreDifferent() {
        AppRoleDto appRoleDto1 = new AppRoleDto();
        appRoleDto1.setId("testId1");
        AppRoleDto appRoleDto2 = new AppRoleDto();
        appRoleDto2.setId("testId2");
        assertThat(appRoleDto1.equals(appRoleDto2)).isFalse();
    }
}
