package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Entity
@Table(name = "laa_app", indexes = {
        @Index(name = "LaaAppCreatedByIdx", columnList = "created_by"),
        @Index(name = "LaaAppCreatedDateIdx", columnList = "created_date"),
        @Index(name = "LaaAppLastModifiedDateIdx", columnList = "last_modified_date"),
        @Index(name = "LaaAppLastModifiedByIdx", columnList = "last_modified_by"),
        @Index(name = "LaaAppNameIdx", columnList = "name"),
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class LaaApp extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @Size(min = 1, max = 255)
    private String name;

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