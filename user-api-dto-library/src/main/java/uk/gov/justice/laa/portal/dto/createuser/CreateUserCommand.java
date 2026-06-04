package uk.gov.justice.laa.portal.dto.createuser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * CQRS Command: Create a new external user.
 * Sent from SiLAS to User API to persist the user and register with TechServices.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserCommand implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String firstName;
    private String lastName;
    private String email;
    private boolean userManager;
    private boolean multiFirmUser;
    private UUID firmId;
    private String createdBy;
}
