package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Permission;

@Repository
public interface EntraUserRepository extends JpaRepository<EntraUser, UUID> {
    @Query("""
            SELECT u from EntraUser u
            LEFT JOIN FETCH u.userProfiles userProfile
            LEFT JOIN FETCH userProfile.appRoles appRole
            LEFT JOIN FETCH appRole.permissions permission
            where u.entraOid = ?1
            """)
    Optional<EntraUser> findByEntraOid(String entraOid);

    @Query("""
            SELECT DISTINCT user FROM EntraUser user
            LEFT JOIN FETCH user.userProfiles userProfile
            LEFT JOIN FETCH userProfile.firm firm
            LEFT JOIN FETCH userProfile.appRoles appRole
            LEFT JOIN FETCH appRole.permissions permission
            WHERE (:search = ''
            OR (LOWER(user.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(user.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(user.email) LIKE LOWER(CONCAT('%', :search, '%'))))
            AND (:permissions IS NULL OR (permission IN :permissions))
            AND (:firmId IS NULL OR userProfile.firm.id = :firmId)
            """)
    Page<EntraUser> findByNameOrEmailAndPermissionsAndFirm(@Param("search") String search,
            @Param("permissions") List<Permission> permissions, @Param("firmId") UUID firmId, Pageable pageable);

    @Query("SELECT u from EntraUser u where u.email = ?1")
    Optional<EntraUser> findByEmailIgnoreCase(String email);
}
