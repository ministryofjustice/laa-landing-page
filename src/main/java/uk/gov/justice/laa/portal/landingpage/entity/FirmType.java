package uk.gov.justice.laa.portal.landingpage.entity;

import lombok.Getter;

@Getter
public enum FirmType {

    ADVOCATE("Advocate"),
    CHAMBERS("Chambers"),
    INDIVIDUAL("Individual"),
    LEGAL_SERVICES_PROVIDER("Legal Services Provider"),
    QM_PROVIDER("QM Provider"),
    PARTNERSHIP("Partnership"),
    LIMITED_COMPANY("Limited Company"),
    LLP("LLP"),
    SOLE_PRACTITIONER("Sole Practitioner"),
    CHARITY("Charity"),
    GOVERNMENT_FUNDED_ORGANISATION("Government Funded Organisation"),
    NA("N/A");

    private final String value;

    FirmType(String value) {
        this.value = value;
    }

}
