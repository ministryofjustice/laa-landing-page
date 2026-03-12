package uk.gov.justice.laa.portal.landingpage.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum InvitationStatus {
    INVITE_SENT("InviteSent"),
    AWAITING_MFA("AwaitingMFA"),
    AWAITING_VERIFICATION("AwaitingVerification"),
    VERIFICATION_SUCCESS("VerificationSuccess"),
    VERIFICATION_FAILED("VerificationFailed");

    private final String value;

    InvitationStatus(String value) {
        this.value = value;
    }

    @JsonCreator
    public static InvitationStatus fromValue(String value) {
        for (InvitationStatus status : InvitationStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown InvitationStatus value: " + value);
    }

    @Override
    public String toString() {
        return value;
    }
}
