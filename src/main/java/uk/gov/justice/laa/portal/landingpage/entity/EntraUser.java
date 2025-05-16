package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entity class for Entra Users
 */
@Entity
@Table(name = "entra_user", indexes = {@Index(columnList = "email", name = "UserEmailIdx")})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class EntraUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "entra_user_id", nullable = false)
    @JsonIgnore
    private UUID entraUserId;

    @Column(name = "email", nullable = false, unique = true)
    @Email
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type")
    private UserType userType;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "end_date")
    private Date endDate;

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
    private Set<EntraAppRegistration> userAppRegistrations = new HashSet<>();

}