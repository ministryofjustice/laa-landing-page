package uk.gov.justice.laa.portal.landingpage.forms;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationsForm {

    @NotNull(message = "At least one application must be selected")
    List<String> apps;
}
