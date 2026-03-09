package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * DTO for AdminApp administration display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAppDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String description;
    private int ordinal;
    private boolean enabled;
}
