package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
@Table(name = "permission", indexes = {
    @Index(name = "PermissionNameIdx", columnList = "name"),
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Permission name must be provided")
    @Size(min = 1, max = 255, message = "Application name must be between 1 and 255 characters")
    private String name;

    @Column(name = "function", nullable = false, length = 255, unique = false)
    @Size(max = 255, message = "Permission function must be less than 255 characters")
    private String function;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "permission_id"),
            foreignKey = @ForeignKey(name = "FK_permission_app_role_permission_id"),
            inverseJoinColumns = @JoinColumn(name = "app_role_id"),
            inverseForeignKey = @ForeignKey(name = "FK_permission_app_role_app_role_id")
    )
    @ToString.Exclude
    @JsonIgnore
    private Set<AppRole> appRoles;

}