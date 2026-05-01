package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatus;
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

    @Query("""
        SELECT u FROM UserAccountStatusAudit u
        WHERE u.statusChange = :statusChange
        AND (:searchTerm IS NULL OR LOWER(u.userEmail) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS String), '%')))
        """)
    Page<UserAccountStatusAudit> findDeletedUsers(
            @Param("statusChange") UserAccountStatus statusChange,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);
}
