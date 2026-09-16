package uk.gov.justice.laa.portal.landingpage.model;

import lombok.Getter;

@Getter
public enum ReactivationRequestStatus {
    IN_REVIEW("In review"),
    INFORMATION_REQUIRED("Information required"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String label;

    ReactivationRequestStatus(String label) {
        this.label = label;
    }
}
