package uk.gov.justice.laa.portal.silas.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.dto.InternalUserPollRequestDto;
import uk.gov.justice.laa.portal.dto.InternalUserPollResultDto;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserCommand;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserResult;
import uk.gov.justice.laa.portal.dto.createuser.EmailCheckResult;
import uk.gov.justice.laa.portal.dto.createuser.FirmSummaryDto;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserApiClient {

    private final RestClient userApiRestClient;

    public List<UUID> getInternalUserEntraIds() {
        log.debug("Calling User API to get internal user Entra IDs");
        try {
            List<UUID> result = userApiRestClient.get()
                    .uri("/api/internal-users/entra-ids")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            log.debug("Retrieved {} internal user Entra IDs from User API", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch internal user Entra IDs from User API: {}", e.getMessage(), e);
            throw e;
        }
    }

    public InternalUserPollResultDto createInternalUser(InternalUserPollRequestDto request) {
        log.info("Calling User API to create internal user: {}", request.getEntraOid());
        try {
            Map<String, Object> response = userApiRestClient.post()
                    .uri("/api/internal-users/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return InternalUserPollResultDto.builder()
                    .success(Boolean.TRUE.equals(response != null ? response.get("success") : false))
                    .message(response != null ? String.valueOf(response.get("message")) : "No response")
                    .entraOid(request.getEntraOid())
                    .build();
        } catch (Exception e) {
            log.error("Failed to create internal user {} via User API: {}", request.getEntraOid(), e.getMessage(), e);
            return InternalUserPollResultDto.builder()
                    .success(false)
                    .message(e.getMessage())
                    .entraOid(request.getEntraOid())
                    .build();
        }
    }

    public InternalUserPollResultDto deleteInternalUser(UUID entraId) {
        log.info("Calling User API to delete internal user: {}", entraId);
        try {
            Map<String, Object> response = userApiRestClient.delete()
                    .uri("/api/internal-users/delete/{entraId}", entraId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return InternalUserPollResultDto.builder()
                    .success(Boolean.TRUE.equals(response != null ? response.get("success") : false))
                    .message(response != null ? String.valueOf(response.get("message")) : "No response")
                    .entraOid(entraId.toString())
                    .build();
        } catch (Exception e) {
            log.error("Failed to delete internal user {} via User API: {}", entraId, e.getMessage(), e);
            return InternalUserPollResultDto.builder()
                    .success(false)
                    .message(e.getMessage())
                    .entraOid(entraId.toString())
                    .build();
        }
    }

    // ========== CQRS: Create User Flow ==========

    /**
     * Query: Check email availability and domain validity.
     */
    public EmailCheckResult checkEmail(String email) {
        log.debug("CQRS Query: checking email '{}'", email);
        try {
            return userApiRestClient.get()
                    .uri("/api/create-user/query/check-email?email={email}", email)
                    .retrieve()
                    .body(EmailCheckResult.class);
        } catch (Exception e) {
            log.error("Failed to check email via User API: {}", e.getMessage(), e);
            return EmailCheckResult.builder()
                    .available(false)
                    .validDomain(false)
                    .message("Email validation service unavailable")
                    .build();
        }
    }

    /**
     * Query: Search firms by name.
     */
    public List<FirmSummaryDto> searchFirms(String searchTerm, int maxResults) {
        log.debug("CQRS Query: searching firms with term '{}'", searchTerm);
        try {
            return userApiRestClient.get()
                    .uri("/api/create-user/query/firms?search={search}&maxResults={max}", searchTerm, maxResults)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to search firms via User API: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Command: Create a new external user.
     */
    public CreateUserResult createUser(CreateUserCommand command) {
        log.info("CQRS Command: creating user with email '{}'", command.getEmail());
        try {
            return userApiRestClient.post()
                    .uri("/api/create-user/command/execute")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .body(CreateUserResult.class);
        } catch (Exception e) {
            log.error("Failed to create user via User API: {}", e.getMessage(), e);
            return CreateUserResult.builder()
                    .success(false)
                    .message("User creation service unavailable: " + e.getMessage())
                    .email(command.getEmail())
                    .build();
        }
    }
}
