package uk.gov.justice.laa.portal.landingpage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves the {@link RoleType} that should be recorded when an external user is disabled.
 *
 * <p>The type is determined from the highest-delegation role held by the disabling user at
 * the moment of the disable action. The hierarchy (highest to lowest delegation) is:
 * <ol>
 *   <li>Global Admin or Security Response → {@link RoleType#PRIVILEGED}</li>
 *   <li>External User Manager or External User Admin → {@link RoleType#LAA}</li>
 *   <li>Firm User Manager → {@link RoleType#FIRM}</li>
 *   <li>Any other actor (automated sync, unknown) → {@link RoleType#NONE}</li>
 * </ol>
 *
 * <p>A {@code NULL} value is reserved for legacy/unknown cases where the disabling actor
 * cannot be determined (e.g. pre-existing disabled accounts).  This class will never return
 * {@code null}; callers must explicitly pass {@code null} when no actor is known.
 */
@Slf4j
@Component
public class RoleTypeResolver {

    /**
     * Resolves the actor role type from the actor's current active user profile roles.
     *
     * @param actor the EntraUser performing the disable action
     * @return the appropriate {@link RoleType} (never {@code null})
     */
    public RoleType resolve(EntraUser actor) {
        if (actor == null) {
            log.warn("RoleTypeResolver.resolve called with null actor — returning NONE");
            return RoleType.NONE;
        }

        if (actor.getUserProfiles() == null) {
            log.warn("RoleTypeResolver.resolve called with null userProfiles — returning NONE");
            return RoleType.NONE;
        }

        List<String> roleNames = actor.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .findFirst()
                .map(profile -> Optional.ofNullable(profile.getAppRoles()).orElse(Set.of()).stream()
                        .map(AppRole::getName)
                        .toList())
                .orElse(List.of());

        return resolveFromRoles(roleNames);
    }

    /**
     * Pure, stateless form of the resolver — suitable for direct testing.
     *
     * @param roleNames the role names the disabling user currently holds
     * @return the appropriate {@link RoleType} (never {@code null})
     */
    public RoleType resolveFromRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return RoleType.NONE;
        }

        // Check from highest to lowest delegation
        if (roleNames.contains(AuthzRole.GLOBAL_ADMIN.getRoleName())
                || roleNames.contains(AuthzRole.SECURITY_RESPONSE.getRoleName())) {
            return RoleType.PRIVILEGED;
        }

        if (roleNames.contains(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName())
                || roleNames.contains(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName())) {
            return RoleType.LAA;
        }

        if (roleNames.contains(AuthzRole.FIRM_USER_MANAGER.getRoleName())) {
            return RoleType.FIRM;
        }

        return RoleType.NONE;
    }
}
