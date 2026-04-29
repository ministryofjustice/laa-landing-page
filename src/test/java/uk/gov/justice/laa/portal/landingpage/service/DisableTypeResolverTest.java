package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.DisableType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DisableTypeResolverTest {

    private final DisableTypeResolver resolver = new DisableTypeResolver();

    @Test
    void resolveFromRoles_nullList_returnsNone() {
        assertThat(resolver.resolveFromRoles(null)).isEqualTo(DisableType.NONE);
    }

    @Test
    void resolveFromRoles_emptyList_returnsNone() {
        assertThat(resolver.resolveFromRoles(List.of())).isEqualTo(DisableType.NONE);
    }

    @Test
    void resolveFromRoles_unknownRole_returnsNone() {
        assertThat(resolver.resolveFromRoles(List.of("Some Other Role"))).isEqualTo(DisableType.NONE);
    }

    @Test
    void resolveFromRoles_globalAdmin_returnsPrivileged() {
        assertThat(resolver.resolveFromRoles(List.of(AuthzRole.GLOBAL_ADMIN.getRoleName())))
                .isEqualTo(DisableType.PRIVILEGED);
    }

    @Test
    void resolveFromRoles_securityResponse_returnsPrivileged() {
        assertThat(resolver.resolveFromRoles(List.of(AuthzRole.SECURITY_RESPONSE.getRoleName())))
                .isEqualTo(DisableType.PRIVILEGED);
    }

    @Test
    void resolveFromRoles_externalUserManager_returnsLaa() {
        assertThat(resolver.resolveFromRoles(List.of(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName())))
                .isEqualTo(DisableType.LAA);
    }

    @Test
    void resolveFromRoles_externalUserAdmin_returnsLaa() {
        assertThat(resolver.resolveFromRoles(List.of(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())))
                .isEqualTo(DisableType.LAA);
    }

    @Test
    void resolveFromRoles_firmUserManager_returnsFirm() {
        assertThat(resolver.resolveFromRoles(List.of(AuthzRole.FIRM_USER_MANAGER.getRoleName())))
                .isEqualTo(DisableType.FIRM);
    }

    @Test
    void resolveFromRoles_globalAdminTakesPrecedenceOverFum() {
        assertThat(resolver.resolveFromRoles(List.of(
                AuthzRole.FIRM_USER_MANAGER.getRoleName(),
                AuthzRole.GLOBAL_ADMIN.getRoleName())))
                .isEqualTo(DisableType.PRIVILEGED);
    }

    @Test
    void resolveFromRoles_euaTakesPrecedenceOverFum() {
        assertThat(resolver.resolveFromRoles(List.of(
                AuthzRole.FIRM_USER_MANAGER.getRoleName(),
                AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())))
                .isEqualTo(DisableType.LAA);
    }

    @Test
    void resolveFromRoles_globalAdminTakesPrecedenceOverEum() {
        assertThat(resolver.resolveFromRoles(List.of(
                AuthzRole.EXTERNAL_USER_MANAGER.getRoleName(),
                AuthzRole.GLOBAL_ADMIN.getRoleName())))
                .isEqualTo(DisableType.PRIVILEGED);
    }
}
