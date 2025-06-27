package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class EntraClaimData {
    private EntraAuthenticationContext authenticationContext;
    private EntraUserInfo user;
    private EntraApplicationInfo application;
}
