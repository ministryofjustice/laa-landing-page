package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "created_date", nullable = false)
    @CreatedDate
    @NonNull
    private LocalDateTime createdDate;

    @Column(name = "created_by", nullable = false, length = 255)
    @NonNull
    @Size(min = 1, max = 255)
    @CreatedBy
    private String createdBy;

    @Column(name = "last_modified_date", nullable = true)
    @LastModifiedDate
    private LocalDateTime lastModified;

    @Column(name = "last_modified_by", nullable = true, length = 255)
    @Size(min = 1, max = 255)
    @LastModifiedBy
    private String lastModifiedBy;

}
