package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.RoleType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppRoleRepository extends JpaRepository<AppRole, UUID> {

    @Query("""
        SELECT role FROM AppRole role
        LEFT JOIN FETCH role.permissions
        """)
    List<AppRole> findAllWithPermissions();

    Optional<AppRole> findByName(String roleName);

    List<AppRole> findByRoleTypeIn(Collection<RoleType> roleTypes);

    List<AppRole> findAllByIdInAndAuthzRoleIs(Collection<UUID> roleIds, boolean authzRole);
}
