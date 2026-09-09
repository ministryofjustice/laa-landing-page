package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum UserAccountStatus {
    ACTIVATION_REQUIRED(1, "Activation required"),
    ACTIVE(2, "Active"),
    DEACTIVATED(3, "Deactivated"),
    DELETED(4, "Deleted"),
    UNKNOWN(99, "Unknown");

    private final int sortOrder;
    private final String value;

    UserAccountStatus(int sortOrder, String value) {
        this.sortOrder = sortOrder;
        this.value = value;
    }

}
