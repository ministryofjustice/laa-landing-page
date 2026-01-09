package uk.gov.justice.laa.portal.landingpage.utils;

import lombok.Getter;

@Getter
public enum UserRoleType {
    EXTERNAL_USER_ADMIN("External User Admin"),
    FIRM_USER_MANAGER("Firm User Manager"),
    GLOBAL_ADMIN("Global Admin");

    private final String description;


    UserRoleType(String description) {
        this.description = description;
    }

}
