package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;

@Data
public class CcmsUserDetails {

    private String uuid;
    private Long userId;
    private String userLoginId;
    private Long userPartyId;
    private String userName;
    private Long providerFirmId;
    private String providerName;
    private String notificationLov;
    private String userType;
    private String connectionResponsibilityKey;
}
