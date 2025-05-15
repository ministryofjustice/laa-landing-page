package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class for LAA Applications
 */
@Entity
@Table(name = "laa_app")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class LaaApp implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "laa_app_id", nullable = false)
    @JsonIgnore
    private UUID laaAppId;

    @Column(name = "app_name", nullable = false)
    private String appName;

    @OneToOne
    @JoinColumn(name = "entra_app_registration_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_laa_app_entra_app_registration_id"))
    @ToString.Exclude
    @JsonIgnore
    private EntraAppRegistration entraAppRegistration;

    @OneToMany(mappedBy = "laaApp")
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaAppRole> appRoles;

}