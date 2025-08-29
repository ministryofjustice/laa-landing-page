package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import lombok.Setter;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

import java.util.List;

/**
 * Data Transfer Object for user search criteria
 */
@Setter
@Getter
public class UserSearchCriteria {
    private String searchTerm;
    private FirmSearchForm firmSearch;
    private List<UserType> userTypes;
    private boolean showFirmAdmins;

    public UserSearchCriteria() {
    }

    public UserSearchCriteria(String searchTerm, FirmSearchForm firmSearch, List<UserType> userTypes, boolean showFirmAdmins) {
        this.searchTerm = searchTerm;
        this.firmSearch = firmSearch;
        this.userTypes = userTypes;
        this.showFirmAdmins = showFirmAdmins;
    }

    @Override
    public String toString() {
        return "UserSearchCriteria{"
                + "searchTerm='" + searchTerm + '\''
                + ", firmSearch='" + firmSearch + '\''
                + ", userTypes=" + userTypes
                + ", showFirmAdmins=" + showFirmAdmins
                + '}';
    }
}
