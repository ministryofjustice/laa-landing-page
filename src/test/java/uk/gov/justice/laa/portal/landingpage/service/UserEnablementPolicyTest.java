package uk.gov.justice.laa.portal.landingpage.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;

@ExtendWith(MockitoExtension.class)
class UserEnablementPolicyTest {

    private final UserEnablementPolicy policy = new UserEnablementPolicy();

    private static final String GA = AuthzRole.GLOBAL_ADMIN.getRoleName();
    private static final String SR = AuthzRole.SECURITY_RESPONSE.getRoleName();
    private static final String EUM = AuthzRole.EXTERNAL_USER_MANAGER.getRoleName();
    private static final String EUA = AuthzRole.EXTERNAL_USER_ADMIN.getRoleName();
    private static final String FUM = AuthzRole.FIRM_USER_MANAGER.getRoleName();
    private static final String IUM = AuthzRole.INTERNAL_USER_MANAGER.getRoleName();

    @Nested
    class CanEnable {

        @Test
        void nullRoleType_internalDelegationRoles_returnTrue() {
            assertThat(policy.canEnable(null, List.of(IUM))).isTrue();
            assertThat(policy.canEnable(null, List.of(EUM))).isTrue();
            assertThat(policy.canEnable(null, List.of(EUA))).isTrue();
            assertThat(policy.canEnable(null, List.of(SR))).isTrue();
            assertThat(policy.canEnable(null, List.of(GA))).isTrue();
        }

        @Test
        void nullRoleType_providerAdminCannotEnable() {
            assertThat(policy.canEnable(null, List.of(FUM))).isFalse();
        }

        @Test
        void nullRoleType_noRoleCannotEnable() {
            assertThat(policy.canEnable(null, List.of())).isFalse();
        }

        // --- NONE role type (Manual User Sync — all roles permitted) ---

        @Test
        void none_gaCanEnable() {
            assertThat(policy.canEnable(RoleType.NONE, List.of(GA))).isTrue();
        }

        @Test
        void none_srCanEnable() {
            assertThat(policy.canEnable(RoleType.NONE, List.of(SR))).isTrue();
        }

        @Test
        void none_eumCanEnable() {
            assertThat(policy.canEnable(RoleType.NONE, List.of(EUM))).isTrue();
        }

        @Test
        void none_euaCanEnable() {
            assertThat(policy.canEnable(RoleType.NONE, List.of(EUA))).isTrue();
        }

        @Test
        void none_fumCanEnable() {
            assertThat(policy.canEnable(RoleType.NONE, List.of(FUM))).isTrue();
        }

        @Test
        void none_noRoleCanEnable() {
            assertThat(policy.canEnable(RoleType.NONE, List.of())).isTrue();
        }

        // --- SYNC actor role type (Automatic User Sync — EUM/EUA+ only) ---

        @Test
        void sync_gaCanEnable() {
            assertThat(policy.canEnable(RoleType.SYNC, List.of(GA))).isTrue();
        }

        @Test
        void sync_srCanEnable() {
            assertThat(policy.canEnable(RoleType.SYNC, List.of(SR))).isTrue();
        }

        @Test
        void sync_eumCanEnable() {
            assertThat(policy.canEnable(RoleType.SYNC, List.of(EUM))).isTrue();
        }

        @Test
        void sync_euaCanEnable() {
            assertThat(policy.canEnable(RoleType.SYNC, List.of(EUA))).isTrue();
        }

        @Test
        void sync_fumCannotEnable() {
            assertThat(policy.canEnable(RoleType.SYNC, List.of(FUM))).isFalse();
        }

        @Test
        void sync_noRoleCannotEnable() {
            assertThat(policy.canEnable(RoleType.SYNC, List.of())).isFalse();
        }

        // --- FIRM role type (Firm User Manager disabled the user) ---

        @Test
        void firm_fumCanEnable() {
            assertThat(policy.canEnable(RoleType.FIRM, List.of(FUM))).isTrue();
        }

        @Test
        void firm_eumCanEnable() {
            assertThat(policy.canEnable(RoleType.FIRM, List.of(EUM))).isTrue();
        }

        @Test
        void firm_euaCanEnable() {
            assertThat(policy.canEnable(RoleType.FIRM, List.of(EUA))).isTrue();
        }

        @Test
        void firm_srCanEnable() {
            assertThat(policy.canEnable(RoleType.FIRM, List.of(SR))).isTrue();
        }

        @Test
        void firm_gaCanEnable() {
            assertThat(policy.canEnable(RoleType.FIRM, List.of(GA))).isTrue();
        }

        @Test
        void firm_noRoleCannotEnable() {
            assertThat(policy.canEnable(RoleType.FIRM, List.of())).isFalse();
        }

        // --- LAA role type (External User Manager / External User Admin disabled the user) ---

        @Test
        void laa_eumCanEnable() {
            assertThat(policy.canEnable(RoleType.LAA, List.of(EUM))).isTrue();
        }

        @Test
        void laa_euaCanEnable() {
            assertThat(policy.canEnable(RoleType.LAA, List.of(EUA))).isTrue();
        }

        @Test
        void laa_srCanEnable() {
            assertThat(policy.canEnable(RoleType.LAA, List.of(SR))).isTrue();
        }

        @Test
        void laa_gaCanEnable() {
            assertThat(policy.canEnable(RoleType.LAA, List.of(GA))).isTrue();
        }

        @Test
        void laa_fumCannotEnable() {
            assertThat(policy.canEnable(RoleType.LAA, List.of(FUM))).isFalse();
        }

        @Test
        void laa_noRoleCannotEnable() {
            assertThat(policy.canEnable(RoleType.LAA, List.of())).isFalse();
        }

        // --- PRIVILEGED role type (Security Response / Global Admin disabled the user) ---

        @Test
        void privileged_gaCanEnable() {
            assertThat(policy.canEnable(RoleType.PRIVILEGED, List.of(GA))).isTrue();
        }

        @Test
        void privileged_srCanEnable() {
            assertThat(policy.canEnable(RoleType.PRIVILEGED, List.of(SR))).isTrue();
        }

        @Test
        void privileged_eumCannotEnable() {
            assertThat(policy.canEnable(RoleType.PRIVILEGED, List.of(EUM))).isFalse();
        }

        @Test
        void privileged_euaCannotEnable() {
            assertThat(policy.canEnable(RoleType.PRIVILEGED, List.of(EUA))).isFalse();
        }

        @Test
        void privileged_fumCannotEnable() {
            assertThat(policy.canEnable(RoleType.PRIVILEGED, List.of(FUM))).isFalse();
        }

        @Test
        void privileged_noRoleCannotEnable() {
            assertThat(policy.canEnable(RoleType.PRIVILEGED, List.of())).isFalse();
        }

        // --- Multi-role scenarios: highest delegation of the enabling user is used ---

        @Test
        void none_fumWithEumRole_canEnable() {
            // FUM alone cannot re-enable a NONE-disabled user, but EUM (higher delegation) can
            assertThat(policy.canEnable(RoleType.NONE, List.of(FUM, EUM))).isTrue();
        }

        @Test
        void none_fumWithSrRole_canEnable() {
            // FUM alone cannot re-enable a NONE-disabled user, but SR (higher delegation) can
            assertThat(policy.canEnable(RoleType.NONE, List.of(FUM, SR))).isTrue();
        }

        @Test
        void laa_fumWithEumRole_canEnable() {
            // FUM alone cannot re-enable a LAA-disabled user, but EUM (higher delegation) can
            assertThat(policy.canEnable(RoleType.LAA, List.of(FUM, EUM))).isTrue();
        }

        @Test
        void privileged_eumWithGaRole_canEnable() {
            // EUM alone cannot re-enable a PRIVILEGED-disabled user, but GA (higher delegation) can
            assertThat(policy.canEnable(RoleType.PRIVILEGED, List.of(EUM, GA))).isTrue();
        }

        @Test
        void privileged_fumWithEumRole_cannotEnable() {
            // Neither FUM nor EUM qualifies to re-enable a PRIVILEGED-disabled user
            assertThat(policy.canEnable(RoleType.PRIVILEGED, List.of(FUM, EUM))).isFalse();
        }
    }

    @Nested
    class RequiresSameFirmCheck {

        @Test
        void nonFirmType_returnsFalse() {
            assertThat(policy.requiresSameFirmCheck(null, List.of(FUM))).isFalse();
            assertThat(policy.requiresSameFirmCheck(RoleType.NONE, List.of(FUM))).isFalse();
            assertThat(policy.requiresSameFirmCheck(RoleType.LAA, List.of(FUM))).isFalse();
            assertThat(policy.requiresSameFirmCheck(RoleType.PRIVILEGED, List.of(FUM))).isFalse();
        }

        @Test
        void firm_fumOnly_requiresSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(RoleType.FIRM, List.of(FUM))).isTrue();
        }

        @Test
        void firm_gaBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(RoleType.FIRM, List.of(GA))).isFalse();
        }

        @Test
        void firm_srBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(RoleType.FIRM, List.of(SR))).isFalse();
        }

        @Test
        void firm_eumBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(RoleType.FIRM, List.of(EUM))).isFalse();
        }

        @Test
        void firm_euaBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(RoleType.FIRM, List.of(EUA))).isFalse();
        }

        @Test
        void firm_fumWithHigherDelegation_bypassesSameFirmCheck() {
            // FUM also holds EUA — higher delegation overrides the same-firm requirement
            assertThat(policy.requiresSameFirmCheck(RoleType.FIRM, List.of(FUM, EUA))).isFalse();
        }
    }
}
