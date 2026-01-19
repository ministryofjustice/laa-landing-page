package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.forms.UserTypeForm;

import java.util.UUID;

@Slf4j
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FirmDirectorySearchCriteria {

    // Nullable
    private String firmSearch;
    private UUID selectedFirmId;
    private String selectedFirmType;
    // Defaulted
    private String search = "";
    private int size = 10;
    private int page = 1;
    private String sort = "name";
    private String direction = "asc";


    /*
        An IDE may give the impression the below setters are unused because they aren't explicitly called in the app code.
        However, they are used by Spring under the hood when binding the audit table request so do not remove.
    */

    public void setSelectedFirmId(String selectedFirmId) {
        if (selectedFirmId != null && !selectedFirmId.isBlank()) {
            try {
                this.selectedFirmId = UUID.fromString(selectedFirmId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid firm ID format: {}", selectedFirmId);
            }
        }
    }

    public void setSelectedFirmType(String selectedFirmType) {
        if (selectedFirmType == null || selectedFirmType.isEmpty()) {
            return;
        }
        try {
            this.selectedFirmType = selectedFirmType;
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid Firm type provided: {}", selectedFirmType);
        }
    }
}
