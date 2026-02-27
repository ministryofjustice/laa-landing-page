package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@SuperBuilder
@Table(name = "user_account_status_audit")
@ToString(doNotUseGetters = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
public class UserAccountStatusAudit extends BaseEntity {

    @Column(name = "status_changed_date", nullable = false)
    @CreatedDate
    @NotNull(message = "Status changed date must be provided")
    private LocalDateTime statusChangedDate;

    @Column(name = "status_changed_by", nullable = false, length = 255)
    @NotBlank(message = "Status changed by must be provided")
    @Size(min = 1, max = 255, message = "Disabled by must be between 1 and 255 characters")
    @CreatedBy
    private String statusChangedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_change", nullable = false, length = 255)
    @NotNull(message = "Status change must be provided")
    private UserAccountStatus statusChange;

    @ManyToOne
    @JoinColumn(name = "entra_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_disable_user_audit_entra_user_id"))
    @ToString.Exclude
    @JsonIgnore
    private EntraUser entraUser;

    @ManyToOne
    @JoinColumn(name = "disable_user_reason_id", nullable = true, foreignKey = @ForeignKey(name = "fk_disable_user_audit_disable_user_reason_id"))
    @ToString.Exclude
    @JsonIgnore
    private DisableUserReason disableUserReason;
}
