package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import lombok.Setter;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfileSilasStatus;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private boolean showProviderUsers;
    private List<UserProfileSilasStatus> selectedStatuses = new ArrayList<>();

    public UserSearchCriteria() {
    }

    public UserSearchCriteria(String searchTerm, FirmSearchForm firmSearch, UserType userType,
                              boolean showFirmAdmins, boolean showMultiFirmUsers) {
        this.searchTerm = searchTerm;
        this.firmSearch = firmSearch;
        this.userType = userType;
        this.showFirmAdmins = showFirmAdmins;
        this.showMultiFirmUsers = showMultiFirmUsers;
    }

    public UserSearchCriteria(String searchTerm, FirmSearchForm firmSearch, UserType userType,
                              boolean showFirmAdmins, boolean showMultiFirmUsers,
                              boolean showProviderUsers, List<UserProfileSilasStatus> selectedStatuses) {
        this.searchTerm = searchTerm;
        this.firmSearch = firmSearch;
        this.userType = userType;
        this.showFirmAdmins = showFirmAdmins;
        this.showMultiFirmUsers = showMultiFirmUsers;
        this.showProviderUsers = showProviderUsers;
        this.selectedStatuses = selectedStatuses != null ? selectedStatuses : new ArrayList<>();
    }

    public boolean hasSelectedStatuses() {
        return selectedStatuses != null && !selectedStatuses.isEmpty();
    }

    @Override
    public String toString() {
        return "UserSearchCriteria{"
                + "searchTerm='" + searchTerm + '\''
                + ", firmSearch='" + firmSearch + '\''
                + ", userType=" + userType
                + ", showFirmAdmins=" + showFirmAdmins
                + ", showMultiFirmUsers=" + showMultiFirmUsers
                + ", showProviderUsers=" + showProviderUsers
                + ", selectedStatuses=" + selectedStatuses
                + '}';
    }
}
