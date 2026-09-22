package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface EntraUserRepositoryCustomAuditSearch {
    Page<Object[]> findAuditUsersWithDynamicProjection(
            String sortType,
            String searchTerm,
            UUID firmId,
            String silasRole,
            UUID appId,
            String userType,
            Boolean multiFirm,
            Boolean inactiveSinceDateFlag,
            Boolean neverActivated,
            Pageable pageable
    );
}
