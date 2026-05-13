package uk.gov.justice.laa.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserPollRequestDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String entraOid;
    private String email;
    private String firstName;
    private String lastName;
}
