package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import uk.gov.justice.laa.portal.landingpage.validator.EndDateAfterStartDate;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "entra_user", indexes = {
    @Index(name = "EntraUserFirstNameIdx", columnList = "first_name"),
    @Index(name = "EntraUserLastNameIdx", columnList = "last_name"),
    @Index(name = "EntraUserEmailIdx", columnList = "email"),
    @Index(name = "EntraUserNameIdx", columnList = "user_name"),
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
@EndDateAfterStartDate(message = "End date must be after start date")
public class EntraUser extends AuditableEntity {

    @Column(name = "user_name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Entra username must be provided")
    @Size(min = 1, max = 255, message = "Entra username must be between 1 and 255 characters")
    private String userName;

    @Column(name = "first_name", nullable = false, length = 255)
    @NotBlank(message = "Entra user first name must be provided")
    @Size(min = 1, max = 255, message = "Entra user first name must be between 1 and 255 characters")
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    @NotBlank(message = "Entra user last name must be provided")
    @Size(min = 1, max = 255, message = "Entra user last name must be between 1 and 255 characters")
    private String lastName;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Entra user email must be provided")
    @Email(message = "Entra user email must be a valid email address")
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "invitation_accepted", nullable = false)
    private boolean invitationAccepted;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "entra_user_app_registration",
            joinColumns = @JoinColumn(name = "entra_user_id"),
            foreignKey = @ForeignKey(name = "FK_entra_user_app_registration_entra_user_id"),
            inverseJoinColumns = @JoinColumn(name = "entra_app_registration_id"),
            inverseForeignKey = @ForeignKey(name = "FK_entra_user_app_registration_entra_app_registration_id")
    )
    @ToString.Exclude
    @JsonIgnore
    private Set<EntraAppRegistration> userAppRegistrations;

    @OneToMany(mappedBy = "entraUser", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaUserProfile> laaUserProfiles;

}