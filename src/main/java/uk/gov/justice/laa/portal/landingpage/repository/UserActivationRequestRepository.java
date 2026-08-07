package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;
import uk.gov.justice.laa.portal.landingpage.entity.UserActivationRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserActivationRequestRepository extends JpaRepository<UserActivationRequest, UUID> {

    Optional<UserActivationRequest> findFirstByRequestIdOrderByVersionDesc(UUID requestId);

    Optional<UserActivationRequest> findFirstByUserProfileIdOrderByCreatedAtDescVersionDesc(UUID userProfileId);

    List<UserActivationRequest> findAllByRequestIdOrderByVersionAsc(UUID requestId);

    @Query("SELECT COALESCE(MAX(u.version), 0) FROM UserActivationRequest u WHERE u.requestId = :requestId")
    Integer findMaxVersionByRequestId(@Param("requestId") UUID requestId);

    @Query("SELECT r FROM UserActivationRequest r WHERE r.version = 1 AND r.requestId IN :requestIds")
    List<UserActivationRequest> findAllFirstVersionsByRequestIdIn(@Param("requestIds") Set<UUID> requestIds);

    @Query("""
                SELECT r
                FROM UserActivationRequest r
                WHERE r.version = (
                    SELECT MAX(sub.version)
                    FROM UserActivationRequest sub
                    WHERE sub.requestId = r.requestId
                )
                ORDER BY r.createdAt DESC
            """)
    List<UserActivationRequest> findAllLatestRequests();

    @Query(value = """
                SELECT r
                FROM UserActivationRequest r
                WHERE r.version = (
                    SELECT MAX(sub.version)
                    FROM UserActivationRequest sub
                    WHERE sub.requestId = r.requestId
                )
            """, countQuery = """
                SELECT COUNT(DISTINCT r.requestId)
                FROM UserActivationRequest r
            """)
    Page<UserActivationRequest> findAllLatestRequests(Pageable pageable);

    @Query("""
            SELECT new uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto(
                u.id,
                u.requestId,
                u.userProfileId,
                u.version,
                u.status,
                u.comments,
                u.actorEntraOid,
                u.actorRoleType,
                u.createdAt,
                CONCAT(
                    COALESCE(e.firstName, 'Unknown'),
                    ' ',
                    COALESCE(e.lastName, 'User')
                )
            )
            FROM UserActivationRequest u,
                 EntraUser e
            WHERE e.entraOid = u.actorEntraOid
              AND u.requestId = :requestId
            ORDER BY u.version ASC
            """)
    List<UserActivationRequestSummaryDto> findRequestHistoryByRequestId(@Param("requestId") UUID requestId);

}
