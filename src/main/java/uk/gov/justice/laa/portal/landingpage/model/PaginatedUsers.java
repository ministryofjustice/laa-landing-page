package uk.gov.justice.laa.portal.landingpage.model;

import java.util.List;

import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.dto.UserProfileDto;

@Data
public class PaginatedUsers {

    private List<UserProfileDto> users;
    private String previousPageLink;
    private String nextPageLink;
    private long totalUsers;
    private int totalPages;
}
