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
 * Entity class for LAA Users
 */
@Entity
@Table(name = "laa_user_profile")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class LaaUserProfile implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_profile_id", nullable = false)
    private UUID userProfileId;

    @Column(name = "is_multi_firm", nullable = false)
    private boolean isMultiFirm;

    @Column(name = "is_admin", nullable = false)
    private boolean isAdmin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firm_id", foreignKey = @ForeignKey(name = "FK_laa_user_profile_firm_id"))
    @ToString.Exclude
    @JsonIgnore
    private Firm firm;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_profile_office",
            joinColumns = @JoinColumn(name = "user_profile_id"),
            foreignKey = @ForeignKey(name = "FK_laa_user_profile_office_user_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "office_id"),
            inverseForeignKey = @ForeignKey(name = "FK_laa_user_profile_office_office_id")
    )
    @ToString.Exclude
    @JsonIgnore
    private Set<Office> offices = new HashSet<>();


    @ManyToMany(mappedBy = "userProfiles", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaAppRole> appRoles = new HashSet<>();

}