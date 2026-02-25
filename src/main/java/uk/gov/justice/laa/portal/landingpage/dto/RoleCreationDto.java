package uk.gov.justice.laa.portal.landingpage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleCreationDto {

    @NotBlank(message = "Role name is required")
    @Size(min = 1, max = 255, message = "Role name must be between 1 and 255 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

    @Size(max = 30, message = "CCMS code must not exceed 30 characters")
    private String ccmsCode;

    @NotNull(message = "Parent app is required")
    private UUID parentAppId;

    @NotEmpty(message = "At least one user type must be selected")
    private List<UserType> userTypeRestriction;

    private List<FirmType> firmTypeRestriction;

    @NotNull(message = "Legacy sync selection is required")
    private Boolean legacySync;

    private UUID id;
    private boolean authzRole;
    private int ordinal;
    private String parentAppName;
}
