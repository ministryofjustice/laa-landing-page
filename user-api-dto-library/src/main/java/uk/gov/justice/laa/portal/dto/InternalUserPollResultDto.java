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
public class InternalUserPollResultDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private String entraOid;
}
