package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Data;

@Data
public class EntraClaimData {
    private EntraAuthenticationContext authenticationContext;
    private EntraUserInfo user;
    private EntraApplicationInfo application;
}
