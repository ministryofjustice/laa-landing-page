package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.Office;

import java.util.List;
import java.util.UUID;

@Repository
public interface OfficeRepository extends JpaRepository<Office, UUID> {
    List<Office> findOfficeByFirm_IdIn(List<UUID> firmIds);

    List<Office> findOfficeByIdIn(List<UUID> ids);
}
