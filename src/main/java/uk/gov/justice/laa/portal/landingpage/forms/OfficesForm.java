package uk.gov.justice.laa.portal.landingpage.forms;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OfficesForm {

    @NotNull(message = "Office selection is required")
    List<String> offices;
}
