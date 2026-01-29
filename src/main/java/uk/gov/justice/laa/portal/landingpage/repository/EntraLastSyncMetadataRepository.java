package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.justice.laa.portal.landingpage.entity.EntraLastSyncMetadata;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EntraLastSyncMetadataRepository extends JpaRepository<EntraLastSyncMetadata, String> {

    @Query("SELECT e FROM EntraLastSyncMetadata e WHERE e.id = 'ENTRA_USER_SYNC'")
    Optional<EntraLastSyncMetadata> getSyncMetadata();

    @Modifying
    @Query("UPDATE EntraLastSyncMetadata e SET e.updatedAt = :updatedAt, e.lastSuccessfulTo = :lastSuccessfulTo WHERE e.id = 'ENTRA_USER_SYNC'")
    int updateSyncMetadata(@Param("updatedAt") LocalDateTime updatedAt, @Param("lastSuccessfulTo") LocalDateTime lastSuccessfulTo);
}
