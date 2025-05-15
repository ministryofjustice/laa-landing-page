package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.LaaApp;

import java.util.UUID;

/**
 * Repository class for LAA Apps
 */
@Repository
public interface LaaAppRepository extends JpaRepository<LaaApp, UUID> {
}
