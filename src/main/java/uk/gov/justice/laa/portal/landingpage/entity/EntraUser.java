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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Check;
import uk.gov.justice.laa.portal.landingpage.validator.EndDateAfterStartDate;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "entra_user", indexes = {
    @Index(name = "UserFirstNameIdx", columnList = "first_name"),
    @Index(name = "UserLastNameIdx", columnList = "last_name"),
    @Index(name = "UserEmailIdx", columnList = "email"),
    @Index(name = "UserNameIdx", columnList = "user_name"),
    @Index(name = "UserCreatedByIdx", columnList = "created_by"),
    @Index(name = "UserCreatedDateIdx", columnList = "created_date"),
    @Index(name = "UserLastModifiedDateIdx", columnList = "last_modified_date"),
    @Index(name = "UserLastModifiedByIdx", columnList = "last_modified_by"),
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
@EndDateAfterStartDate(message = "End date must be after start date")
@Check(name = "end_date_after_start_date", constraints = "end_date > start_date")
public class EntraUser extends AuditableEntity {

    @Column(name = "user_name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Username must be provided")
    @Size(min = 1, max = 255, message = "Username must be between 1 and 255 characters")
    private String userName;

    @Column(name = "first_name", nullable = false, length = 255)
    @NotBlank(message = "User first name must be provided")
    @Size(min = 1, max = 255, message = "User first name must be between 1 and 255 characters")
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 255)
    @NotBlank(message = "User last name must be provided")
    @Size(min = 1, max = 255, message = "User last name must be between 1 and 255 characters")
    private String lastName;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    @NotBlank(message = "User email must be provided")
    @Email(message = "User email must be a valid email address")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 255)
    @NotNull(message = "User status must be provided")
    private UserStatus userStatus;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_app_registration",
            joinColumns = @JoinColumn(name = "user_id"),
            foreignKey = @ForeignKey(name = "FK_user_app_registration_user_id"),
            inverseJoinColumns = @JoinColumn(name = "app_registration_id"),
            inverseForeignKey = @ForeignKey(name = "FK_user_app_registration_app_registration_id")
    )
    @ToString.Exclude
    @JsonIgnore
    private Set<EntraAppRegistration> userAppRegistrations;

    @OneToMany(mappedBy = "entraUser", fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private Set<LaaUserProfile> laaUserProfiles;

}