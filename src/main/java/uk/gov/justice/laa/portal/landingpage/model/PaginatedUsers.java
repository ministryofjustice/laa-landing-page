package uk.gov.justice.laa.portal.landingpage.model;

import java.util.List;

import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;

@Data
public class PaginatedUsers {

    private List<EntraUserDto> users;
    private String previousPageLink;
    private String nextPageLink;
    private long totalUsers;
    private int totalPages;
}
