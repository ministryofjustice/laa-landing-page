package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;

import java.util.List;
import java.util.UUID;

public interface UserAccountStatusAuditRepository extends JpaRepository<UserAccountStatusAudit, UUID> {

    List<UserAccountStatusAudit> findByEntraUserId(UUID entraUserId);

    @Query("""
        SELECT u FROM UserAccountStatusAudit u
        WHERE u.entraUserId = :entraUserId
        ORDER BY u.statusChangedDate DESC
        """)
    List<UserAccountStatusAudit> findByEntraUserIdOrderByStatusChangedDateDesc(@Param("entraUserId") UUID entraUserId);

    @Query("""
            SELECT u FROM UserAccountStatusAudit u
            WHERE u.statusChange = 'DELETED'
            AND (
                :searchTerm IS NULL
                OR :searchTerm = ''
                OR LOWER(CAST(u.userEmail AS string)) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                OR LOWER(u.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                OR LOWER(replace(u.userName, ' ', '')) LIKE LOWER(CONCAT('%', replace(:searchTerm, ' ', ''), '%'))
            )
            """)
    Page<UserAccountStatusAudit> findDeletedUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
}
