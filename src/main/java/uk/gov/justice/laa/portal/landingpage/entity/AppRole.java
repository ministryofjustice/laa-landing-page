package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
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

    @Column(name = "ordinal", nullable = false, unique = false)
    private int ordinal;

    @Column(name = "ccms_code", nullable = true, length = 30, unique = true)
    @Size(max = 30, message = "Application role CCMS Code must be no more than 30 characters")
    private String ccmsCode;

    @Column(name = "legacy_sync", nullable = false)
    @ColumnDefault("false")
    @NotNull(message = "Application role legacy sync flag must be provided")
    private boolean legacySync;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "firm_type_restriction", nullable = true, columnDefinition = "firm_type_enum[]")
    private FirmType[] firmTypeRestriction;

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

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type_restriction", nullable = true, columnDefinition = "text[]")
    private UserType[] userTypeRestriction;

    @Column(name = "description", nullable = false, length = 255)
    @NotBlank(message = "Application role description must be provided")
    @Size(min = 1, max = 255, message = "Application role description must be between 1 and 255 characters")
    private String description;

    @Column(name = "authz_role", nullable = false)
    @ColumnDefault("false")
    private boolean authzRole;

    @ElementCollection(targetClass = Permission.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "app_role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<Permission> permissions;

    @OneToMany(mappedBy = "assigningRole", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    @JsonManagedReference("assigning-role")
    @Builder.Default
    private Set<RoleAssignment> assigningRoles = new HashSet<>();

    @OneToMany(mappedBy = "assignableRole", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    @JsonManagedReference("assignable-role")
    @Builder.Default
    private Set<RoleAssignment> assignableRoles = new HashSet<>();

}
