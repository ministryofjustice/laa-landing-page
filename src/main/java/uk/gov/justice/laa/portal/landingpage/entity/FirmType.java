package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum FirmType {

    ADVOCATE("Advocate"),
    CHAMBERS("Chambers"),
    LEGAL_SERVICES_PROVIDER("Legal Services Provider");

    private final String value;

    FirmType(String value) {
        this.value = value;
    }

}
