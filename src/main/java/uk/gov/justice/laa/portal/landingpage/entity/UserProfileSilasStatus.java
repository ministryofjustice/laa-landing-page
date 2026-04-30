package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum UserProfileSilasStatus {
    INCOMPLETE(1, "Incomplete"),
    ACTIVATION_PENDING(2, "Activation pending"),
    DISABLED(3, "Disabled"),
    NO_ROLES_ASSIGNED(4, "No roles assigned"),
    COMPLETE(5, "Complete"),
    UNKNOWN(6, "Unknown");

    private final int sortOrder;
    private final String value;

    UserProfileSilasStatus(int sortOrder, String value) {
        this.sortOrder = sortOrder;
        this.value = value;
    }

}
