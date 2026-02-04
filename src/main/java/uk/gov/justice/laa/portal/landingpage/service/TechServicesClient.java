package uk.gov.justice.laa.portal.landingpage.service;

import java.util.UUID;

import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.techservices.ChangeAccountEnabledResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUsersResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

public interface TechServicesClient {

    void updateRoleAssignment(UUID userId);

    void deleteRoleAssignment(UUID userId);

    TechServicesApiResponse<RegisterUserResponse> registerNewUser(EntraUserDto user);

    TechServicesApiResponse<SendUserVerificationEmailResponse> sendEmailVerification(EntraUserDto user);

    TechServicesApiResponse<ChangeAccountEnabledResponse> disableUser(EntraUserDto user, String reason);

    TechServicesApiResponse<ChangeAccountEnabledResponse> enableUser(EntraUserDto user);

    TechServicesApiResponse<GetUsersResponse> getUsers(String fromDateTime, String toDateTime);

}
