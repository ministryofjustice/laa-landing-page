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
import jakarta.persistence.ManyToOne;
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
public class LaaAppRole extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Application role name must be provided")
    @Size(min = 1, max = 255, message = "Application role name must be between 1 and 255 characters")
    private String name;

    @ManyToOne
    @JoinColumn(name = "app_id", nullable = false, foreignKey = @ForeignKey(name = "FK_app_role_app_id"))
    @ToString.Exclude
    @JsonIgnore
    private LaaApp laaApp;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_profile_app_role",
            joinColumns = @JoinColumn(name = "app_role_id"),
            foreignKey = @ForeignKey(name = "FK_app_role_app_role_id"),
            inverseJoinColumns = @JoinColumn(name = "user_profile_id"),
            inverseForeignKey = @ForeignKey(name = "FK_app_role_user_profile_id")
    )
    @ToString.Exclude
    @JsonIgnore
    @Builder.Default
    private Set<LaaUserProfile> userProfiles = new HashSet<>();

}