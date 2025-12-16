package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for paginated audit user results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedAuditUsers implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * List of audit users for the current page
     */
    private List<AuditUserDto> users;

    /**
     * Total number of users across all pages
     */
    private long totalUsers;

    /**
     * Total number of pages
     */
    private int totalPages;

    /**
     * Current page number (1-based)
     */
    private int currentPage;

    /**
     * Number of users per page
     */
    private int pageSize;
}
