package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;

@Slf4j
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FirmDirectorySearchCriteria {

    // Nullable
    private String firmSearch;
    private String selectedFirmType;
    // Defaulted
    private String search = "";
    private int size = 10;
    private int page = 1;
    private String sort = "name";
    private String direction = "asc";

    public void setSelectedFirmType(String selectedFirmType) {
        if (selectedFirmType == null || selectedFirmType.isEmpty()) {
            return;
        }
        this.selectedFirmType = String.valueOf(FirmType.valueOf(selectedFirmType));

    }
}
