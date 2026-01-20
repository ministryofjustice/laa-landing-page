package uk.gov.justice.laa.portal.landingpage.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for comparison result between PDA and database.
 */
@Data
@Builder
public class ComparisonResultDto {
    @Builder.Default
    private List<ItemInfo> created = new ArrayList<>();

    @Builder.Default
    private List<ItemInfo> updated = new ArrayList<>();

    @Builder.Default
    private List<ItemInfo> deleted = new ArrayList<>();

    @Builder.Default
    private List<ItemInfo> exists = new ArrayList<>();

    // Separate counts for firms and offices
    private int firmsCreated;
    private int firmsUpdated;
    private int firmsDeleted;
    private int firmsExists;

    private int officesCreated;
    private int officesUpdated;
    private int officesDeleted;
    private int officesExists;

    /**
     * Minimal info for each item (firm or office).
     */
    @Data
    @Builder
    public static class ItemInfo {
        private String type;  // "firm" or "office"
        private String code;  // firmNumber or officeAccountNo
        private String name;  // firmName or office address
        private UUID dbId;    // database ID if exists
    }
}
