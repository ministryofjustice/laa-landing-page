package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Form object for capturing reassignment reason.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReassignmentReasonForm {

    @NotBlank(message = "Please provide a reason for the reassignment")
    private String reason;
}
