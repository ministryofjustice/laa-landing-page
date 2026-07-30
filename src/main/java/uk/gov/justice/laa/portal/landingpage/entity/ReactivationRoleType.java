package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum ReactivationRoleType {

    NONE("None"),

    PROVIDER_ADMIN("Provider Admin"),

    LAA("Legal Aid Agency");

    private final String displayName;

    ReactivationRoleType(String displayName) {
        this.displayName = displayName;
    }

}
