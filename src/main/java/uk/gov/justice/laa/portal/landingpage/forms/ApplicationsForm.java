package uk.gov.justice.laa.portal.landingpage.forms;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ApplicationsForm {
    @NotEmpty(message = "At least one service must be selected")
    List<String> apps;
}
