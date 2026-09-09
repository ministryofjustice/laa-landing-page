package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum UserProfileSilasStatus {
    ACTIVATION_REQUIRED(1, "Activation required"),
    NO_ACCESS_ASSIGNED(2, "No access assigned"),
    NO_FIRMS_LINKED(3, "No firms linked"),
    COMPLETE(4, "Complete"),
    UNKNOWN(5, "Unknown");

    private final int sortOrder;
    private final String value;

    UserProfileSilasStatus(int sortOrder, String value) {
        this.sortOrder = sortOrder;
        this.value = value;
    }

}
