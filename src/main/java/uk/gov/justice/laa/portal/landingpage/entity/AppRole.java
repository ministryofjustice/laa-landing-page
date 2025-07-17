package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "app_role", indexes = {
    @Index(name = "AppRoleNameIdx", columnList = "name"),
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class AppRole extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Application role name must be provided")
    @Size(min = 1, max = 255, message = "Application role name must be between 1 and 255 characters")
    private String name;

    @ManyToOne
    @JoinColumn(name = "app_id", nullable = false, foreignKey = @ForeignKey(name = "FK_app_role_app_id"))
    @ToString.Exclude
    @JsonIgnore
    private App app;

    @ManyToMany(mappedBy = "appRoles", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private Set<UserProfile> userProfiles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 255)
    @NotNull(message = "App role type must be provided")
    private RoleType roleType;

    @Column(name = "user_type_restriction", nullable = true, length = 255)
    private String userTypeRestriction;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @Column(name = "authz_role")
    private boolean authzRole;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "app_role_permission",
            joinColumns = @JoinColumn(name = "app_role_id"),
            foreignKey = @ForeignKey(name = "FK_app_role_permission_app_role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"),
            inverseForeignKey = @ForeignKey(name = "FK_app_role_permission_permission_id")
    )
    @ToString.Exclude
    @JsonIgnore
    private List<Permission> permissions;

    @OneToMany(targetEntity = AppRole.class)
    @JoinColumn(name = "id", referencedColumnName = "id")
    private Map<String, AppRole> roleAssignments;

}