package uk.gov.justice.laa.portal.landingpage.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@Repository
public interface EntraUserRepository extends JpaRepository<EntraUser, UUID>, EntraUserRepositoryCustomAuditSearch {
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


    /* ---------------- INTERNAL USERS ---------------- */

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'INTERNAL'
        """)
    long countInternalUsers();

    /* ---------------- TOTAL EXTERNAL USERS ---------------- */

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
        """)
    long countTotalExternalUsers();


    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.multiFirmUser = TRUE
        """)
    long countTotalExternalMultiFirmUsers();


    /* ---------------- ACTIVE EXTERNAL USERS ---------------- */

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.enabled = TRUE
          AND eu.invitationStatus = 'VERIFICATION_SUCCESS'
        """)
    long countActiveExternalUsers();


    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.enabled = TRUE
          AND eu.invitationStatus = 'VERIFICATION_SUCCESS'
          AND eu.multiFirmUser = true
        """)
    long countActiveExternalMultiFirmUsers();

    /* ---------------- COMPLETE EXTERNAL USERS ---------------- */
    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.enabled = TRUE
          AND eu.invitationStatus = 'VERIFICATION_SUCCESS'
          AND NOT EXISTS (
              SELECT 1
              FROM UserProfile up2
              WHERE up2.entraUser.id = eu.id
                AND up2.appRoles IS EMPTY
          )
        """)
    long countCompleteExternalUsers();

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.enabled = TRUE
          AND eu.invitationStatus = 'VERIFICATION_SUCCESS'
          AND eu.multiFirmUser = TRUE
          AND NOT EXISTS (
              SELECT 1
              FROM UserProfile up2
              WHERE up2.entraUser.id = eu.id
                AND up2.appRoles IS EMPTY
          )
        """)
    long countCompleteExternalMultiFirmUsers();

    /* ---------------- EXTERNAL USERS WITH NO ROLES ASSIGNED ---------------- */

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.enabled = TRUE
          AND eu.invitationStatus = 'VERIFICATION_SUCCESS'
          AND EXISTS (
              SELECT 1
              FROM UserProfile up2
              WHERE up2.entraUser.id = eu.id
                AND up2.appRoles IS EMPTY
          )
        """)
    long countExternalUsersWithNoRoles();

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.enabled = TRUE
          AND eu.invitationStatus = 'VERIFICATION_SUCCESS'
                  AND eu.multiFirmUser = TRUE
          AND EXISTS (
              SELECT 1
              FROM UserProfile up2
              WHERE up2.entraUser.id = eu.id
                AND up2.appRoles IS EMPTY
          )
        """)
    long countExternalMultiFirmUsersWithNoRoles();

    /* ---------------- DISABLED EXTERNAL USERS ---------------- */

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.enabled = FALSE
          AND eu.invitationStatus = 'VERIFICATION_SUCCESS'
        """)
    long countDisabledExternalUsers();

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.enabled = FALSE
          AND eu.invitationStatus = 'VERIFICATION_SUCCESS'
          AND eu.multiFirmUser = TRUE
        """)
    long countDisabledExternalMultiFirmUsers();

    /* ---------------- INCOMPLETE EXTERNAL USERS ---------------- */

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.invitationStatus <> 'VERIFICATION_SUCCESS'
          AND EXISTS (
              SELECT 1
              FROM UserProfile up2
              WHERE up2.entraUser.id = eu.id
                AND up2.appRoles IS EMPTY
          )
        """)
    long countIncompleteExternalUsers();

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.invitationStatus <> 'VERIFICATION_SUCCESS'
          AND eu.multiFirmUser = TRUE
          AND EXISTS (
              SELECT 1
              FROM UserProfile up2
              WHERE up2.entraUser.id = eu.id
                AND up2.appRoles IS EMPTY
          )
        """)
    long countIncompleteExternalMultiFirmUsers();

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.invitationStatus <> 'VERIFICATION_SUCCESS'
          AND NOT EXISTS (
              SELECT 1
              FROM UserProfile up2
              WHERE up2.entraUser.id = eu.id
                AND up2.appRoles IS EMPTY
          )
        """)
    long countActivationPendingExternalUsers();

    @Query("""
        SELECT COUNT(DISTINCT eu.id)
        FROM EntraUser eu
        LEFT JOIN UserProfile up
            ON up.entraUser.id = eu.id
        WHERE up.userType = 'EXTERNAL'
          AND eu.invitationStatus <> 'VERIFICATION_SUCCESS'
          AND eu.multiFirmUser = TRUE
          AND NOT EXISTS (
              SELECT 1
              FROM UserProfile up2
              WHERE up2.entraUser.id = eu.id
                AND up2.appRoles IS EMPTY
          )
        """)
    long countActivationPendingExternalMultiFirmUsers();

    List<EntraUser> findEntraUserByCcmsEbsUserIsTrue();

    @Query("""
        SELECT DISTINCT u.firstName, u.lastName, u.email, up.createdDate
        FROM EntraUser u
        JOIN u.userProfiles up
        JOIN up.appRoles ar
        JOIN ar.app a
        WHERE up.userType = :userType
          AND a.name = :appName
          AND up.createdDate >= :startDate
          AND up.createdDate < :endDate
        """)
    List<Object[]> findCcmsUsersWithAppInPeriod(
            @Param("userType") UserType userType,
            @Param("appName") String appName,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
