package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * DTO for paginated FirmDirectory
 */
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedFirmDirectory extends PaginatedBase implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private List<FirmDirectoryDto> firmDirectories;

    public PaginatedFirmDirectory(long totalUsers, int totalPages, int currentPage, int pageSize, List<FirmDirectoryDto> firmDirectories) {
        super(totalUsers, totalPages, currentPage, pageSize);
        this.firmDirectories = firmDirectories;
    }

}
