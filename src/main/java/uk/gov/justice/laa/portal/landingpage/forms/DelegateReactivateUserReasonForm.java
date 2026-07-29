package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class DelegateReactivateUserReasonForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "A reason must be provided")
    @Size(min = 10, max = 500, message = "Reason must be between 10 and 500 characters")
    String reason;
}
