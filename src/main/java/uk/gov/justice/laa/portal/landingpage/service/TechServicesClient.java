package uk.gov.justice.laa.portal.landingpage.service;

import java.util.UUID;

import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;

public interface TechServicesClient {

    void updateRoleAssignment(UUID userId);

    RegisterUserResponse registerNewUser(EntraUserDto user);

}
