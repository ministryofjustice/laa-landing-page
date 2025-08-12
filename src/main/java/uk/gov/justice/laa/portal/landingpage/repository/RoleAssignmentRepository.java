package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignmentId;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, RoleAssignmentId> {

    List<RoleAssignment> findByAssigningRole_IdIn(Collection<UUID> ids);
}
