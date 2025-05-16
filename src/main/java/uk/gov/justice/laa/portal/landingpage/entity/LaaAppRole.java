package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class for LAA Application Roles
 */
@Entity
@Table(name = "laa_app_role")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class LaaAppRole implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "app_role_id", nullable = false)
    private UUID appRoleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @ManyToOne
    @JoinColumn(name = "laa_app_id", nullable = false, foreignKey = @ForeignKey(name = "FK_laa_app_role_laa_app_id"))
    @ToString.Exclude
    @JsonIgnore
    private LaaApp laaApp;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_profile_app_role",
            joinColumns = @JoinColumn(name = "app_role_id"),
            foreignKey = @ForeignKey(name = "FK_laa_app_role_app_role_id"),
            inverseJoinColumns = @JoinColumn(name = "user_profile_id"),
            inverseForeignKey = @ForeignKey(name = "FK_laa_app_role_user_profile_id")
    )
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaUserProfile> userProfiles = new HashSet<>();

}