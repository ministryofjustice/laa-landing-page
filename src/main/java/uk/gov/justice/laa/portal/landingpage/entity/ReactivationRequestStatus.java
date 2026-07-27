package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum ReactivationRequestStatus {
    IN_REVIEW,
    INFORMATION_REQUIRED,
    REJECTED,
    APPROVED
}
