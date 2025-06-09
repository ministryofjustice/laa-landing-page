package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
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

import java.util.Set;

@Entity
@Table(name = "app_registration", indexes = {
    @Index(name = "AppRegistrationNameIdx", columnList = "name")
    }
)
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class AppRegistration extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "App registration name must be provided")
    @Size(min = 1, max = 255, message = "App registration name must be between 1 and 255 characters")
    private String name;

    @ManyToMany(mappedBy = "userAppRegistrations", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<EntraUser> entraUsers;

    @OneToOne(mappedBy = "appRegistration", cascade = CascadeType.PERSIST)
    @ToString.Exclude
    @JsonIgnore
    private App app;

}