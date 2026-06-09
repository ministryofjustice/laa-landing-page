package uk.gov.justice.laa.portal.landingpage.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserResult;
import uk.gov.justice.laa.portal.dto.createuser.EmailCheckResult;
import uk.gov.justice.laa.portal.dto.createuser.FirmSummaryDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Client for SiLAS create user API.
 * Used when feature.flag.silas.create.user=true.
 * Delegates the create user flow to SiLAS which orchestrates via CQRS + OPA.
 */
@Slf4j
@Service
public class SilasCreateUserClient {

    private final RestClient silasRestClient;

    public SilasCreateUserClient(@Qualifier("silasRestClient") RestClient silasRestClient) {
        this.silasRestClient = silasRestClient;
    }

    /**
     * Check authorization via SiLAS (which uses OPA).
     */
    public boolean isAuthorized(boolean isActorInternal, Set<String> actorPermissions) {
        try {
            Map<String, Object> request = Map.of(
                    "isActorInternal", isActorInternal,
                    "permissions", actorPermissions
            );
            Map response = silasRestClient.post()
                    .uri("/api/create-user/authorize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);
            return response != null && Boolean.TRUE.equals(response.get("authorized"));
        } catch (Exception e) {
            log.error("SiLAS authorization check failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validate email via SiLAS (which queries User API).
     */
    public EmailCheckResult validateEmail(String email) {
        try {
            return silasRestClient.get()
                    .uri("/api/create-user/validate-email?email={email}", email)
                    .retrieve()
                    .body(EmailCheckResult.class);
        } catch (Exception e) {
            log.error("SiLAS email validation failed: {}", e.getMessage(), e);
            return EmailCheckResult.builder()
                    .available(false)
                    .validDomain(false)
                    .message("Validation service unavailable")
                    .build();
        }
    }

    /**
     * Search firms via SiLAS (which queries User API).
     */
    public List<FirmSummaryDto> searchFirms(String searchTerm, int maxResults) {
        try {
            return silasRestClient.get()
                    .uri("/api/create-user/search-firms?search={search}&maxResults={max}", searchTerm, maxResults)
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<FirmSummaryDto>>() {});
        } catch (Exception e) {
            log.error("SiLAS firm search failed: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Execute create user via SiLAS (which uses OPA + issues CQRS command to User API).
     */
    public CreateUserResult executeCreateUser(String firstName, String lastName, String email,
                                               boolean isUserManager, boolean isMultiFirmUser,
                                               UUID firmId, String createdBy,
                                               boolean isActorInternal, Set<String> actorPermissions) {
        try {
            Map<String, Object> request = Map.of(
                    "firstName", firstName,
                    "lastName", lastName,
                    "email", email,
                    "isUserManager", isUserManager,
                    "isMultiFirmUser", isMultiFirmUser,
                    "firmId", firmId != null ? firmId.toString() : "",
                    "createdBy", createdBy,
                    "isActorInternal", isActorInternal,
                    "actorPermissions", actorPermissions
            );
            return silasRestClient.post()
                    .uri("/api/create-user/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(CreateUserResult.class);
        } catch (Exception e) {
            log.error("SiLAS create user failed: {}", e.getMessage(), e);
            return CreateUserResult.builder()
                    .success(false)
                    .message("SiLAS service unavailable: " + e.getMessage())
                    .email(email)
                    .build();
        }
    }
}
