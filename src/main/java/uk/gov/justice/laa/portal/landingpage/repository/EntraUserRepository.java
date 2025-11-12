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

    Optional<EntraUser> findByEmailIgnoreCase(String email);

    @Query("""
            SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END
            FROM EntraUser u
            WHERE LOWER(SUBSTRING(u.email, LOCATE('@', u.email) + 1)) = LOWER(:domain)
            """)
    boolean existsByEmailDomain(@Param("domain") String domain);
}
