package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;

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

    /**
     * Query for audit table - fetches all users with their profiles and roles
     * Supports filtering by name, email, firm, and SiLAS role
     */
    @Query(value = """
            SELECT DISTINCT u FROM EntraUser u
            WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                   LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND (:firmId IS NULL OR EXISTS (
                SELECT 1 FROM UserProfile up2 WHERE up2.entraUser.id = u.id AND up2.firm.id = :firmId
            ))
            AND (:silasRole IS NULL OR :silasRole = '' OR EXISTS (
                SELECT 1 FROM UserProfile up3
                JOIN up3.appRoles ar3
                WHERE up3.entraUser.id = u.id AND ar3.authzRole = true AND ar3.name = :silasRole
            ))
            """, countQuery = """
            SELECT COUNT(DISTINCT u.id) FROM EntraUser u
            WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                   LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND (:firmId IS NULL OR EXISTS (
                SELECT 1 FROM UserProfile up2 WHERE up2.entraUser.id = u.id AND up2.firm.id = :firmId
            ))
            AND (:silasRole IS NULL OR :silasRole = '' OR EXISTS (
                SELECT 1 FROM UserProfile up3
                JOIN up3.appRoles ar3
                WHERE up3.entraUser.id = u.id AND ar3.authzRole = true AND ar3.name = :silasRole
            ))
            """)
    Page<EntraUser> findAllUsersForAudit(
            @Param("searchTerm") String searchTerm,
            @Param("firmId") UUID firmId,
            @Param("silasRole") String silasRole,
            Pageable pageable);

    /**
     * Batch fetch user profiles with firms and roles for given users
     */
    @Query("""
            SELECT DISTINCT u FROM EntraUser u
            LEFT JOIN FETCH u.userProfiles up
            LEFT JOIN FETCH up.firm
            LEFT JOIN FETCH up.appRoles
            WHERE u.id IN :userIds
            """)
    List<EntraUser> findUsersWithProfilesAndRoles(@Param("userIds") Set<UUID> userIds);
}
