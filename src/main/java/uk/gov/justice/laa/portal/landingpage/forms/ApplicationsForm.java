package uk.gov.justice.laa.portal.landingpage.forms;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationsForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(message = "At least one service must be selected")
    List<String> apps;
}
