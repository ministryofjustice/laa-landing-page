package uk.gov.justice.laa.portal.landingpage.client;

import java.util.List;
import java.util.UUID;

public final class RoleAssignmentContract {

    private RoleAssignmentContract() {
    }

    public record Role(UUID appRoleId, String appRoleName) { }

    public record Options(List<Role> items) { }

    public record Command(UUID userProfileId, List<UUID> appRoleIds) { }

    public record Result(UUID userProfileId, List<Role> assignedRoles) { }
}
