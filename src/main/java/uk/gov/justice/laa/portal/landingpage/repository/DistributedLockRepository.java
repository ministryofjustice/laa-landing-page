package uk.gov.justice.laa.portal.landingpage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.DistributedLock;

import java.time.LocalDateTime;

public interface DistributedLockRepository extends JpaRepository<DistributedLock, String> {

    @Modifying
    @Query("UPDATE DistributedLock l SET l.lockedUntil = :lockedUntil, l.lockedBy = :lockedBy "
            + "WHERE l.key = :key AND (l.lockedUntil < CURRENT_TIMESTAMP OR l.lockedBy = :lockedBy)")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    int acquireLock(@Param("key") String key,
                    @Param("lockedUntil") LocalDateTime lockedUntil,
                    @Param("lockedBy") String lockedBy);

    @Modifying
    @Query("DELETE FROM DistributedLock l WHERE l.key = :key AND l.lockedBy = :lockedBy")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void releaseLock(@Param("key") String key, @Param("lockedBy") String lockedBy);
}
