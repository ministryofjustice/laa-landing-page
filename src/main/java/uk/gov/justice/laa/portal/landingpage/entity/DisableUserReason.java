package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Setter
@SuperBuilder
@Table(name = "disable_user_reason")
@ToString(doNotUseGetters = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class DisableUserReason extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255)
    @NotBlank(message = "Reason name must be provided")
    @Size(min = 1, max = 255, message = "Reason name must be between 1 and 255 characters")
    private String name;

    @Column(name = "description", nullable = false, length = 255)
    @NotBlank(message = "Reason description must be provided")
    @Size(min = 1, max = 255, message = "Reason description must be between 1 and 255 characters")
    private String description;

    @Column(name = "entra_description", nullable = false, length = 255)
    @NotBlank(message = "Reason entra description must be provided")
    @Size(min = 1, max = 255, message = "Reason entra description must be between 1 and 255 characters")
    private String entraDescription;
}
