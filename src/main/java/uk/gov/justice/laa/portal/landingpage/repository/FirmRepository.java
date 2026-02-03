package uk.gov.justice.laa.portal.landingpage.repository;

import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;

import java.util.List;
import java.util.UUID;

@Repository
public interface FirmRepository extends JpaRepository<Firm, UUID> {
    Firm findFirmByName(String name);
    
    @Query("SELECT f FROM Firm f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Firm> findByNameOrCodeContaining(@Param("searchTerm") String searchTerm);

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
}
