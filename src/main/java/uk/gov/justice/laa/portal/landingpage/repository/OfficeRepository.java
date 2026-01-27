package uk.gov.justice.laa.portal.landingpage.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.Office;

@Repository
public interface OfficeRepository extends JpaRepository<Office, UUID> {
    List<Office> findOfficeByFirm_IdIn(List<UUID> firmIds);

    List<Office> findOfficeByIdIn(List<UUID> ids);

    Office findByCode(String code);

    List<Office> findByFirm(Firm firm);

    // Performance Optimizations for PDA Sync
    /**
     * Fetch all offices with firm and parent firm eagerly loaded (avoids N+1 queries).
     * Use this instead of findAll() when firm relationships are needed.
     */
    @Query("SELECT DISTINCT o FROM Office o JOIN FETCH o.firm f LEFT JOIN FETCH f.parentFirm")
    List<Office> findAllWithFirm();
}
