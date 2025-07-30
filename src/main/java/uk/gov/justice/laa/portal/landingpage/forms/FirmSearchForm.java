package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FirmSearchForm {

    @Size(min = 2, max = 100, message = "Firm name must be between 2-100 characters")
    @NotEmpty(message = "Enter a firm name to search")
    private String firmSearch;
    
    private String selectedFirmId;
}
