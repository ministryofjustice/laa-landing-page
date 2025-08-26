package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.List;

import uk.gov.justice.laa.portal.landingpage.entity.UserType;

/**
 * Data Transfer Object for user search criteria
 */
public class UserSearchCriteria {
    private String searchTerm;
    private String firmSearch;
    private List<UserType> userTypes;
    private boolean showFirmAdmins;

    public UserSearchCriteria() {
    }

    public UserSearchCriteria(String searchTerm, String firmSearch, List<UserType> userTypes, boolean showFirmAdmins) {
        this.searchTerm = searchTerm;
        this.firmSearch = firmSearch;
        this.userTypes = userTypes;
        this.showFirmAdmins = showFirmAdmins;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getFirmSearch() {
        return firmSearch;
    }

    public void setFirmSearch(String firmSearch) {
        this.firmSearch = firmSearch;
    }

    public List<UserType> getUserTypes() {
        return userTypes;
    }

    public void setUserTypes(List<UserType> userTypes) {
        this.userTypes = userTypes;
    }

    public boolean isShowFirmAdmins() {
        return showFirmAdmins;
    }

    public void setShowFirmAdmins(boolean showFirmAdmins) {
        this.showFirmAdmins = showFirmAdmins;
    }

    @Override
    public String toString() {
        return "UserSearchCriteria{" +
                "searchTerm='" + searchTerm + '\'' +
                ", firmSearch='" + firmSearch + '\'' +
                ", userTypes=" + userTypes +
                ", showFirmAdmins=" + showFirmAdmins +
                '}';
    }
}
