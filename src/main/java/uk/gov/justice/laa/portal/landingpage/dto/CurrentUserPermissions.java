package uk.gov.justice.laa.portal.landingpage.dto;

import uk.gov.justice.laa.portal.landingpage.entity.Permission;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/** Session-scoped UI hints only; never use these to authorize an operation. */
public final class CurrentUserPermissions implements Serializable {

    private static final long serialVersionUID = 1L;
    private final Set<String> names;

    public CurrentUserPermissions(Collection<String> names) {
        this.names = Set.copyOf(names);
    }

    public boolean allows(Permission permission) {
        return permission != null && names.contains(permission.name());
    }
}
