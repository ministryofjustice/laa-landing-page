package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

/**
 * Represents the delegation level of the user who disabled an external user account.
 * Stored on {@code entra_user.disable_type} at the time of disable so that enable
 * authorisation can be enforced without depending on the disabling user's current roles.
 *
 * <p>The hierarchy (lowest to highest delegation):
 * <ol>
 *   <li>{@link #NONE}      – acted by a manual sync process (or unattributed); all roles permitted to re-enable</li>
 *   <li>{@link #SYNC}      – acted by an automated sync process; only EUM/EUA or higher can re-enable</li>
 *   <li>{@link #FIRM}      – acted by a Firm User Manager; any FUM (same firm), EUM/EUA or higher can re-enable</li>
 *   <li>{@link #LAA}       – acted by an External User Manager or External User Admin; only EUM/EUA or higher</li>
 *   <li>{@link #PRIVILEGED} – acted by Security Response or Global Admin; only GA or SR can re-enable</li>
 * </ol>
 *
 * <p>A {@code NULL} value in the database means the acted was not attributed to a known
 * actor (legacy data before this field existed). In that case only internal/LAA delegation
 * roles may re-enable the user.
 */
@Getter
public enum RoleType {

    /**
     * Disabled by an automated external user sync process.
     * Only External User Manager / Admin or higher can re-enable.
     */
    SYNC("Sync"),

    /**
     * Disabled by a manual user sync process, or an unattributed disable before full tracking was introduced.
     * All roles are permitted to re-enable.
     */
    NONE("None"),

    /**
     * Disabled by a Firm User Manager.
     * A FUM from the same firm, or any EUM/EUA or higher, can re-enable.
     */
    FIRM("Firm"),

    /**
     * Disabled by an External User Manager or External User Admin.
     * Only External User Manager / Admin or higher can re-enable.
     */
    LAA("LAA"),

    /**
     * Disabled by Security Response or Global Admin.
     * Only Security Response or Global Admin can re-enable.
     */
    PRIVILEGED("Privileged");

    private final String displayName;

    RoleType(String displayName) {
        this.displayName = displayName;
    }

}
