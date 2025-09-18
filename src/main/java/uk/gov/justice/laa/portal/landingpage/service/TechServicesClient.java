package uk.gov.justice.laa.portal.landingpage.service;

import java.util.UUID;

import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

public interface TechServicesClient {

    void updateRoleAssignment(UUID userId);

    TechServicesApiResponse<RegisterUserResponse> registerNewUser(EntraUserDto user);

}
