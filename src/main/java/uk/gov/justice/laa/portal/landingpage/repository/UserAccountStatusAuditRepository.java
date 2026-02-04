package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;

import java.util.List;
import java.util.UUID;

public interface UserAccountStatusAuditRepository extends JpaRepository<UserAccountStatusAudit, UUID> {
    List<UserAccountStatusAudit> findByEntraUser(EntraUser entraUser);
}
