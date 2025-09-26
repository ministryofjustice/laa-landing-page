package uk.gov.justice.laa.portal.landingpage.dto;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class OfficeData implements Serializable {
    private List<String> selectedOffices;
    private List<String> selectedOfficesDisplay;
}
