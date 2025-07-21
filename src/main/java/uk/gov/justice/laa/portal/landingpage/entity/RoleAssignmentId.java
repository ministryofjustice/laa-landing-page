package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoleAssignmentId implements Serializable {

    private UUID assigningRole;

    private UUID assignableRole;
}
