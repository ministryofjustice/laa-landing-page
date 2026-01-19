package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.UUID;

@Repository
public interface FirmRepository extends JpaRepository<Firm, UUID> {
    Firm findFirmByName(String name);
    
    @Query("SELECT f FROM Firm f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Firm> findByNameOrCodeContaining(@Param("searchTerm") String searchTerm);

    Page<Firm> findAllByName(String name, Pageable pageable);

    Page<Firm> findAllById(UUID id, Pageable pageable);

    Page<Firm> findAllByType(FirmType type, Pageable attr2);

    /**
     * Query for Firm directory - fetches all firms
     * Supports filtering by search Term and Firm Type
     */

    @Query(
            value = """
                    SELECT DISTINCT f
                    FROM Firm f
                    WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                           LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                           LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                      AND (:firmId IS NULL OR f.id = :firmId)
                      AND (COALESCE(:firmType, f.type) = f.type)
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT f.id)
                    FROM Firm f
                    WHERE (:searchTerm IS NULL OR :searchTerm = '' OR
                           LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
                           LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                      AND (:firmId IS NULL OR f.id = :firmId)
                      AND (COALESCE(:firmType, f.type) = f.type)
                    """
    )
    Page<Firm> findAllFirms(
            @Param("searchTerm") String searchTerm,
            @Param("firmId") UUID firmId,
            @Param("firmType") FirmType firmType,
            Pageable pageable);
}
