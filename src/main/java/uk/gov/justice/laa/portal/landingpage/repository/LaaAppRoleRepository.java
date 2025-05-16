package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.LaaAppRole;

import java.util.UUID;

/**
 * Repository class for LAA App Roles
 */
@Repository
public interface LaaAppRoleRepository extends JpaRepository<LaaAppRole, UUID> {
}
