package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Entity
@Table(name = "office", indexes = {
    @Index(name = "OfficeCodeIdx", columnList = "code"),
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class Office extends BaseEntity {

    @Column(name = "code", nullable = true, length = 255, unique = true)
    @Size(max = 255, message = "Office code must be less than 255 characters")
    private String code;

    @Embedded
    @Valid
    private Address address;

    @ManyToOne
    @JoinColumn(name = "firm_id", nullable = false, foreignKey = @ForeignKey(name = "FK_office_firm_id"))
    @ToString.Exclude
    @JsonIgnore
    private Firm firm;

    @ManyToMany(mappedBy = "offices", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<UserProfile> userProfiles;

    @Embeddable
    @NoArgsConstructor
    @SuperBuilder
    @Getter
    @Setter
    public static class Address {

        @Column(name = "address_line_1", nullable = true, length = 255)
        @Size(min = 1, max = 255, message = "Office address must be between 1 and 255 characters")
        private String addressLine1;

        @Column(name = "address_line_2", nullable = true, length = 255)
        @Size(min = 1, max = 255, message = "Office address line 2 must be between 1 and 255 characters")
        private String addressLine2;

        @Column(name = "address_line_3", nullable = true, length = 255)
        @Size(min = 1, max = 255, message = "Office address line 3 must be between 1 and 255 characters")
        private String addressLine3;

        @Column(name = "city", nullable = true, length = 255)
        @Size(min = 1, max = 255, message = "Office city must be between 1 and 255 characters")
        private String city;

        @Column(name = "post_code", nullable = true, length = 20)
        @Size(max = 20, message = "Office postcode must be between 2 and 20 characters")
        private String postcode;

    }

}
