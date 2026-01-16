package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Getter;
import lombok.Setter;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.forms.FirmSearchForm;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data Transfer Object for user search criteria
 */
@Setter
@Getter
public class FirmSearchCriteria implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String searchTerm;
    private FirmSearchForm firmSearch;
    private String firmType;

    public FirmSearchCriteria(String searchTerm, FirmSearchForm firmSearch, String firmType) {
        this.searchTerm = searchTerm;
        this.firmSearch = firmSearch;
        this.firmType = firmType;
    }

    @Override
    public String toString() {
        return "FirmSearchCriteria{" +
                "searchTerm='" + searchTerm + '\'' +
                ", firmSearch=" + firmSearch +
                ", firmType=" + firmType +
                '}';
    }
}
