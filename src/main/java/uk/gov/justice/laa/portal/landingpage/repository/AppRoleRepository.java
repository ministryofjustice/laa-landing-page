package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.AppType;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRole, UUID> {

    @Query("""
            SELECT role FROM AppRole role
            LEFT JOIN FETCH role.permissions
            """)
    List<AppRole> findAllWithPermissions();

    Optional<AppRole> findByName(String roleName);

    @Query(value = "SELECT * FROM app_role ar WHERE :userType = ANY(ar.user_type_restriction)", nativeQuery = true)
    List<AppRole> findByUserTypeRestrictionContains(@Param("userType") String userType);

    List<AppRole> findAllByIdInAndAuthzRoleIs(Collection<UUID> roleIds, boolean authzRole);

    List<AppRole> findAllByIdIn(Collection<UUID> roleIds);

    /**
     * Find all SiLAS roles (authz roles) ordered by name
     */
    @Query("""
            SELECT role FROM AppRole role
            WHERE role.authzRole = true
            ORDER BY role.name
            """)
    List<AppRole> findAllAuthzRoles();

    @Query(value = "SELECT name FROM app_role WHERE 'EXTERNAL' = ANY(user_type_restriction) ORDER BY name",
            nativeQuery = true)
    List<String> getExternalRoleNames();

    List<AppRole> findByApp_AppType(AppType appType);

    List<AppRole> findByApp_NameAndApp_AppType(String appName, AppType appType);

    @Query(value = "DELETE from role_permission WHERE app_role_id = :roleId",
            nativeQuery = true)
    @Modifying
    void deleteRolePermissions(UUID roleId);

}
