package uk.gov.justice.laa.portal.landingpage.forms;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RolesForm implements java.io.Serializable{

    List<String> roles;
}
