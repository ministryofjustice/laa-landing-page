package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum AppRoleGroup {

    NONE(0, ""),
    PROVIDER(1, "Provider"),
    CHAMBERS(1, "Chambers"),
    ADVOCATE(1, "Advocate");

    private final int ordinal;
    private final String title;

    AppRoleGroup(int ordinal, String title) {
        this.ordinal = ordinal;
        this.title = title;
    }

}
