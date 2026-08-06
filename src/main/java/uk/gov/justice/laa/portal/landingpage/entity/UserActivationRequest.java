package uk.gov.justice.laa.portal.landingpage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import uk.gov.justice.laa.portal.landingpage.model.ReactivationRequestStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "user_activation_request",
        uniqueConstraints = @UniqueConstraint(
                name = "unq_request_version",
                columnNames = {"request_id", "version"}
        )
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@ToString(doNotUseGetters = true)
public class UserActivationRequest extends BaseEntity {

    @Column(name = "request_id", nullable = false)
    @NotNull(message = "User reactivation request id must be provided")
    private UUID requestId;

    @Column(name = "user_profile_id", nullable = false)
    @NotNull(message = "User reactivation request user profile id must be provided")
    private UUID userProfileId;

    @Column(name = "version", nullable = false)
    @ColumnDefault("1")
    @Builder.Default
    private Integer version = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 255)
    @NotNull(message = "User reactivation request status must be provided")
    private ReactivationRequestStatus status;

    @Column(name = "comments", nullable = false, length = 4000, columnDefinition = "TEXT")
    @NotBlank(message = "Reactivate user request comments must be provided")
    @Size(min = 1, max = 4000, message = "Reactivate user request comments must be between 1 and 4000 characters")
    private String comments;

    @Column(name = "actor_entra_oid", nullable = false)
    @NotNull(message = "User reactivation request actor entra oid must be provided")
    private String actorEntraOid;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role_type", nullable = false, length = 255)
    @NotNull(message = "Actor role type must be provided")
    private ReactivationRoleType actorRoleType;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @NotNull(message = "Created date must be provided")
    private Instant createdAt;
}
