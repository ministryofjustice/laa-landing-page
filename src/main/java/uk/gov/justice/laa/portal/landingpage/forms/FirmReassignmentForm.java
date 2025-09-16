package uk.gov.justice.laa.portal.landingpage.forms;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Form object for capturing firm reassignment details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirmReassignmentForm {

    @NotBlank(message = "Please select a firm")
    private String firmSearch;

    @NotNull(message = "Please select a firm")
    private UUID selectedFirmId;

    @NotBlank(message = "Please provide a reason for the reassignment")
    private String reason;
}
