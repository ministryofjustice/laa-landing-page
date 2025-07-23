package uk.gov.justice.laa.portal.landingpage.service;

import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;

import java.util.UUID;

public interface TechServicesClient {

    void updateRoleAssignment(UUID userId);

    RegisterUserResponse registerNewUser(EntraUserDto user);

}
