package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
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


    @Query(value = """
            SELECT 
                ar.id AS assignable_id,
                ar.name AS assignable_name,
                ar.description AS assignable_description,
                COALESCE(ar2.id::text, 'ALL') AS assigning_id,
                COALESCE(ar2.name, 'ALL') AS assigning_name
            FROM app a
            JOIN app_role ar
                ON a.id = ar.app_id                -- FIXED FK
            LEFT JOIN role_assignment ra 
                ON ra.assignable_role_id = ar.id
            LEFT JOIN app_role ar2
                ON ra.assigning_role_id = ar2.id
            WHERE a.app_type = 'LAA'
              AND ar.authz_role = false
            ORDER BY ar.ordinal, ar.name, assigning_name
            """, nativeQuery = true)
    List<Object[]> findAssignableRolesWithAssigningRoles();

    @Query(value = """
            SELECT 
                ar.id AS assignable_id,
                ar.name AS assignable_name,
                ar.description AS assignable_description,
                COALESCE(ar2.id::text, 'ALL') AS assigning_id,
                COALESCE(ar2.name, 'ALL') AS assigning_name
            FROM app a
            JOIN app_role ar
                ON a.id = ar.app_id                -- FIXED FK
            LEFT JOIN role_assignment ra 
                ON ra.assignable_role_id = ar.id
            LEFT JOIN app_role ar2
                ON ra.assigning_role_id = ar2.id
            WHERE a.app_type = 'LAA'
              AND ar.authz_role = false
              AND a.name = :appName
            ORDER BY ar.ordinal, ar.name, assigning_name
            """, nativeQuery = true)
    List<Object[]> findAssignableRolesWithAssigningRolesByAppName(String appName);

    void deleteByAssignableRole(AppRole targetRole);
}
