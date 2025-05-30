package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "entra_app_registration", indexes = {
    @Index(name = "EntraAppRegistrationCreatedByIdx", columnList = "created_by"),
    @Index(name = "EntraAppRegistrationCreatedDateIdx", columnList = "created_date"),
    @Index(name = "EntraAppRegistrationLastModifiedDateIdx", columnList = "last_modified_date"),
    @Index(name = "EntraAppRegistrationLastModifiedByIdx", columnList = "last_modified_by"),
    @Index(name = "EntraAppRegistrationNameIdx", columnList = "name")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class EntraAppRegistration extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Entra app registration name must be provided")
    @Size(min = 1, max = 255, message = "Entra app registration name must be between 1 and 255 characters")
    private String name;

    @ManyToMany(mappedBy = "userAppRegistrations", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<EntraUser> entraUsers;

    @OneToOne(mappedBy = "entraAppRegistration")
    @ToString.Exclude
    @JsonIgnore
    private LaaApp laaApp;

}