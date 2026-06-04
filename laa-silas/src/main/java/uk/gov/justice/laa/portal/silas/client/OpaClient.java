package uk.gov.justice.laa.portal.silas.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Set;

/**
 * Client for Open Policy Agent (OPA).
 * Evaluates authorization policies for the create user flow.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpaClient {

    private final RestClient opaRestClient;

    /**
     * Check if the actor is allowed to create an external user.
     *
     * @param isInternal      whether the actor is an internal user
     * @param permissions     the actor's permission set
     * @return true if OPA allows the action
     */
    public boolean canCreateExternalUser(boolean isInternal, Set<String> permissions) {
        Map<String, Object> input = Map.of(
                "actor", Map.of(
                        "is_internal", isInternal,
                        "permissions", permissions
                )
        );

        return evaluatePolicy("/v1/data/authz/can_create_external_user", input);
    }

    private boolean evaluatePolicy(String policyPath, Map<String, Object> input) {
        Map<String, Object> requestBody = Map.of("input", input);
        log.debug("Calling OPA at {} with input: {}", policyPath, input);

        try {
            OpaResponse response = opaRestClient.post()
                    .uri(policyPath)
                    .body(requestBody)
                    .retrieve()
                    .body(OpaResponse.class);

            boolean result = response != null && Boolean.TRUE.equals(response.result());
            log.debug("OPA decision for {}: {}", policyPath, result);
            return result;
        } catch (Exception e) {
            log.error("OPA call failed for {}, denying by default", policyPath, e);
            return false;
        }
    }

    public record OpaResponse(Boolean result) {}
}
