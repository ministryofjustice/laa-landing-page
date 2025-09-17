package uk.gov.justice.laa.portal.landingpage.forms;

import java.io.Serializable;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OfficesForm implements Serializable {

    @NotNull(message = "Office selection is required")
    List<String> offices;
}
