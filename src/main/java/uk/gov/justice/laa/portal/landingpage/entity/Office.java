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
@Table(name = "office", indexes = {
    @Index(name = "OfficeNameIdx", columnList = "name"),
    @Index(name = "OfficeCodeIdx", columnList = "code"),
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class Office extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Office name must be provided")
    @Size(min = 1, max = 255, message = "Office name must be between 1 and 255 characters")
    private String name;

    @Column(name = "code", nullable = true, length = 255, unique = true)
    @Size(max = 255, message = "Office code must be less than 255 characters")
    private String code;

    @Column(name = "address", nullable = false, length = 500)
    @NotBlank(message = "Office address must be provided")
    @Size(min = 1, max = 500, message = "Office address must be between 1 and 500 characters")
    private String address;

    @Column(name = "phone", nullable = false, length = 255)
    @NotBlank(message = "Office phone number must be provided")
    @Size(min = 1, max = 255, message = "Office phone number must be between 1 and 255 characters")
    private String phone;

    @ManyToOne
    @JoinColumn(name = "firm_id", nullable = false, foreignKey = @ForeignKey(name = "FK_office_firm_id"))
    @ToString.Exclude
    @JsonIgnore
    private Firm firm;

    @ManyToMany(mappedBy = "offices", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<UserProfile> userProfiles;

}