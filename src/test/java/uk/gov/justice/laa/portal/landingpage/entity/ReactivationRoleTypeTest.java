package uk.gov.justice.laa.portal.landingpage.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReactivationRoleTypeTest {

    @Nested
    @DisplayName("Tests for getDisplayName()")
    class DisplayNameTests {

        @Test
        @DisplayName("Should return correct display names for each enum constant")
        void shouldReturnCorrectDisplayNames() {
            assertThat(ReactivationRoleType.NONE.getDisplayName()).isEqualTo("None");
            assertThat(ReactivationRoleType.LAA_OST.getDisplayName()).isEqualTo("Legal Aid Agency (Online Support)");
            assertThat(ReactivationRoleType.PROVIDER_ADMIN.getDisplayName()).isEqualTo("Provider Admin");
            assertThat(ReactivationRoleType.LAA_USER_REGISTRATION.getDisplayName()).isEqualTo("Legal Aid Agency (User Registration)");
            assertThat(ReactivationRoleType.LAA.getDisplayName()).isEqualTo("Legal Aid Agency");
        }
    }

    @Nested
    @DisplayName("Tests for getLabelForUi(String userName)")
    class GetLabelForUiTests {

        @Test
        @DisplayName("Should format label with user name when role is PROVIDER_ADMIN")
        void shouldFormatLabelWithUserNameForProviderAdmin() {
            String userName = "John Doe";
            String result = ReactivationRoleType.PROVIDER_ADMIN.getLabelForUi(userName);

            assertThat(result).isEqualTo("John Doe (Provider Admin)");
        }

        @ParameterizedTest
        @EnumSource(value = ReactivationRoleType.class, names = {"PROVIDER_ADMIN"}, mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("Should return display name only for non-PROVIDER_ADMIN roles")
        void shouldReturnDisplayNameOnlyForNonProviderAdminRoles(ReactivationRoleType roleType) {
            String userName = "Jane Smith";
            String result = roleType.getLabelForUi(userName);

            assertThat(result).isEqualTo(roleType.getDisplayName());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("Should handle null, empty, or blank user names gracefully for PROVIDER_ADMIN")
        void shouldHandleNullOrBlankUserNameForProviderAdmin(String userName) {
            String result = ReactivationRoleType.PROVIDER_ADMIN.getLabelForUi(userName);

            assertThat(result).isEqualTo(userName + " (Provider Admin)");
        }
    }

    @Nested
    @DisplayName("Enum Standard Operations")
    class EnumStandardTests {

        @Test
        @DisplayName("Should contain expected number of enum values")
        void shouldHaveCorrectNumberOfValues() {
            assertThat(ReactivationRoleType.values()).hasSize(5);
        }

        @Test
        @DisplayName("Should resolve correct enum instance from valueOf")
        void shouldResolveValueOf() {
            assertThat(ReactivationRoleType.valueOf("PROVIDER_ADMIN")).isEqualTo(ReactivationRoleType.PROVIDER_ADMIN);
            assertThat(ReactivationRoleType.valueOf("LAA_OST")).isEqualTo(ReactivationRoleType.LAA_OST);
        }
    }
}
