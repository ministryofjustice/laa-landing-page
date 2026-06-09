package uk.gov.justice.laa.portal.silas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserCommand;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserResult;
import uk.gov.justice.laa.portal.dto.createuser.EmailCheckResult;
import uk.gov.justice.laa.portal.dto.createuser.FirmSummaryDto;
import uk.gov.justice.laa.portal.silas.client.OpaClient;
import uk.gov.justice.laa.portal.silas.client.UserApiClient;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Business logic orchestrator for the create user flow (CQRS pattern).
 *
 * Responsibilities:
 * - Checks authorization via OPA
 * - Validates input using User API query endpoints (read side)
 * - Issues create command to User API (write side)
 *
 * Does NOT directly access the database or TechServices.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserService {

    private final UserApiClient userApiClient;
    private final OpaClient opaClient;

    /**
     * Step 1: Check if the actor is authorized to create a user (OPA policy).
     */
    public boolean isAuthorized(boolean isActorInternal, Set<String> actorPermissions) {
        return opaClient.canCreateExternalUser(isActorInternal, actorPermissions);
    }

    /**
     * Step 2: Validate email - CQRS Query to User API.
     * Returns validation result without mutating any state.
     */
    public EmailCheckResult validateEmail(String email) {
        return userApiClient.checkEmail(email);
    }

    /**
     * Step 3: Search firms - CQRS Query to User API.
     */
    public List<FirmSummaryDto> searchFirms(String searchTerm, int maxResults) {
        return userApiClient.searchFirms(searchTerm, maxResults);
    }

    /**
     * Step 4: Execute create user - CQRS Command to User API.
     * User API handles TechServices registration + DB persistence.
     */
    public CreateUserResult executeCreateUser(String firstName, String lastName, String email,
                                               boolean isUserManager, boolean isMultiFirmUser,
                                               UUID firmId, String createdBy) {
        CreateUserCommand command = CreateUserCommand.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .userManager(isUserManager)
                .multiFirmUser(isMultiFirmUser)
                .firmId(firmId)
                .createdBy(createdBy)
                .build();

        return userApiClient.createUser(command);
    }
}
