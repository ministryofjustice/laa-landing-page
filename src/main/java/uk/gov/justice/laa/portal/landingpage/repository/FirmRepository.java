package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.Firm;

@Repository
public interface FirmRepository extends JpaRepository<Firm, UUID> {
    Firm findFirmByName(String name);

    Firm findByCode(String code);

    @Query("SELECT f FROM Firm f WHERE LOWER(f.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(f.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Firm> findByNameOrCodeContaining(@Param("searchTerm") String searchTerm);

    @Query("SELECT f FROM Firm f LEFT JOIN Office o ON o.firm.id = f.id GROUP BY f HAVING COUNT(o.id) = 0")
    List<Firm> findFirmsWithoutOffices();
}
