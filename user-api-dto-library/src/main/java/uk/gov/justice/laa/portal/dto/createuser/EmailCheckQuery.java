package uk.gov.justice.laa.portal.dto.createuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * CQRS Query: Check if an email is available for user creation.
 * Sent from SiLAS to User API to validate email uniqueness and domain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailCheckQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String email;
}
