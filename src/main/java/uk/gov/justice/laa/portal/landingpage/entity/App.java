package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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

    @Column(name = "entra_app_id", nullable = true, length = 255, unique = true)
    @Size(max = 255, message = "Entra App ID must be less than 255 characters")
    private String entraAppId;

    @Column(name = "security_group_oid", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Security Group Oid must be provided")
    @Size(max = 255, message = "Security Group Oid must be less than 255 characters")
    private String securityGroupOid;

    @Column(name = "security_group_name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Security Group Name must be provided")
    @Size(max = 255, message = "Security Group Name must be less than 255 characters")
    private String securityGroupName;

    @OneToMany(mappedBy = "app", cascade = CascadeType.PERSIST)
    @ToString.Exclude
    @JsonIgnore
    private Set<AppRole> appRoles;

}