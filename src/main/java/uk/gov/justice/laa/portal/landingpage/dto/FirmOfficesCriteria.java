package uk.gov.justice.laa.portal.landingpage.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class FirmOfficesCriteria {

    private String officeCode;
    private String officeAddress;
    private boolean officeStatus;

    private int size = 10;
    private int page = 1;
    private String sort = "code";
    private String direction = "asc";
}
