package uk.gov.justice.laa.portal.dto.createuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * CQRS Query: Search for firms by name.
 * Sent from SiLAS to User API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmSearchQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String searchTerm;
    private int maxResults;
}
