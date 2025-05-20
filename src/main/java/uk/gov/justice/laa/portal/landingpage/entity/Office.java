package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "office", indexes = {
    @Index(name = "OfficeCreatedByIdx", columnList = "created_by"),
    @Index(name = "OfficeCreatedDateIdx", columnList = "created_date"),
    @Index(name = "OfficeLastModifiedDateIdx", columnList = "last_modified_date"),
    @Index(name = "OfficeLastModifiedByIdx", columnList = "last_modified_by"),
    @Index(name = "OfficeNameIdx", columnList = "name"),
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class Office extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @Size(min = 1, max = 255)
    private String name;

    @Column(name = "address", nullable = false, length = 500)
    @Size(min = 1, max = 500)
    private String address;

    @Column(name = "phone", nullable = false, length = 255)
    @Size(min = 1, max = 255)
    private String phone;

    @ManyToOne
    @JoinColumn(name = "firm_id", nullable = false, foreignKey = @ForeignKey(name = "FK_office_firm_id"))
    @ToString.Exclude
    @JsonIgnore
    private Firm firm;

    @ManyToMany(mappedBy = "offices", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaUserProfile> userProfiles;

}