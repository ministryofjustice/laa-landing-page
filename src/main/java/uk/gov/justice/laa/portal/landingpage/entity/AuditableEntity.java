package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class AuditableEntity extends BaseEntity {

    @Column(name = "created_date", nullable = false)
    @CreatedDate
    @NotNull(message = "Created date must be provided")
    private LocalDateTime createdDate;

    @Column(name = "created_by", nullable = false, length = 255)
    @NotBlank(message = "Created by must be provided")
    @Size(min = 1, max = 255, message = "Created by must be between 1 and 255 characters")
    @CreatedBy
    private String createdBy;

    @Column(name = "last_modified_date", nullable = true)
    @LastModifiedDate
    private LocalDateTime lastModified;

    @Column(name = "last_modified_by", nullable = true, length = 255)
    @Nullable
    @Size(min = 1, max = 255, message = "Last modified by must be between 1 and 255 characters")
    @LastModifiedBy
    private String lastModifiedBy;

}
