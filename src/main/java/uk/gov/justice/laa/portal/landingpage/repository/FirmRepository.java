package uk.gov.justice.laa.portal.landingpage.repository;

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

    @Query("""
            SELECT f.name, f.code, COUNT(up.id) FROM Firm f
            JOIN UserProfile up ON up.firm.id = f.id
            JOIN EntraUser eu ON eu.id = up.entraUser.id
            WHERE eu.multiFirmUser = TRUE
            GROUP BY f.id, f.name, f.code
            """)
    List<Object[]>findMultiFirmUserCountsByFirm();
}
