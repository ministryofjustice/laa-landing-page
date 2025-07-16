package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum UserType {
    INTERNAL("Internal User"), // Placeholder friendly name.
    EXTERNAL_SINGLE_FIRM_ADMIN("Provider Admin"),
    EXTERNAL_SINGLE_FIRM("Provider User"),
    EXTERNAL_MULTI_FIRM("Provider Multi-firm User"); // Placeholder friendly name.

    public static final String[]  ADMIN_TYPES = new String[]{INTERNAL.name(), EXTERNAL_SINGLE_FIRM_ADMIN.name()};
    // Adding a static list of all types to save on unnecessary memory churn.
    public static final List<UserType> ALL_USER_TYPES = Arrays.asList(values());

    public static final List<UserType> INTERNAL_TYPES = List.of(UserType.INTERNAL);
    public static final List<UserType> EXTERNAL_TYPES = List.of(UserType.EXTERNAL_SINGLE_FIRM, UserType.EXTERNAL_SINGLE_FIRM_ADMIN, UserType.EXTERNAL_MULTI_FIRM);

    /** Used in the UI dropdown. */
    private final String friendlyName;

    UserType(String friendlyName) {
        this.friendlyName = friendlyName;
    }
    public boolean isAdmin() {
        return Arrays.stream(ADMIN_TYPES).anyMatch(type -> type.equals(this.name()));
    }
}
