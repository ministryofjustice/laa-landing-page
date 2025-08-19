package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "distributed_lock")
public class DistributedLock {

    @Id
    @Column(name = "lock_key", nullable = false, unique = true)
    private String key;

    @Column(name = "locked_until", nullable = false)
    private LocalDateTime lockedUntil;

    @Column(name = "locked_by", nullable = false)
    private String lockedBy;
}
