package uk.gov.justice.laa.portal.landingpage.model;

import lombok.Getter;

@Getter
public enum ReactivationRequestPageMode {
    TRACK("Track reactivation requests", false),
    MANAGE("Manage reactivation requests", true);

    private final String heading;
    private final boolean manageMode;

    ReactivationRequestPageMode(String heading, boolean manageMode) {
        this.heading = heading;
        this.manageMode = manageMode;
    }
}
