package uk.gov.justice.laa.portal.landingpage.dto;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class OfficeData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<String> selectedOffices;
    private List<String> selectedOfficesDisplay;
}
