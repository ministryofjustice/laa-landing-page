package uk.gov.justice.laa.portal.landingpage.forms;

import java.io.Serializable;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
