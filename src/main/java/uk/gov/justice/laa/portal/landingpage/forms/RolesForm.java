package uk.gov.justice.laa.portal.landingpage.forms;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class RolesForm implements Serializable {

    List<String> roles;
}
