package uk.gov.justice.laa.portal.landingpage.service;

import org.springframework.stereotype.Component;
import uk.gov.justice.laa.portal.landingpage.entity.AuthzRole;
import uk.gov.justice.laa.portal.landingpage.entity.DisableType;

import java.util.List;

/**
 * Encapsulates the re-enable permission matrix.
 *
 * <p>Each combination of {@link DisableType} (the level at which a user was disabled) and
 * the enabling actor's roles is evaluated against the following hierarchy:
 *
 * <table border="1">
 *   <tr><th>disable_type</th><th>FUM</th><th>EUM / EUA</th><th>SR / GA</th></tr>
 *   <tr><td>NULL (unknown/legacy)</td><td>✅</td><td>✅</td><td>✅</td></tr>
 *   <tr><td>NONE (sync)</td><td>❌</td><td>✅</td><td>✅</td></tr>
 *   <tr><td>FIRM</td><td>✅ (same-firm check required separately)</td><td>✅</td><td>✅</td></tr>
 *   <tr><td>LAA</td><td>❌</td><td>✅</td><td>✅</td></tr>
 *   <tr><td>PRIVILEGED</td><td>❌</td><td>❌</td><td>✅</td></tr>
 * </table>
 *
 * <p>When the actor is a FUM and the disable type is {@link DisableType#FIRM}, this class
 * returns {@code true} — but the caller is responsible for the additional same-firm check via
 * {@link #requiresSameFirmCheck(DisableType, List)}.
 *
 * <p>This class is intentionally stateless and has no external dependencies, making it
 * straightforward to modify the matrix or add new disable types without touching other services.
 */
@Component
public class UserEnablementPolicy {

    /**
     * Returns {@code true} if an actor with the given roles is permitted to re-enable a user
     * that was disabled with the given {@code disableType}.
     *
     * <p>Note: when this returns {@code true} and {@link #requiresSameFirmCheck} also returns
     * {@code true}, the caller must additionally verify that the actor and the disabled user
     * belong to the same firm.
     *
     * @param disableType the stored disable type of the target user ({@code null} = legacy/unknown)
     * @param actorRoles  the role names held by the user attempting to enable
     * @return {@code true} if enabling is permitted at the role level
     */
    public boolean canEnable(DisableType disableType, List<String> actorRoles) {
        // Legacy / not-logged case: all roles permitted
        if (disableType == null) {
            return true;
        }

        boolean isGlobalAdminOrSecurityResponse = actorRoles.contains(AuthzRole.GLOBAL_ADMIN.getRoleName())
                || actorRoles.contains(AuthzRole.SECURITY_RESPONSE.getRoleName());

        boolean isEuaLevel = actorRoles.contains(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName())
                || actorRoles.contains(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName());

        boolean isFirmUserManager = actorRoles.contains(AuthzRole.FIRM_USER_MANAGER.getRoleName());

        return switch (disableType) {
            case NONE ->
                    // Sync-disabled: EUM/EUA or higher only
                    isEuaLevel || isGlobalAdminOrSecurityResponse;

            case FIRM ->
                    // FUM-disabled: any FUM (same-firm check handled separately), EUM/EUA, or higher
                    isFirmUserManager || isEuaLevel || isGlobalAdminOrSecurityResponse;

            case LAA ->
                    // EUM/EUA-disabled: EUM/EUA or higher only
                    isEuaLevel || isGlobalAdminOrSecurityResponse;

            case PRIVILEGED ->
                    // SR/GA-disabled: only SR or GA
                    isGlobalAdminOrSecurityResponse;
        };
    }

    /**
     * Returns {@code true} when enabling is conditionally permitted but a same-firm check must
     * also pass before the enable is allowed.
     *
     * <p>This is true when the disable type is {@link DisableType#FIRM} and the actor is a
     * Firm User Manager (without a higher-delegation role that would bypass the firm restriction).
     *
     * @param disableType the stored disable type of the target user
     * @param actorRoles  the role names held by the user attempting to enable
     * @return {@code true} if a same-firm check is required
     */
    public boolean requiresSameFirmCheck(DisableType disableType, List<String> actorRoles) {
        if (disableType != DisableType.FIRM) {
            return false;
        }

        boolean hasHigherDelegation = actorRoles.contains(AuthzRole.GLOBAL_ADMIN.getRoleName())
                || actorRoles.contains(AuthzRole.SECURITY_RESPONSE.getRoleName())
                || actorRoles.contains(AuthzRole.EXTERNAL_USER_MANAGER.getRoleName())
                || actorRoles.contains(AuthzRole.EXTERNAL_USER_ADMIN.getRoleName());

        return !hasHigherDelegation && actorRoles.contains(AuthzRole.FIRM_USER_MANAGER.getRoleName());
    }
}
