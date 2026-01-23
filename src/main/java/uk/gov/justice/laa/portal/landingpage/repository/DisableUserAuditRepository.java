package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserAudit;

import java.util.UUID;

public interface DisableUserAuditRepository extends JpaRepository<DisableUserAudit, UUID> {
}
