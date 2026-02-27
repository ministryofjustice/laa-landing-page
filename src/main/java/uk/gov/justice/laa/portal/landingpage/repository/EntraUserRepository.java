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
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.repository.projection.UserAuditAccountStatusProjection;
import uk.gov.justice.laa.portal.landingpage.repository.projection.UserAuditFirmProjection;
import uk.gov.justice.laa.portal.landingpage.repository.projection.UserAuditProfileCountProjection;

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

    /**
     * Find user by ID with eagerly loaded associations to avoid LazyInitializationException.
     * Useful in contexts where the Hibernate session is not available (e.g., security checks, tests).
     */
    @Query("""
            SELECT u FROM EntraUser u
            LEFT JOIN FETCH u.userProfiles up
            LEFT JOIN FETCH up.appRoles ar
            LEFT JOIN FETCH ar.permissions
            LEFT JOIN FETCH up.firm
            WHERE u.id = :id
            """)
    Optional<EntraUser> findByIdWithAssociations(@Param("id") UUID id);

    Optional<EntraUser> findByEmailIgnoreCase(String email);

    /**
     * Query for audit table - fetches all users with their profiles and roles
     * Supports filtering by name, email, firm, and SiLAS role
     */
    @Query(value = """
            SELECT DISTINCT u FROM EntraUser u
            LEFT JOIN u.userProfiles up
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
            AND (:appId IS NULL OR EXISTS (
                SELECT 1 FROM UserProfile up4
                JOIN up4.appRoles ar4
                JOIN ar4.app a4
                WHERE up4.entraUser.id = u.id AND a4.id = :appId
            ))
            AND (:userType IS NULL OR EXISTS (
                SELECT 1 FROM UserProfile up5
                WHERE up5.entraUser.id = u.id AND up5.userType = :userType
            ))
            AND (:multiFirm IS NULL OR u.multiFirmUser = :multiFirm)
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
            AND (:appId IS NULL OR EXISTS (
                SELECT 1 FROM UserProfile up4
                JOIN up4.appRoles ar4
                JOIN ar4.app a4
                WHERE up4.entraUser.id = u.id AND a4.id = :appId
            ))
            AND (:userType IS NULL OR EXISTS (
                SELECT 1 FROM UserProfile up5
                WHERE up5.entraUser.id = u.id AND up5.userType = :userType
            ))
            AND (:multiFirm IS NULL OR u.multiFirmUser = :multiFirm)
            """)
    Page<EntraUser> findAllUsersForAudit(
            @Param("searchTerm") String searchTerm,
            @Param("firmId") UUID firmId,
            @Param("silasRole") String silasRole,
            @Param("appId") UUID appId,
            @Param("userType") UserType userType,
            @Param("multiFirm") Boolean multiFirm,
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

    /**
     * Query for audit table with profile count - returns count alongside user ID
     */
    @Query(value = """
            SELECT u.id as userId, COUNT(DISTINCT up.id) as profileCount
            FROM entra_user u
            LEFT JOIN user_profile up ON up.entra_user_id = u.id
            LEFT JOIN (
                SELECT DISTINCT up_role.entra_user_id
                FROM user_profile up_role
                JOIN user_profile_app_role upar ON upar.user_profile_id = up_role.id
                JOIN app_role ar ON ar.id = upar.app_role_id
                WHERE ar.authz_role = true
                AND (:silasRole IS NULL OR :silasRole = '' OR ar.name = :silasRole)
            ) role_filter ON role_filter.entra_user_id = u.id
            LEFT JOIN (
                SELECT DISTINCT up_app.entra_user_id
                FROM user_profile up_app
                JOIN user_profile_app_role upar_app ON upar_app.user_profile_id = up_app.id
                JOIN app_role ar_app ON ar_app.id = upar_app.app_role_id
                WHERE :appId IS NULL OR ar_app.app_id = CAST(:appId AS uuid)
            ) app_filter ON app_filter.entra_user_id = u.id
            WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                   LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND (:firmId IS NULL OR up.firm_id = CAST(:firmId AS uuid))
            AND (:silasRole IS NULL OR :silasRole = '' OR role_filter.entra_user_id IS NOT NULL)
            AND (:appId IS NULL OR app_filter.entra_user_id IS NOT NULL)
            AND (:userType IS NULL OR up.user_type = :userType)
            AND (:multiFirm IS NULL or u.multi_firm_user = :multiFirm)
            GROUP BY u.id
            """, nativeQuery = true)
    Page<UserAuditProfileCountProjection> findAllUsersForAuditWithProfileCount(
            @Param("searchTerm") String searchTerm,
            @Param("firmId") UUID firmId,
            @Param("silasRole") String silasRole,
            @Param("appId") UUID appId,
            @Param("userType") String userType,
            @Param("multiFirm") Boolean multiFirm,
            Pageable pageable);

    /**
     * Query for audit table with firm name - returns firm name alongside user ID
     */
    @Query(value = """
            SELECT u.id as userId, MIN(f.name) as firmName
            FROM entra_user u
            LEFT JOIN user_profile up ON up.entra_user_id = u.id
            LEFT JOIN firm f ON f.id = up.firm_id
            LEFT JOIN (
                SELECT DISTINCT up_role.entra_user_id
                FROM user_profile up_role
                JOIN user_profile_app_role upar ON upar.user_profile_id = up_role.id
                JOIN app_role ar ON ar.id = upar.app_role_id
                WHERE ar.authz_role = true
                AND (:silasRole IS NULL OR :silasRole = '' OR ar.name = :silasRole)
            ) role_filter ON role_filter.entra_user_id = u.id
            LEFT JOIN (
                SELECT DISTINCT up_app.entra_user_id
                FROM user_profile up_app
                JOIN user_profile_app_role upar_app ON upar_app.user_profile_id = up_app.id
                JOIN app_role ar_app ON ar_app.id = upar_app.app_role_id
                WHERE :appId IS NULL OR ar_app.app_id = CAST(:appId AS uuid)
            ) app_filter ON app_filter.entra_user_id = u.id
            WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                   LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND (:firmId IS NULL OR up.firm_id = CAST(:firmId AS uuid))
            AND (:silasRole IS NULL OR :silasRole = '' OR role_filter.entra_user_id IS NOT NULL)
            AND (:appId IS NULL OR app_filter.entra_user_id IS NOT NULL)
            AND (:userType IS NULL OR up.user_type = :userType)
            AND (:multiFirm IS NULL or u.multi_firm_user = :multiFirm)
            GROUP BY u.id
            """, nativeQuery = true)
    Page<UserAuditFirmProjection> findAllUsersForAuditWithFirm(
            @Param("searchTerm") String searchTerm,
            @Param("firmId") UUID firmId,
            @Param("silasRole") String silasRole,
            @Param("appId") UUID appId,
            @Param("userType") String userType,
            @Param("multiFirm") Boolean multiFirm,
            Pageable pageable);

    /**
     * Simplified query for sorting by account status
     * Returns calculated account status for each user
     * Uses same filtering logic as main audit query for consistency
     */
    @Query(value = """
            SELECT u.id as userId,
                   CASE
                       WHEN EXISTS (
                           SELECT 1 FROM user_profile
                           WHERE entra_user_id = u.id
                           AND status = 'PENDING'
                       ) THEN 'Pending'
                       WHEN u.status = 'DEACTIVE' THEN 'Disabled'
                       ELSE 'Active'
                   END as accountStatus
            FROM entra_user u
            LEFT JOIN user_profile up ON up.entra_user_id = u.id
            LEFT JOIN (
                SELECT DISTINCT up_role.entra_user_id
                FROM user_profile up_role
                JOIN user_profile_app_role upar ON upar.user_profile_id = up_role.id
                JOIN app_role ar ON ar.id = upar.app_role_id
                WHERE ar.authz_role = true
                AND (:silasRole IS NULL OR :silasRole = '' OR ar.name = :silasRole)
            ) role_filter ON role_filter.entra_user_id = u.id
            LEFT JOIN (
                SELECT DISTINCT up_app.entra_user_id
                FROM user_profile up_app
                JOIN user_profile_app_role upar_app ON upar_app.user_profile_id = up_app.id
                JOIN app_role ar_app ON ar_app.id = upar_app.app_role_id
                WHERE :appId IS NULL OR ar_app.app_id = CAST(:appId AS uuid)
            ) app_filter ON app_filter.entra_user_id = u.id
            WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                   LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND (:firmId IS NULL OR up.firm_id = CAST(:firmId AS uuid))
            AND (:silasRole IS NULL OR :silasRole = '' OR role_filter.entra_user_id IS NOT NULL)
            AND (:appId IS NULL OR app_filter.entra_user_id IS NOT NULL)
            AND (:userType IS NULL OR up.user_type = :userType)
            AND (:multiFirm IS NULL or u.multi_firm_user = :multiFirm)
            GROUP BY u.id
            """, countQuery = """
            SELECT COUNT(DISTINCT u.id)
            FROM entra_user u
            LEFT JOIN user_profile up ON up.entra_user_id = u.id
            WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                   LOWER(CONCAT(u.first_name, ' ', u.last_name)) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                   LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            AND (:firmId IS NULL OR up.firm_id = CAST(:firmId AS uuid))
            AND (:silasRole IS NULL OR :silasRole = '' OR EXISTS (
                SELECT 1 FROM user_profile up2
                JOIN user_profile_app_role upar ON upar.user_profile_id = up2.id
                JOIN app_role ar ON ar.id = upar.app_role_id
                WHERE up2.entra_user_id = u.id
                AND ar.authz_role = true
                AND ar.name = :silasRole
            ))
            AND (:appId IS NULL OR EXISTS (
                SELECT 1 FROM user_profile up2
                JOIN user_profile_app_role upar ON upar.user_profile_id = up2.id
                JOIN app_role ar ON ar.id = upar.app_role_id
                WHERE up2.entra_user_id = u.id
                AND ar.app_id = CAST(:appId AS uuid)
            ))
            AND (:userType IS NULL OR up.user_type = :userType)
            AND (:multiFirm IS NULL or u.multi_firm_user = :multiFirm)
            """, nativeQuery = true)
    Page<UserAuditAccountStatusProjection> findAllUsersForAuditWithAccountStatus(
            @Param("searchTerm") String searchTerm,
            @Param("firmId") UUID firmId,
            @Param("silasRole") String silasRole,
            @Param("appId") UUID appId,
            @Param("userType") String userType,
            @Param("multiFirm") Boolean multiFirm,
            Pageable pageable);

    boolean existsByEntraOidAndEnabledFalse(String id);

    @Query("""
            SELECT 'Unlinked multi-firm users', NULL, COUNT(eu.id)
                FROM EntraUser eu
                LEFT JOIN UserProfile up
                ON up.entraUser.id = eu.id
                WHERE eu.multiFirmUser = TRUE
                AND up.id IS NULL
            """)
    List<Object[]> findUnlinkedMultifirmUsersCount();

    @Query("""
            SELECT 'Total multi-firm users', NULL, COUNT(*)
                FROM EntraUser
                WHERE multiFirmUser = TRUE
            """)
    List<Object[]> findTotalMultiFirmUsersCount();
}
