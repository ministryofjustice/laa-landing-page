package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.DisableType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UserEnablementPolicyTest {

    private final UserEnablementPolicy policy = new UserEnablementPolicy();

    private static final String GA = AuthzRole.GLOBAL_ADMIN.getRoleName();
    private static final String SR = AuthzRole.SECURITY_RESPONSE.getRoleName();
    private static final String EUM = AuthzRole.EXTERNAL_USER_MANAGER.getRoleName();
    private static final String EUA = AuthzRole.EXTERNAL_USER_ADMIN.getRoleName();
    private static final String FUM = AuthzRole.FIRM_USER_MANAGER.getRoleName();

    @Nested
    class CanEnable {

        @Test
        void nullDisableType_anyRole_returnsTrue() {
            assertThat(policy.canEnable(null, List.of(FUM))).isTrue();
            assertThat(policy.canEnable(null, List.of(EUA))).isTrue();
            assertThat(policy.canEnable(null, List.of(GA))).isTrue();
            assertThat(policy.canEnable(null, List.of())).isTrue();
        }

        @Test
        void none_gaCanEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(GA))).isTrue();
        }

        @Test
        void none_srCanEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(SR))).isTrue();
        }

        @Test
        void none_eumCanEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(EUM))).isTrue();
        }

        @Test
        void none_euaCanEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(EUA))).isTrue();
        }

        @Test
        void none_fumCannotEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of(FUM))).isFalse();
        }

        @Test
        void none_noRoleCannotEnable() {
            assertThat(policy.canEnable(DisableType.NONE, List.of())).isFalse();
        }

        @Test
        void firm_fumCanEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(FUM))).isTrue();
        }

        @Test
        void firm_euaCanEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(EUA))).isTrue();
        }

        @Test
        void firm_gaCanEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of(GA))).isTrue();
        }

        @Test
        void firm_noRoleCannotEnable() {
            assertThat(policy.canEnable(DisableType.FIRM, List.of())).isFalse();
        }

        @Test
        void laa_eumCanEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of(EUM))).isTrue();
        }

        @Test
        void laa_euaCanEnable() {
            assertThat(policy.canEnable(DisableType.LAA, List.of(EUA))).isTrue();
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
        void privileged_fumCannotEnable() {
            assertThat(policy.canEnable(DisableType.PRIVILEGED, List.of(FUM))).isFalse();
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
        void firm_fumWithHigherDelegation_bypassesSameFirmCheck() {
            // FUM also holds EUA — higher delegation overrides the same-firm requirement
            assertThat(policy.requiresSameFirmCheck(DisableType.FIRM, List.of(FUM, EUA))).isFalse();
        }
    }
}
