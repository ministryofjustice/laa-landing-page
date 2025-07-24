package uk.gov.justice.laa.portal.landingpage.entity;

public enum UserProfileStatus {
    COMPLETE("COMPLETE"),
    PENDING("PENDING");

    private final String value;

    UserProfileStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
