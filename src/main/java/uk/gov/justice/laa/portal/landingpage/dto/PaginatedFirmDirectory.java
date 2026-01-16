package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for paginated FirmDirectory
 */
public class PaginatedFirmDirectory extends PaginatedBase implements Serializable {

    public PaginatedFirmDirectory(List<AuditUserDto> users, long totalUsers, int totalPages, int currentPage, int pageSize) {
        super(users, totalUsers, totalPages, currentPage, pageSize);
    }

}
