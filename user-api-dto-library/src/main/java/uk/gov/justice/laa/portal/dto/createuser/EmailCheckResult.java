package uk.gov.justice.laa.portal.dto.createuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * CQRS Query Result: Email validation response.
 * Returned from User API to SiLAS after checking email availability and domain validity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCheckResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean available;
    private boolean validDomain;
    private boolean isMultiFirmUser;
    private String message;
}
