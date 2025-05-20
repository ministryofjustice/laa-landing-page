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
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "laa_app_role", indexes = {
    @Index(name = "LaaAppRoleCreatedByIdx", columnList = "created_by"),
    @Index(name = "LaaAppRoleCreatedDateIdx", columnList = "created_date"),
    @Index(name = "LaaAppRoleLastModifiedDateIdx", columnList = "last_modified_date"),
    @Index(name = "LaaAppRoleLastModifiedByIdx", columnList = "last_modified_by"),
    @Index(name = "LaaAppRoleNameIdx", columnList = "name"),
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class LaaAppRole extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @Size(min = 1, max = 255)
    private String name;

    @ManyToOne
    @JoinColumn(name = "laa_app_id", nullable = false, foreignKey = @ForeignKey(name = "FK_laa_app_role_laa_app_id"))
    @ToString.Exclude
    @JsonIgnore
    private LaaApp laaApp;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_profile_app_role",
            joinColumns = @JoinColumn(name = "laa_app_role_id"),
            foreignKey = @ForeignKey(name = "FK_laa_app_role_app_role_id"),
            inverseJoinColumns = @JoinColumn(name = "laa_user_profile_id"),
            inverseForeignKey = @ForeignKey(name = "FK_laa_app_role_user_profile_id")
    )
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaUserProfile> userProfiles = new HashSet<>();

}