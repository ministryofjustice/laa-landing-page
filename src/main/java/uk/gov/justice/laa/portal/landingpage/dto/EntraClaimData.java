package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class EntraClaimData {
    private EntraAuthenticationContext authenticationContext;
    private EntraUserInfo user;
    private EntraApplicationInfo application;
}
