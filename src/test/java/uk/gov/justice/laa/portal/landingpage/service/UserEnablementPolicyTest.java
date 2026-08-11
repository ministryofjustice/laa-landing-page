package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.DisableType;

@ExtendWith(MockitoExtension.class)
class UserEnablementPolicyTest {

    private final UserEnablementPolicy policy = new UserEnablementPolicy();

    private static final String GA = AuthzRole.GLOBAL_ADMIN.getRoleName();
    private static final String SR = AuthzRole.SECURITY_RESPONSE.getRoleName();
    private static final String EUM = AuthzRole.EXTERNAL_USER_MANAGER.getRoleName();
    private static final String EUA = AuthzRole.EXTERNAL_USER_ADMIN.getRoleName();
    private static final String EUS = AuthzRole.EXTERNAL_USER_SUPPORT.getRoleName();
    private static final String FUM = AuthzRole.FIRM_USER_MANAGER.getRoleName();
    private static final String IUM = AuthzRole.INTERNAL_USER_MANAGER.getRoleName();

    @Nested
    class CanEnable {

        @Test
        void nullDisableType_internalDelegationRoles_returnTrue() {
            assertThat(policy.canEnable(null, List.of(IUM))).isTrue();
            assertThat(policy.canEnable(null, List.of(EUM))).isFalse();
            assertThat(policy.canEnable(null, List.of(EUA))).isTrue();
            assertThat(policy.canEnable(null, List.of(EUS))).isFalse();
            assertThat(policy.canEnable(null, List.of(SR))).isTrue();
            assertThat(policy.canEnable(null, List.of(GA))).isTrue();
        }

        @Test
        void nullDisableType_providerAdminCannotEnable() {
            assertThat(policy.canEnable(null, List.of(FUM))).isFalse();
        }

        @Test
        void nullDisableType_noRoleCannotEnable() {
            assertThat(policy.canEnable(null, List.of())).isFalse();
        }

        // --- NONE disable type (Manual User Sync — all roles permitted) ---

        @Test
        void none_gaCanEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(GA))).isTrue();
        }

        @Test
        void none_srCanEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(SR))).isTrue();
        }

        @Test
        void none_eumCannotEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(EUM))).isFalse();
        }

        @Test
        void none_euaCanEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(EUA))).isTrue();
        }

        @Test
        void none_eusCannotEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(EUS))).isFalse();
        }

        @Test
        void none_fumCannotEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(FUM))).isFalse();
        }

        @Test
        void none_noRoleCannotEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of())).isFalse();
        }

        // --- SYNC disable type (Automatic User Sync — EUM/EUA+ only) ---

        @Test
        void sync_gaCanEnable() {
            assertThat(policy.canEnable(DisableType.SYNC, List.of(GA))).isTrue();
        }

        @Test
        void sync_srCanEnable() {
            assertThat(policy.canEnable(DisableType.SYNC, List.of(SR))).isTrue();
        }

        @Test
        void sync_eumCannotEnable() {
            assertThat(policy.canEnable(DisableType.SYNC, List.of(EUM))).isFalse();
        }

        @Test
        void sync_euaCanEnable() {
            assertThat(policy.canEnable(DisableType.SYNC, List.of(EUA))).isTrue();
        }

        @Test
        void sync_eusCannotEnable() {
            assertThat(policy.canEnable(DisableType.SYNC, List.of(EUS))).isFalse();
        }

        @Test
        void sync_fumCannotEnable() {
            assertThat(policy.canEnable(DisableType.SYNC, List.of(FUM))).isFalse();
        }

        @Test
        void sync_noRoleCannotEnable() {
            assertThat(policy.canEnable(DisableType.SYNC, List.of())).isFalse();
        }

        // --- FIRM disable type (Firm User Manager disabled the user) ---

        @Test
        void firm_fumCanEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(FUM))).isTrue();
        }

        @Test
        void firm_eumCannotEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(EUM))).isFalse();
        }

        @Test
        void firm_euaCanEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(EUA))).isTrue();
        }

        @Test
        void firm_eusCannotEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(EUS))).isFalse();
        }

        @Test
        void firm_srCanEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(SR))).isTrue();
        }

        @Test
        void firm_gaCanEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(GA))).isTrue();
        }

        @Test
        void firm_noRoleCannotEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of())).isFalse();
        }

        // --- LAA disable type (External User Manager / External User Admin disabled the user) ---

        @Test
        void laa_eumCannotEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of(EUM))).isFalse();
        }

        @Test
        void laa_euaCanEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of(EUA))).isTrue();
        }

        @Test
        void laa_eusCannotEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of(EUS))).isFalse();
        }

        @Test
        void laa_srCanEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of(SR))).isTrue();
        }

        @Test
        void laa_gaCanEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of(GA))).isTrue();
        }

        @Test
        void laa_fumCannotEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of(FUM))).isFalse();
        }

        @Test
        void laa_noRoleCannotEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of())).isFalse();
        }

        // --- PRIVILEGED disable type (Security Response / Global Admin disabled the user) ---

        @Test
        void privileged_gaCanEnable() {
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(GA))).isTrue();
        }

        @Test
        void privileged_srCanEnable() {
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(SR))).isTrue();
        }

        @Test
        void privileged_eumCannotEnable() {
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(EUM))).isFalse();
        }

        @Test
        void privileged_euaCannotEnable() {
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(EUA))).isFalse();
        }

        @Test
        void privileged_eusCannotEnable() {
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(EUS))).isFalse();
        }

        @Test
        void privileged_fumCannotEnable() {
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(FUM))).isFalse();
        }

        @Test
        void privileged_noRoleCannotEnable() {
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of())).isFalse();
        }

        // --- Multi-role scenarios: highest delegation of the enabling user is used ---

        @Test
        void none_fumWithEumRole_cannotEnable() {
            // FUM alone cannot re-enable a NONE-disabled user, but EUM (higher delegation) can
            assertThat(policy.canEnable(DisableType.NONE, List.of(FUM, EUM))).isFalse();
        }

        @Test
        void none_fumWithSrRole_canEnable() {
            // FUM alone cannot re-enable a NONE-disabled user, but SR (higher delegation) can
            assertThat(policy.canEnable(DisableType.NONE, List.of(FUM, SR))).isTrue();
        }

        @Test
        void laa_fumWithEumRole_cannotEnable() {
            // FUM alone cannot re-enable a LAA-disabled user, but EUM (higher delegation) can
            assertThat(policy.canEnable(DisableType.LAA, List.of(FUM, EUM))).isFalse();
        }

        @Test
        void privileged_eumWithGaRole_canEnable() {
            // EUM alone cannot re-enable a PRIVILEGED-disabled user, but GA (higher delegation) can
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(EUM, GA))).isTrue();
        }

        @Test
        void privileged_fumWithEumRole_cannotEnable() {
            // Neither FUM nor EUM qualifies to re-enable a PRIVILEGED-disabled user
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(FUM, EUM))).isFalse();
        }
    }

    @Nested
    class RequiresSameFirmCheck {

        @Test
        void nonFirmType_returnsFalse() {
            assertThat(policy.requiresSameFirmCheck(null, List.of(FUM))).isFalse();
            assertThat(policy.requiresSameFirmCheck(DisableType.NONE, List.of(FUM))).isFalse();
            assertThat(policy.requiresSameFirmCheck(DisableType.LAA, List.of(FUM))).isFalse();
            assertThat(policy.requiresSameFirmCheck(DisableType.PRIVILEGED, List.of(FUM))).isFalse();
        }

        @Test
        void firm_fumOnly_requiresSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(DisableType.FIRM, List.of(FUM))).isTrue();
        }

        @Test
        void firm_gaBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(DisableType.FIRM, List.of(GA))).isFalse();
        }

        @Test
        void firm_srBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(DisableType.FIRM, List.of(SR))).isFalse();
        }

        @Test
        void firm_eumBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(DisableType.FIRM, List.of(EUM))).isFalse();
        }

        @Test
        void firm_euaBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(DisableType.FIRM, List.of(EUA))).isFalse();
        }

        @Test
        void firm_eusBypassesSameFirmCheck() {
            assertThat(policy.requiresSameFirmCheck(DisableType.FIRM, List.of(EUS))).isFalse();
        }

        @Test
        void firm_fumWithHigherDelegation_bypassesSameFirmCheck() {
            // FUM also holds EUA — higher delegation overrides the same-firm requirement
            assertThat(policy.requiresSameFirmCheck(DisableType.FIRM, List.of(FUM, EUA))).isFalse();
        }
    }

    @Nested
    class AccessControlServiceTest {

        private static Stream<Arguments> provideRolesForNullDisableTypeTrue() {
            return Stream.of(
                    Arguments.of(List.of(AuthzRole.INTERNAL_USER_MANAGER.getRoleName())),
                    Arguments.of(List.of(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())),
                    Arguments.of(List.of(AuthzRole.EXTERNAL_USER_SUPPORT.getRoleName())),
                    Arguments.of(List.of(AuthzRole.GLOBAL_ADMIN.getRoleName())),
                    Arguments.of(List.of(AuthzRole.SECURITY_RESPONSE.getRoleName())),
                    Arguments.of(List.of(AuthzRole.GLOBAL_ADMIN.getRoleName(), AuthzRole.SECURITY_RESPONSE.getRoleName()))
            );
        }

        @ParameterizedTest
        @MethodSource("provideRolesForNullDisableTypeTrue")
        @DisplayName("Should return true for null disableType when actor has qualifying roles")
        void shouldReturnTrueWhenNullDisableTypeAndHasQualifyingRole(List<String> actorRoles) {
            boolean result = policy.canDelegateReactivationRequest(null, actorRoles);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for null disableType when actor lacks qualifying roles")
        void shouldReturnFalseWhenNullDisableTypeAndLacksQualifyingRole() {
            List<String> roles = List.of(
                    AuthzRole.EXTERNAL_USER_MANAGER.getRoleName(),
                    AuthzRole.FIRM_USER_MANAGER.getRoleName(),
                    "UNKNOWN_ROLE"
            );

            boolean result = policy.canDelegateReactivationRequest(null, roles);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null disableType when actor roles list is empty")
        void shouldReturnFalseWhenNullDisableTypeAndEmptyRoles() {
            boolean result = policy.canDelegateReactivationRequest(null, Collections.emptyList());

            assertThat(result).isFalse();
        }


        @ParameterizedTest
        @EnumSource(value = DisableType.class, names = {"NONE", "SYNC", "LAA", "FIRM"})
        @DisplayName("Should return true when role is FIRM_USER_MANAGER")
        void shouldReturnTrueForFirmUserManager(DisableType disableType) {
            List<String> roles = List.of(AuthzRole.FIRM_USER_MANAGER.getRoleName());

            boolean result = policy.canDelegateReactivationRequest(disableType, roles);

            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = DisableType.class, names = {"NONE", "SYNC", "LAA", "FIRM"})
        @DisplayName("Should return true when role is EXTERNAL_USER_MANAGER")
        void shouldReturnTrueForExternalUserManager(DisableType disableType) {
            List<String> roles = List.of(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName());

            boolean result = policy.canDelegateReactivationRequest(disableType, roles);

            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = DisableType.class, names = {"NONE", "SYNC", "LAA", "FIRM"})
        @DisplayName("Should return true when role is EXTERNAL_USER_SUPPORT")
        void shouldReturnTrueForExternalUserSupport(DisableType disableType) {
            List<String> roles = List.of(AuthzRole.EXTERNAL_USER_SUPPORT.getRoleName());

            boolean result = policy.canDelegateReactivationRequest(disableType, roles);

            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = DisableType.class, names = {"NONE", "SYNC", "LAA", "FIRM"})
        @DisplayName("Should return false when role is neither FIRM_USER_MANAGER nor EXTERNAL_USER_MANAGER")
        void shouldReturnFalseForOtherRoles(DisableType disableType) {
            List<String> roles = List.of(
                    AuthzRole.GLOBAL_ADMIN.getRoleName(),
                    AuthzRole.SECURITY_RESPONSE.getRoleName(),
                    AuthzRole.EXTERNAL_USER_ADMIN.getRoleName(),
                    AuthzRole.INTERNAL_USER_MANAGER.getRoleName()
            );

            boolean result = policy.canDelegateReactivationRequest(disableType, roles);

            assertThat(result).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = DisableType.class, names = {"NONE", "SYNC", "LAA", "FIRM"})
        @DisplayName("Should return false when actor roles list is empty")
        void shouldReturnFalseForEmptyRoles(DisableType disableType) {
            boolean result = policy.canDelegateReactivationRequest(disableType, Collections.emptyList());

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should always return false for PRIVILEGED disableType regardless of actor roles")
        void shouldAlwaysReturnFalseForPrivileged() {
            List<String> allRoles = List.of(
                    AuthzRole.GLOBAL_ADMIN.getRoleName(),
                    AuthzRole.SECURITY_RESPONSE.getRoleName(),
                    AuthzRole.EXTERNAL_USER_ADMIN.getRoleName(),
                    AuthzRole.EXTERNAL_USER_MANAGER.getRoleName(),
                    AuthzRole.INTERNAL_USER_MANAGER.getRoleName(),
                    AuthzRole.FIRM_USER_MANAGER.getRoleName()
            );

            boolean result = policy.canDelegateReactivationRequest(DisableType.PRIVILEGED, allRoles);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for PRIVILEGED disableType when roles list is empty")
        void shouldReturnFalseForPrivilegedAndEmptyRoles() {
            boolean result = policy.canDelegateReactivationRequest(DisableType.PRIVILEGED, Collections.emptyList());

            assertThat(result).isFalse();
        }
    }

}
