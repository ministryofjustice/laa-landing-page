package uk.gov.justice.laa.portal.landingpage.model;

import lombok.Getter;

@Getter
public enum ReactivationRequestUserType {
    PROVIDER_USER("Provider User", "Provider"),
    PROVIDER_ADMIN("Provider Admin", "Provider Admin"),
    THIRD_PARTY("3rd Party", "3rd Party");

    private final String filterLabel;
    private final String tableLabel;

    ReactivationRequestUserType(String filterLabel, String tableLabel) {
        this.filterLabel = filterLabel;
        this.tableLabel = tableLabel;
    }
}
