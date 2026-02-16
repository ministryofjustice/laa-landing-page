package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;

@Repository
public interface FirmRepository extends JpaRepository<Firm, UUID> {
    Firm findFirmByName(String name);

    Firm findByCode(String code);

    Firm findByCodeAndEnabledTrue(String code);

    @Query("SELECT f FROM Firm f WHERE f.enabled = true AND (LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Firm> findByNameOrCodeContaining(@Param("searchTerm") String searchTerm);

    List<Firm> findAllByEnabledTrue();

    @Query("SELECT f FROM Firm f WHERE f.enabled = true AND NOT EXISTS (SELECT 1 FROM Office o WHERE o.firm.id = f.id)")
    List<Firm> findFirmsWithoutOffices();

    // Performance Optimizations for PDA Sync
    /**
     * Fetch all firms with parent firm eagerly loaded (avoids N+1 queries).
     * Use this instead of findAll() when parent firm relationships are needed.
     */
    @Query("SELECT DISTINCT f FROM Firm f LEFT JOIN FETCH f.parentFirm")
    List<Firm> findAllWithParentFirm();

    /**
     * Query for Firm directory - fetches all firms
     * Supports filtering by search Term, FirmID, Firm Type, and enabled status
     */

    @Query(
            value = """
                    SELECT DISTINCT f
                    FROM Firm f
                    WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                           LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                           LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                      AND (COALESCE(:firmType, f.type) = f.type)
                      AND (:includeDisabled = true OR f.enabled = true)
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT f.id)
                    FROM Firm f
                    WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                           LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                           LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                      AND (COALESCE(:firmType, f.type) = f.type)
                      AND (:includeDisabled = true OR f.enabled = true)
                    """
    )
    Page<Firm> getFirmsPage(
            @Param("searchTerm") String searchTerm,
            @Param("firmType") FirmType firmType,
            @Param("includeDisabled") boolean includeDisabled,
            Pageable pageable);
}
