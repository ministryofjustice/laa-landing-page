package uk.gov.justice.laa.portal.landingpage.entity;

import java.util.Arrays;

public enum UserType {
    INTERNAL,
    EXTERNAL_SINGLE_FIRM_ADMIN,
    EXTERNAL_SINGLE_FIRM,
    EXTERNAL_MULTI_FIRM;

    public static final String[]  ADMIN_TYPES = new String[]{INTERNAL.name(), EXTERNAL_SINGLE_FIRM_ADMIN.name()};

    public boolean isAdmin() {
        return Arrays.stream(ADMIN_TYPES).anyMatch(type -> type.equals(this.name()));
    }
}
