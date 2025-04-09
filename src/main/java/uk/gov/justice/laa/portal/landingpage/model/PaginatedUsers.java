package uk.gov.justice.laa.portal.landingpage.model;

import lombok.Data;

import java.util.List;


/**
 * Javadoc comment.
 */
@Data
public class PaginatedUsers {
    private List<UserModel> users;
    private String previousPageLink;
    private String nextPageLink;
    private int totalUsers;
}