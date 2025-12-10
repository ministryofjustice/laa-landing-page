package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum FirmType {

    LEGAL_SERVICES_PROVIDER(1, "Provider"),
    CHAMBERS(2, "Chambers"),
    ADVOCATE(3, "Advocate");

    private final int ordinal;
    private final String value;

    FirmType(int ordinal, String value) {
        this.ordinal = ordinal;
        this.value = value;
    }

}
