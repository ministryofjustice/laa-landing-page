package uk.gov.justice.laa.portal.landingpage.entity;

import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.Check;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "user_profile", indexes = {
        @Index(name = "UserProfileCreatedByIdx", columnList = "created_by"),
        @Index(name = "UserProfileCreatedDateIdx", columnList = "created_date"),
        @Index(name = "UserProfileLastModifiedDateIdx", columnList = "last_modified_date"),
        @Index(name = "UserProfileLastModifiedByIdx", columnList = "last_modified_by"),
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
@Check(name = "firm_not_null_for_non_internal_users_only", constraints = "(firm_id IS NULL AND user_type = 'INTERNAL') OR (firm_id IS NOT NULL AND user_type != 'INTERNAL')")
public class UserProfile extends AuditableEntity {

    @Column(name = "active_profile", nullable = false)
    private boolean activeProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 255)
    @NotNull(message = "User type must be provided")
    private UserType userType;

    @Column(name = "legacy_user_id", nullable = true)
    private UUID legacyUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entra_user_id", foreignKey = @ForeignKey(name = "FK_user_profile_user_id"))
    @ToString.Exclude
    @JsonIgnore
    private EntraUser entraUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firm_id", foreignKey = @ForeignKey(name = "FK_user_profile_firm_id"))
    @ToString.Exclude
    @JsonIgnore
    private Firm firm;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_profile_office", joinColumns = @JoinColumn(name = "user_profile_id"), foreignKey = @ForeignKey(name = "FK_user_profile_office_user_profile_id"), inverseJoinColumns = @JoinColumn(name = "office_id"), inverseForeignKey = @ForeignKey(name = "FK_user_profile_office_office_id"))
    @ToString.Exclude
    @JsonIgnore
    private Set<Office> offices;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_profile_app_role", joinColumns = @JoinColumn(name = "user_profile_id"), foreignKey = @ForeignKey(name = "FK_user_profile_app_role_user_profile_id"), inverseJoinColumns = @JoinColumn(name = "app_role_id"), inverseForeignKey = @ForeignKey(name = "FK_user_profile_app_role_app_role_id"))
    @ToString.Exclude
    @JsonIgnore
    private Set<AppRole> appRoles;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 255)
    @NotNull(message = "User profile status must be provided")
    private UserProfileStatus userProfileStatus;

    @Column(name = "last_sync_successful", nullable = false)
    private boolean lastCcmsSyncSuccessful;

    @PrePersist
    public void prePersist() {
        if (legacyUserId == null) {
            legacyUserId = UUID.randomUUID();
        }
    }

}