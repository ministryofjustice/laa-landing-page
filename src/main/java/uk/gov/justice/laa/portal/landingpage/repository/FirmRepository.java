package uk.gov.justice.laa.portal.landingpage.repository;

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

    Page<Firm> findAllByName(String name, Pageable pageable);

    Page<Firm> findAllById(UUID id, Pageable pageable);

    Page<Firm> findAllByType(FirmType type, Pageable attr2);
}
