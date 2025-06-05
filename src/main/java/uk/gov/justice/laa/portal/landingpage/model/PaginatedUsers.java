package uk.gov.justice.laa.portal.landingpage.model;

import java.util.List;

import lombok.Data;

@Data
public class PaginatedUsers {

    private List<UserModel> users;
    private String previousPageLink;
    private String nextPageLink;
    private int totalUsers;

    public int getTotalPages(int size) {
        int totalPages = (int) Math.ceil((double) this.totalUsers / size);
        if (totalPages > 0) {
            return totalPages;
        } else {
            return 1;
        }
    }
}
