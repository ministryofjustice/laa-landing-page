package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum AppGroup {
    AUTHZ(0, "Admin Services"),
    LAA(1, "Leagal aid services");

    private final int ordinal;
    private final String name;

    AppGroup(int ordinal, String name) {
        this.ordinal = ordinal;
        this.name = name;
    }

}
