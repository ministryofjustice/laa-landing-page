package uk.gov.justice.laa.portal.landingpage.dto;


import lombok.Data;

import java.util.List;

@Data
@SuppressWarnings("checkstyle:MissingJavadocType")
public class OfficeData {
    private List<String> selectedOffices;
}
