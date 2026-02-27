package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignment;
import uk.gov.justice.laa.portal.landingpage.entity.RoleAssignmentId;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, RoleAssignmentId> {

    List<RoleAssignment> findByAssigningRole_IdIn(Collection<UUID> ids);

    List<RoleAssignment> findByAssignableRole_Id(UUID id);

    @Modifying
    @Transactional
    @Query("""
                DELETE FROM RoleAssignment ra
                WHERE ra.assigningRole.id = :roleId
                   OR ra.assignableRole.id = :roleId
            """)
    void deleteByRoleIdInEitherColumn(@Param("roleId") UUID roleId);

}
