package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum SilasAccountStatus {
    ACTIVE("Active"),
    DEACTIVATED("Deactivated"),
    ACTIVATION_REQUIRED("Activation required"),
    UNKNOWN("Unknown");

    private final String value;

    SilasAccountStatus(String value) {
        this.value = value;
    }

}
