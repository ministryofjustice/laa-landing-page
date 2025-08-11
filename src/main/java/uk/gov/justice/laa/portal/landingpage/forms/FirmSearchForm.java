package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FirmSearchForm {

    @Size(min = 1, message = "Enter a firm name to search")
    private String firmSearch;

    private String selectedFirmId;
}
