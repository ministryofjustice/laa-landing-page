package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "entra_last_sync_metadata")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntraLastSyncMetadata {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_successful_to")
    private LocalDateTime lastSuccessfulTo;
}
