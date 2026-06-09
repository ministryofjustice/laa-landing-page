package uk.gov.justice.laa.portal.silas.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserCommand;
import uk.gov.justice.laa.portal.dto.createuser.CreateUserResult;
import uk.gov.justice.laa.portal.dto.createuser.EmailCheckResult;
import uk.gov.justice.laa.portal.dto.createuser.FirmSummaryDto;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserApiClient {

    private final RestClient userApiRestClient;

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
