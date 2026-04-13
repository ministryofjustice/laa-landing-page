package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;

import java.util.List;
import java.util.UUID;

public interface UserAccountStatusAuditRepository extends JpaRepository<UserAccountStatusAudit, UUID> {

    List<UserAccountStatusAudit> findByEntraUser(EntraUser entraUser);

    @Query("""
        SELECT u FROM UserAccountStatusAudit u
        WHERE u.entraUser.id = :entraUserId
        ORDER BY u.statusChangedDate DESC
        """)
    List<UserAccountStatusAudit> findByEntraUserIdOrderByStatusChangedDateDesc(@Param("entraUserId") UUID entraUserId);
}
