package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum AppType {
    AUTHZ(0, "Admin services"),
    LAA(1, "Legal aid Services");

    private final int ordinal;
    private final String name;

    AppType(int ordinal, String name) {
        this.ordinal = ordinal;
        this.name = name;
    }

}
