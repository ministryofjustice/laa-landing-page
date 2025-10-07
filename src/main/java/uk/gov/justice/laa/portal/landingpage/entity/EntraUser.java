package uk.gov.justice.laa.portal.landingpage.entity;

import java.util.Set;

import org.hibernate.annotations.ColumnDefault;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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

@Entity
@Table(name = "entra_user", indexes = {
    @Index(name = "UserFirstNameIdx", columnList = "first_name"),
    @Index(name = "UserLastNameIdx", columnList = "last_name"),
    @Index(name = "UserEmailIdx", columnList = "email"),
    @Index(name = "UserEntraOidIdx", columnList = "entra_oid"),
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
public class EntraUser extends AuditableEntity {

    @Column(name = "entra_oid", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Entra Object ID must be provided")
    @Size(min = 1, max = 255, message = "Entra Object ID must be between 1 and 255 characters")
    private String entraOid;

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
    @ColumnDefault("AWAITING_APPROVAL")
    private UserStatus userStatus;

    @Column(name = "multi_firm_user", nullable = false)
    @ColumnDefault("false")
    private boolean isMultiFirmUser;

    @OneToMany(mappedBy = "entraUser", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @ToString.Exclude
    @JsonIgnore
    private Set<UserProfile> userProfiles;

}