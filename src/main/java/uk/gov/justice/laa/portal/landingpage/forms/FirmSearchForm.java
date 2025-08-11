package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FirmSearchForm {

    @NotBlank(message = "Enter a firm name to search")
    private String firmSearch;

    private String selectedFirmId;

    public void setFirmSearch(String firmSearch) {
        if(firmSearch != null) {
            this.firmSearch = firmSearch.trim();
        }
    }
}
