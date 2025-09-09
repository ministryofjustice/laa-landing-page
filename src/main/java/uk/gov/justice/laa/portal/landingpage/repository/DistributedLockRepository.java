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
    @Query(value = """
                INSERT INTO distributed_lock (lock_key, locked_until, locked_by)
                VALUES (:key, :lockedUntil, :lockedBy)
                ON CONFLICT (lock_key)
                DO UPDATE SET
                    locked_until = EXCLUDED.locked_until,
                    locked_by = EXCLUDED.locked_by
                WHERE distributed_lock.locked_until < CURRENT_TIMESTAMP
            """, nativeQuery = true)
    @Transactional
    int acquireLock(@Param("key") String key,
                    @Param("lockedUntil") LocalDateTime lockedUntil,
                    @Param("lockedBy") String lockedBy);

    @Modifying
    @Query("DELETE FROM DistributedLock l WHERE l.key = :key AND l.lockedBy = :lockedBy")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void releaseLock(@Param("key") String key, @Param("lockedBy") String lockedBy);
}
