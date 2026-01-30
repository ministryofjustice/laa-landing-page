package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DisableUserReasonForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "A reason must be selected")
    String reasonId;
}
