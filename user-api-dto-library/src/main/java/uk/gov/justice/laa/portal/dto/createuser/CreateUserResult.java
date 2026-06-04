package uk.gov.justice.laa.portal.dto.createuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * CQRS Command Result: Response after creating a user.
 * Returned from User API to SiLAS after successful creation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private String entraOid;
    private UUID userProfileId;
    private String email;
    private String fullName;
    private boolean multiFirmUser;
}
