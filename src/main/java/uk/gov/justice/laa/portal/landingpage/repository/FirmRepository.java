package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;

import java.util.List;
import java.util.UUID;

@Repository
public interface FirmRepository extends JpaRepository<Firm, UUID> {
    Firm findFirmByName(String name);
    
    @Query("SELECT f FROM Firm f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Firm> findByNameOrCodeContaining(@Param("searchTerm") String searchTerm);

    /**
     * Query for Firm directory - fetches all firms
     * Supports filtering by search Term, FirmID and Firm Type
     */

    @Query(
            value = """
                    SELECT DISTINCT f
                    FROM Firm f
                    WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                           LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                           LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                      AND (COALESCE(:firmType, f.type) = f.type)
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT f.id)
                    FROM Firm f
                    WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                           LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                           LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                      AND (COALESCE(:firmType, f.type) = f.type)
                    """
    )
    Page<Firm> getFirmsPage(
            @Param("searchTerm") String searchTerm,
            @Param("firmType") FirmType firmType,
            Pageable pageable);

    @Query(value = """
        SELECT
                f.id AS firmId,
                f.name AS firmName,
                f.code AS firmCode,
                r.name AS roleName,
                COUNT(DISTINCT up.id) AS userCount
        FROM firm f
        JOIN user_profile up ON up.firm_id = f.id
        JOIN user_profile_app_role upar ON upar.user_profile_id = up.id
        JOIN app_role r ON r.id = upar.app_role_id
        WHERE 'EXTERNAL' = ANY(r.user_type_restriction)
        GROUP BY f.id, f.name, f.code, r.name
        ORDER BY f.name, r.name
        """, nativeQuery = true)
    List<Tuple> findRoleCountsByFirm();

    @Query(
            value = """
        SELECT
            f.name AS "Firm Name",
            f.code AS "Firm Code",
            f.type AS "Firm Type",
            pf.code AS "Parent Firm Code",
            COUNT(DISTINCT up.id) AS "User Count",
            COUNT(DISTINCT CASE WHEN ar.id IS NOT NULL THEN up.id END) AS "Admin User Count",
            COUNT(DISTINCT CASE WHEN eu.multi_firm_user = TRUE THEN up.id END) AS "Multi-Firm User Count",
            COUNT(DISTINCT CASE WHEN eu.status = 'DEACTIVE' THEN up.id END) AS "Disabled User Count"
        FROM firm f
        JOIN user_profile up
            ON up.firm_id = f.id
        LEFT JOIN firm pf
            ON pf.id = f.parent_firm_id
        LEFT JOIN user_profile_app_role upr
            ON upr.user_profile_id = up.id
        LEFT JOIN app_role ar
            ON ar.id = upr.app_role_id
            AND ar.name = 'Firm User Manager'
        LEFT JOIN entra_user eu
            ON eu.id = up.entra_user_id
        GROUP BY
            f.id, f.name, f.code, f.type, pf.code
        ORDER BY
            COALESCE(f.parent_firm_id, f.id),
            f.parent_firm_id NULLS FIRST,
            f.code
        """,
            nativeQuery = true
    )
    List<Object[]> findAllFirmExternalUserCount();
}
