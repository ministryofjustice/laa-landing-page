package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Form object for capturing app role deletion reason.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAppRoleReasonForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Please provide a reason for the role deletion")
    @Size(min = 10, max = 1000, message = "Reason must be between 10 and 1000 characters")
    private String reason;

    @NotBlank(message = "Please provide the app name")
    private String appName;

    @NotBlank(message = "Please provide the app role id")
    private String appRoleId;
}
