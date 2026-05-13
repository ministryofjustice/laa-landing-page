package uk.gov.justice.laa.portal.silas.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.portal.dto.InternalUserPollRequestDto;
import uk.gov.justice.laa.portal.dto.InternalUserPollResultDto;

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
}
