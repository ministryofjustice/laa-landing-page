package uk.gov.justice.laa.portal.landingpage.model;

import java.util.List;

import lombok.Data;

/**
 * Javadoc comment.
 */
@Data
public class PaginatedUsers {

    private List<UserModel> users;
    private String previousPageLink;
    private String nextPageLink;
    private int totalUsers;
    private int totalPages;
}
