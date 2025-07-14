package uk.gov.justice.laa.portal.landingpage.entity;

import java.util.Arrays;
import java.util.List;

public enum UserType {
    INTERNAL,
    EXTERNAL_SINGLE_FIRM_ADMIN,
    EXTERNAL_SINGLE_FIRM,
    EXTERNAL_MULTI_FIRM;

    public static final String[]  ADMIN_TYPES = new String[]{INTERNAL.name(), EXTERNAL_SINGLE_FIRM_ADMIN.name()};
    public static final String[]  USER_CREATION_TYPES = new String[]{INTERNAL.name()};
    // Adding a static list of all types to save on unnecessary memory churn.
    public static final List<UserType> ALL_USER_TYPES = Arrays.asList(values());

    public static final List<UserType> INTERNAL_TYPES = List.of(UserType.INTERNAL);
    public static final List<UserType> EXTERNAL_TYPES = List.of(UserType.EXTERNAL_SINGLE_FIRM, UserType.EXTERNAL_SINGLE_FIRM_ADMIN, UserType.EXTERNAL_MULTI_FIRM);

    public boolean isAdmin() {
        return Arrays.stream(ADMIN_TYPES).anyMatch(type -> type.equals(this.name()));
    }

    public boolean isAllowedToCreateUsers() {
        return Arrays.stream(USER_CREATION_TYPES).anyMatch(type -> type.equals(this.name()));
    }
}
