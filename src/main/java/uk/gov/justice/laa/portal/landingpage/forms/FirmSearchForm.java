package uk.gov.justice.laa.portal.landingpage.forms;

import java.io.Serializable;
import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmSearchForm implements Serializable {

    private String firmSearch;

    private UUID selectedFirmId;

    private boolean skipFirmSelection;

    public FirmSearchForm(String firmSearch, UUID selectedFirmId) {
        this.firmSearch = firmSearch;
        this.selectedFirmId = selectedFirmId;
    }

    public void setFirmSearch(String firmSearch) {
        if (firmSearch != null) {
            this.firmSearch = firmSearch.trim();
        }
    }

    @AssertTrue(message = "Please make a valid firm selection")
    public boolean isValid() {
        return skipFirmSelection ^ (firmSearch != null && !firmSearch.isBlank());
    }
}
