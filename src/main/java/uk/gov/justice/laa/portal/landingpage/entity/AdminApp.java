package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "admin_app", indexes = {
    @Index(name = "AdminAppNameIdx", columnList = "name"),
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class AdminApp extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Admin app name must be provided")
    @Size(min = 1, max = 255, message = "Admin app name must be between 1 and 255 characters")
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Admin app description must be provided")
    @Size(min = 1, max = 1000, message = "Admin app description must be between 1 and 1000 characters")
    private String description;

    @Column(name = "ordinal", nullable = false)
    @ColumnDefault("0")
    private int ordinal;

    @Column(name = "enabled", nullable = false)
    @ColumnDefault("true")
    private boolean enabled;
}
