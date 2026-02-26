package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.Office;

import java.util.List;
import java.util.UUID;

@Repository
public interface OfficeRepository extends JpaRepository<Office, UUID> {
    List<Office> findOfficeByFirm_IdIn(List<UUID> firmIds);

    List<Office> findOfficeByIdIn(List<UUID> ids);



    @Query(
            value = """
        SELECT * FROM office 
        WHERE firm_id = :id
        """,
            countQuery = """
        SELECT count(*) FROM office 
        WHERE firm_id = :id
        """,
            nativeQuery = true
    )
    Page<Office> findAllByFirmId(UUID id, Pageable pageable);


}
