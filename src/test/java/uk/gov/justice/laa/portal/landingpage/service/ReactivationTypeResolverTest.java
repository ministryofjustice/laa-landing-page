package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.ReactivationRoleType;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactivationTypeResolverTest {

    private ReactivationTypeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ReactivationTypeResolver();
    }

    @Nested
    @DisplayName("Tests for resolveFromRoles(List<String>)")
    class ResolveFromRolesTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should return NONE when role list is null or empty")
        void shouldReturnNoneWhenRolesNullOrEmpty(List<String> roleNames) {
            ReactivationRoleType result = resolver.resolveFromRoles(roleNames);

            assertThat(result).isEqualTo(ReactivationRoleType.NONE);
        }

        @Test
        @DisplayName("Should resolve to LAA when GLOBAL_ADMIN role is present")
        void shouldResolveToLaaForGlobalAdmin() {
            List<String> roles = List.of(AuthzRole.GLOBAL_ADMIN.getRoleName());

            ReactivationRoleType result = resolver.resolveFromRoles(roles);

            assertThat(result).isEqualTo(ReactivationRoleType.LAA);
        }

        @Test
        @DisplayName("Should resolve to LAA when SECURITY_RESPONSE role is present")
        void shouldResolveToLaaForSecurityResponse() {
            List<String> roles = List.of(AuthzRole.SECURITY_RESPONSE.getRoleName());

            ReactivationRoleType result = resolver.resolveFromRoles(roles);

            assertThat(result).isEqualTo(ReactivationRoleType.LAA);
        }

        @Test
        @DisplayName("Should resolve to LAA_USER_REGISTRATION for EXTERNAL_USER_ADMIN role")
        void shouldResolveToLaaUserRegistrationForExternalUserAdmin() {
            List<String> roles = List.of(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName());

            ReactivationRoleType result = resolver.resolveFromRoles(roles);

            assertThat(result).isEqualTo(ReactivationRoleType.LAA_USER_REGISTRATION);
        }

        @Test
        @DisplayName("Should resolve to LAA_OST for EXTERNAL_USER_MANAGER role")
        void shouldResolveToLaaOstForExternalUserManager() {
            List<String> roles = List.of(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName());

            ReactivationRoleType result = resolver.resolveFromRoles(roles);

            assertThat(result).isEqualTo(ReactivationRoleType.LAA_OST);
        }

        @Test
        @DisplayName("Should resolve to PROVIDER_ADMIN for FIRM_USER_MANAGER role")
        void shouldResolveToProviderAdminForFirmUserManager() {
            List<String> roles = List.of(AuthzRole.FIRM_USER_MANAGER.getRoleName());

            ReactivationRoleType result = resolver.resolveFromRoles(roles);

            assertThat(result).isEqualTo(ReactivationRoleType.PROVIDER_ADMIN);
        }

        @Test
        @DisplayName("Should return NONE when role names do not match any known AuthzRole")
        void shouldReturnNoneForUnrecognizedRoles() {
            List<String> roles = List.of("UNKNOWN_ROLE", "ROLE_USER");

            ReactivationRoleType result = resolver.resolveFromRoles(roles);

            assertThat(result).isEqualTo(ReactivationRoleType.NONE);
        }

        @Test
        @DisplayName("Should prioritize higher delegation roles when multiple roles are present")
        void shouldPrioritizeHighestDelegationRole() {
            // GLOBAL_ADMIN > EXTERNAL_USER_ADMIN > FIRM_USER_MANAGER
            List<String> roles = List.of(
                    AuthzRole.FIRM_USER_MANAGER.getRoleName(),
                    AuthzRole.EXTERNAL_USER_ADMIN.getRoleName(),
                    AuthzRole.GLOBAL_ADMIN.getRoleName()
            );

            ReactivationRoleType result = resolver.resolveFromRoles(roles);

            assertThat(result).isEqualTo(ReactivationRoleType.LAA);
        }
    }

    @Nested
    @DisplayName("Tests for resolve(EntraUser)")
    class ResolveEntraUserTests {

        @Test
        @DisplayName("Should return NONE when actor is null")
        void shouldReturnNoneWhenActorIsNull() {
            ReactivationRoleType result = resolver.resolve(null);

            assertThat(result).isEqualTo(ReactivationRoleType.NONE);
        }

        @Test
        @DisplayName("Should return NONE when actor userProfiles is null")
        void shouldReturnNoneWhenUserProfilesIsNull() {
            EntraUser actor = mock(EntraUser.class);
            when(actor.getUserProfiles()).thenAnswer(invocation -> null);

            ReactivationRoleType result = resolver.resolve(actor);

            assertThat(result).isEqualTo(ReactivationRoleType.NONE);
        }

        @Test
        @DisplayName("Should return NONE when user has no active profiles")
        void shouldReturnNoneWhenNoActiveProfiles() {
            EntraUser actor = mock(EntraUser.class);
            UserProfile inactiveProfile = mock(UserProfile.class);
            when(inactiveProfile.isActiveProfile()).thenReturn(false);

            when(actor.getUserProfiles()).thenReturn(Set.of(inactiveProfile));

            ReactivationRoleType result = resolver.resolve(actor);

            assertThat(result).isEqualTo(ReactivationRoleType.NONE);
        }

        @Test
        @DisplayName("Should resolve correctly from active profile app roles")
        void shouldResolveFromActiveProfile() {
            EntraUser actor = mock(EntraUser.class);
            UserProfile activeProfile = mock(UserProfile.class);
            AppRole appRole = mock(AppRole.class);

            when(activeProfile.isActiveProfile()).thenReturn(true);
            when(appRole.getName()).thenReturn(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName());
            when(activeProfile.getAppRoles()).thenReturn(Set.of(appRole));

            when(actor.getUserProfiles()).thenReturn(Set.of(activeProfile));

            ReactivationRoleType result = resolver.resolve(actor);

            assertThat(result).isEqualTo(ReactivationRoleType.LAA_USER_REGISTRATION);
        }

        @Test
        @DisplayName("Should handle null appRoles on active profile gracefully")
        void shouldHandleNullAppRolesOnActiveProfile() {
            EntraUser actor = mock(EntraUser.class);
            UserProfile activeProfile = mock(UserProfile.class);

            when(activeProfile.isActiveProfile()).thenReturn(true);
            when(activeProfile.getAppRoles()).thenAnswer(invocation -> null);

            when(actor.getUserProfiles()).thenReturn(Set.of(activeProfile));

            ReactivationRoleType result = resolver.resolve(actor);

            assertThat(result).isEqualTo(ReactivationRoleType.NONE);
        }

        @Test
        @DisplayName("Should pick roles from the first active profile when multiple profiles exist")
        void shouldPickFirstActiveProfile() {
            EntraUser actor = mock(EntraUser.class);

            UserProfile firstActive = mock(UserProfile.class);
            AppRole firstRole = mock(AppRole.class);
            when(firstActive.isActiveProfile()).thenReturn(true);
            when(firstRole.getName()).thenReturn(AuthzRole.FIRM_USER_MANAGER.getRoleName());
            when(firstActive.getAppRoles()).thenReturn(Set.of(firstRole));

            UserProfile secondActive = mock(UserProfile.class);

            when(actor.getUserProfiles()).thenReturn(Set.of(firstActive, secondActive));

            ReactivationRoleType result = resolver.resolve(actor);

            assertThat(result).isEqualTo(ReactivationRoleType.PROVIDER_ADMIN);
        }
    }
}
