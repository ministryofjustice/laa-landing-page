package uk.gov.justice.laa.portal.landingpage.forms;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RolesForm {

    @NotNull(message = "At least one role must be selected")
    List<String> roles;
}
