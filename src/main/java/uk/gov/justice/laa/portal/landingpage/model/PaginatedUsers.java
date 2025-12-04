package uk.gov.justice.laa.portal.landingpage.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.dto.UserSearchResultsDto;

@Data
public class PaginatedUsers implements Serializable {

    private List<UserSearchResultsDto> users = new ArrayList<>();
    private String previousPageLink;
    private String nextPageLink;
    private long totalUsers;
    private int totalPages;
}
