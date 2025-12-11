package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import lombok.Setter;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data Transfer Object for user search criteria
 */
@Setter
@Getter
public class UserSearchCriteria implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String searchTerm;
    private FirmSearchForm firmSearch;
    private UserType userType;
    private boolean showFirmAdmins;
    private boolean showMultiFirmUsers;

    public UserSearchCriteria() {
    }

    public UserSearchCriteria(String searchTerm, FirmSearchForm firmSearch, UserType userType, boolean showFirmAdmins, boolean showMultiFirmUsers) {
        this.searchTerm = searchTerm;
        this.firmSearch = firmSearch;
        this.userType = userType;
        this.showFirmAdmins = showFirmAdmins;
        this.showMultiFirmUsers = showMultiFirmUsers;
    }

    @Override
    public String toString() {
        return "UserSearchCriteria{"
                + "searchTerm='" + searchTerm + '\''
                + ", firmSearch='" + firmSearch + '\''
                + ", userType=" + userType
                + ", showFirmAdmins=" + showFirmAdmins
                + ", showMultiFirmUsers=" + showMultiFirmUsers
                + '}';
    }
}
