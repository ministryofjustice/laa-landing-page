package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@Table(name = "delete_user_reason")
@ToString(doNotUseGetters = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class DeleteUserReason extends BaseEntity {

    @Column(name = "code", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Reason code must be provided")
    @Size(min = 1, max = 255, message = "Reason code must be between 1 and 255 characters")
    private String code;

    @Column(name = "label", nullable = false, length = 255)
    @NotBlank(message = "Reason label must be provided")
    @Size(min = 1, max = 255, message = "Reason label must be between 1 and 255 characters")
    private String label;

    @Column(name = "editable_by_internal_user", nullable = false)
    @Builder.Default
    private boolean editableByInternalUser = false;

    @Column(name = "editable_by_external_user", nullable = false)
    @Builder.Default
    private boolean editableByExternalUser = false;

    @Column(name = "system_generated", nullable = false)
    @Builder.Default
    private boolean systemGenerated = false;
}
