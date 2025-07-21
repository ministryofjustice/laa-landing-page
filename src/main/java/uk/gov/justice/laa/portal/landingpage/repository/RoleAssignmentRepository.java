package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignmentId;

public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, RoleAssignmentId> {
}
