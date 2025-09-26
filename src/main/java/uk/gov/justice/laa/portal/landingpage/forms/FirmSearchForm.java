package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class FirmSearchForm implements Serializable {

    @NotBlank(message = "Enter a firm name to search")
    private String firmSearch;

    private UUID selectedFirmId;

    public void setFirmSearch(String firmSearch) {
        if (firmSearch != null) {
            this.firmSearch = firmSearch.trim();
        }
    }
}
