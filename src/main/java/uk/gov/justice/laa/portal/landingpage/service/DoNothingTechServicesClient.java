package uk.gov.justice.laa.portal.landingpage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.techservices.RegisterUserResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.SendUserVerificationEmailResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

import java.util.UUID;

public class DoNothingTechServicesClient implements TechServicesClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void updateRoleAssignment(UUID userId) {
        logger.info("Updating role assignment received on Dummy Tech Services Client for user {}", userId);
        // Do Nothing
    }

    @Override
    public void deleteRoleAssignment(UUID userId) {
        logger.info("Removing all role assignments on Dummy Tech Services Client for user {}", userId);
        // Do Nothing
    }

    @Override
    public TechServicesApiResponse<RegisterUserResponse> registerNewUser(EntraUserDto user) {
        logger.info("Register new user request received on Dummy Tech Services Client for user {} {}", user.getFirstName(), user.getLastName());
        // return success response with random uuid
        return TechServicesApiResponse.success(RegisterUserResponse.builder().success(true).message("Success")
                .createdUser(RegisterUserResponse.CreatedUser.builder().id(UUID.randomUUID().toString())
                        .displayName(user.getFullName()).mail(user.getEmail()).build())
                .build());
    }

    @Override
    public TechServicesApiResponse<SendUserVerificationEmailResponse> sendEmailVerification(EntraUserDto user) {
        logger.info("Verification email has been resent from Dummy Tech Services Client for user {} {}", user.getFirstName(), user.getLastName());
        // Do Nothing
        return TechServicesApiResponse.success(SendUserVerificationEmailResponse.builder().success(true)
                .message("Activation code has been generated and sent successfully via email.")
                .build());
    }

}
