package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
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
 * Entity class for Entra Application Registrations
 */
@Entity
@Table(name = "entra_app_registration")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class EntraAppRegistration implements Serializable {

    @Id
    @Column(name = "entra_app_registration_id", nullable = false)
    @JsonIgnore
    private UUID entraAppRegistrationId;

    @Column(name = "app_registration_name", nullable = false)
    private String appRegistrationName;

    @ManyToMany(mappedBy = "userAppRegistrations", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<EntraUser> entraUsers = new HashSet<>();

    @OneToOne(mappedBy = "entraAppRegistration")
    @ToString.Exclude
    @JsonIgnore
    private LaaApp laaApp;

}