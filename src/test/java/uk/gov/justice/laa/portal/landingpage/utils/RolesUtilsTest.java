package uk.gov.justice.laa.portal.landingpage.utils;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.dto.AppRoleDto;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.laa.portal.landingpage.utils.UserRoleType.EXTERNAL_USER_ADMIN;
import static uk.gov.justice.laa.portal.landingpage.utils.UserRoleType.FIRM_USER_MANAGER;
import static uk.gov.justice.laa.portal.landingpage.utils.UserRoleType.GLOBAL_ADMIN;

class RolesUtilsTest {

    @Test
    void isCurrentProfileExternalUserAdmin() {
        assertTrue(RolesUtils.isCurrentProfileExternalUserAdmin(UserProfile.builder()
                .appRoles(Set.of(AppRole.builder()
                        .name(EXTERNAL_USER_ADMIN.getDescription())
                        .build()))
                .build()));

    }

    @Test
    void isNotCurrentProfileExternalUserAdmin() {
        assertFalse(RolesUtils.isCurrentProfileExternalUserAdmin(UserProfile.builder()
                .appRoles(Set.of(AppRole.builder()
                        .name("other role")
                        .build()))
                .build()));

    }

    @Test
    void isCurrentProfileGlobalAdmin() {
        assertTrue(RolesUtils.isCurrentProfileGlobalAdmin(UserProfile.builder()
                .appRoles(Set.of(AppRole.builder()
                        .name(GLOBAL_ADMIN.getDescription())
                        .build()))
                .build()));
    }

    @Test
    void isNotCurrentProfileGlobalAdmin() {
        assertFalse(RolesUtils.isCurrentProfileGlobalAdmin(UserProfile.builder()
                .appRoles(Set.of(AppRole.builder()
                        .name("no global role")
                        .build()))
                .build()));
    }

    @Test
    void isProvideAdmin() {

        assertTrue(RolesUtils.isProvideAdmin(List.of(AppRoleDto.builder()
                .name(FIRM_USER_MANAGER.getDescription())
                .build())));
    }

    @Test
    void isNotProvideAdmin() {

        assertTrue(RolesUtils.isProvideAdmin(List.of(AppRoleDto.builder()
                .name(FIRM_USER_MANAGER.getDescription())
                .build())));
    }
}