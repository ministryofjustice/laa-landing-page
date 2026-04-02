package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;

import java.util.Set;

@Entity
@Table(name = "app", indexes = {
    @Index(name = "AppNameIdx", columnList = "name"),
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class App extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Application name must be provided")
    @Size(min = 1, max = 255, message = "Application name must be between 1 and 255 characters")
    private String name;

    @Column(name = "description", nullable = false, length = 255, unique = true, columnDefinition = "TEXT")
    @NotBlank(message = "Application description must be provided")
    @Size(min = 1, max = 255, message = "Application description must be between 1 and 255 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_type", nullable = false, length = 255, unique = false)
    @NotNull(message = "Application Type must be provided")
    private AppType appType;

    @Column(name = "ordinal", nullable = false)
    @ColumnDefault("0")
    private int ordinal;

    @Column(name = "url", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Application url must be provided")
    @Size(min = 1, max = 255, message = "Application url must be between 1 and 255 characters")
    private String url;

    @Column(name = "enabled", nullable = false)
    @ColumnDefault("true")
    private boolean enabled;

    @Column(name = "entra_app_id", nullable = true, length = 255, unique = true)
    @Size(max = 255, message = "Entra App ID must be less than 255 characters")
    private String entraAppId;

    @Column(name = "entra_oid", nullable = true, length = 255, unique = true)
    @Size(max = 255, message = "Entra App Object OID must be less than 255 characters")
    private String entraOid;

    @Column(name = "security_group_oid", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Security Group Oid must be provided")
    @Size(max = 255, message = "Security Group Oid must be less than 255 characters")
    private String securityGroupOid;

    @OneToMany(mappedBy = "app", cascade = CascadeType.PERSIST)
    @ToString.Exclude
    @JsonIgnore
    private Set<AppRole> appRoles;

}
