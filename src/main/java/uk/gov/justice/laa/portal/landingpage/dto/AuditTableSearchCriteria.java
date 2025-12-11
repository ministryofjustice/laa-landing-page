package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

import java.util.UUID;

@Slf4j
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class AuditTableSearchCriteria {

    // Nullable
    private String firmSearch;
    private UUID selectedFirmId;
    private String silasRole;
    private UUID selectedAppId;
    private UserType selectedUserType;
    private boolean multiFirm;
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

    public void setSelectedAppId(String selectedAppId) {
        if (selectedAppId != null && !selectedAppId.isBlank()) {
            try {
                this.selectedAppId = UUID.fromString(selectedAppId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid app ID format: {}", selectedAppId);
            }
        }
    }

    public void setSelectedUserType(String selectedUserType) {
        if (selectedUserType == null || selectedUserType.isEmpty()) {
            return;
        }
        if ("MULTI_FIRM".equals(selectedUserType)) {
            multiFirm = true;
            return;
        }
        try {
            this.selectedUserType = UserType.valueOf(selectedUserType);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid user type provided: {}", selectedUserType);
        }
    }
}
