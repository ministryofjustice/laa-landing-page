package uk.gov.justice.laa.portal.landingpage.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

/**
 * DTO representing the results of an Entra app synchronization operation.
 */
@Data
@Builder
public class AppSyncResultDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Builder.Default
    private List<AppDto> apps = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public void addError(String error) {
        this.errors.add(error);
    }
}
