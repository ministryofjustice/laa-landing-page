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
 * Entity class for LAA Office
 */
@Entity
@Table(name = "office")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class Office implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "office_id", nullable = false)
    @JsonIgnore
    private UUID officeId;

    @Column(name = "office_name", nullable = false)
    private String officeName;

    @Column(name = "office_address", length = 500)
    private String officeAddress;

    @Column(name = "office_phone")
    private String officePhone;

    @ManyToOne
    @JoinColumn(name = "firm_id", nullable = false, foreignKey = @ForeignKey(name = "FK_office_firm_id"))
    @ToString.Exclude
    @JsonIgnore
    private Firm firm;

    @ManyToMany(mappedBy = "offices", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaUserProfile> userProfiles = new HashSet<>();

}