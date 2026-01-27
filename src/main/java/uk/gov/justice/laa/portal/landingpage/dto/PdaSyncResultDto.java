package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing the results of a PDA synchronization operation.
 */
@Data
@Builder
public class PdaSyncResultDto {

    @Builder.Default
    private int firmsCreated = 0;

    @Builder.Default
    private int firmsReactivated = 0;

    @Builder.Default
    private int firmsUpdated = 0;

    @Builder.Default
    private int firmsDeleted = 0;

    @Builder.Default
    private int officesCreated = 0;

    @Builder.Default
    private int officesReactivated = 0;

    @Builder.Default
    private int officesUpdated = 0;

    @Builder.Default
    private int officesDeleted = 0;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
