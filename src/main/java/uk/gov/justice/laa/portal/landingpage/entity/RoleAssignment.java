package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "role_assignment")
@IdClass(RoleAssignmentId.class)
@Check(name = "role_assignment_no_self_assignable", constraints = "assigning_role_id <> assignable_role_id")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@AllArgsConstructor
@ToString(doNotUseGetters = true)
public class RoleAssignment {

    @Id
    @ManyToOne
    @JoinColumn(name = "assigning_role_id")
    @JsonBackReference("assigning-role")
    private AppRole assigningRole;

    @Id
    @ManyToOne
    @JoinColumn(name = "assignable_role_id")
    @JsonBackReference("assignable-role")
    private AppRole assignableRole;
}
