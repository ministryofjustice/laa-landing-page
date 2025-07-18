package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "role_assignment")
@IdClass(RoleAssignmentId.class)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class RoleAssignment {

    @Id
    @ManyToOne
    @JoinColumn(name = "assigning_role_id", referencedColumnName = "id")
    private AppRole assigningRole;

    @Id
    @ManyToOne
    @JoinColumn(name = "assignable_role_id", referencedColumnName = "id")
    private AppRole assignableRole;
}
