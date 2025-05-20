package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "firm", indexes = {
        @Index(name = "FirmCreatedByIdx", columnList = "created_by"),
        @Index(name = "FirmCreatedDateIdx", columnList = "created_date"),
        @Index(name = "FirmLastModifiedDateIdx", columnList = "last_modified_date"),
        @Index(name = "FirmLastModifiedByIdx", columnList = "last_modified_by"),
        @Index(name = "FirmNameIdx", columnList = "name"),
})
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class Firm extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 255)
    private FirmType type;

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @Size(min = 1, max = 255)
    private String name;

    @OneToMany(mappedBy = "firm", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<Office> offices;

    @OneToMany(mappedBy = "firm", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaUserProfile> userProfiles;

}