package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "app", indexes = {
    @Index(name = "AppNameIdx", columnList = "name"),
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED, force = true)
@ToString(doNotUseGetters = true)
public class Permission extends BaseEntity {

    @Column(name = "name", nullable = false, length = 255, unique = true)
    @NotBlank(message = "Permission name must be provided")
    @Size(min = 1, max = 255, message = "Application name must be between 1 and 255 characters")
    private String name;

    @Column(name = "entra_app_id", nullable = false, length = 255, unique = true)
    @Size(max = 255, message = "Permission function must be less than 255 characters")
    private String function;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @ToString.Exclude
    @JsonIgnore
    private List<AppRole> appRoles;

}