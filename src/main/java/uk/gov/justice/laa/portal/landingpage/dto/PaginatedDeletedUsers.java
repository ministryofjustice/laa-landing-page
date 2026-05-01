package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Paginated response for deleted users
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedDeletedUsers implements Serializable {

    /**
     * List of deleted users for the current page
     */
    private List<DeletedUserAuditDto> deletedUsers;

    /**
     * Total number of deleted users matching the search criteria
     */
    private long totalDeletedUsers;

    /**
     * Total number of pages
     */
    private int totalPages;

    /**
     * Current page number (1-based)
     */
    private int currentPage;

    /**
     * Page size
     */
    private int pageSize;
}

