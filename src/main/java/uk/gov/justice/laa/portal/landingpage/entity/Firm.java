package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Set;

@Entity
@Table(name = "firm", indexes = {
    @Index(name = "FirmNameIdx", columnList = "name"),
    @Index(name = "FirmCodeIdx", columnList = "code"),
})
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class Firm extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, length = 255, columnDefinition = "firm_type_enum")
    @NotNull(message = "Firm type must be provided")
    private FirmType type;

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Firm name must be provided")
    @Size(min = 1, max = 255, message = "Firm name must be between 1 and 255 characters")
    private String name;

    @Column(name = "code", nullable = true, length = 255, unique = true)
    @Size(max = 255, message = "Firm code must be less than 255 characters")
    private String code;

    @OneToMany(mappedBy = "firm", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<Office> offices;

    @OneToMany(mappedBy = "firm", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<UserProfile> userProfiles;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_firm_id", foreignKey = @ForeignKey(name = "FK_parent_firm_firm_id"))
    @ToString.Exclude
    @JsonIgnore
    private Firm parentFirm;

    @OneToMany(mappedBy = "parentFirm", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<Firm> childFirms;
}
