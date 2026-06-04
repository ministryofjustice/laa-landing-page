package uk.gov.justice.laa.portal.dto.createuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * Lightweight firm DTO used in CQRS query responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmSummaryDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String name;
    private String code;
}
