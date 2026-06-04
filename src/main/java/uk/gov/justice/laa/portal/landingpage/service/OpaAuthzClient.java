package uk.gov.justice.laa.portal.landingpage.service;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OpaAuthzClient {

    private static final Logger log = LoggerFactory.getLogger(OpaAuthzClient.class);

    private final RestClient opaRestClient;

    public OpaAuthzClient(@Qualifier("opaRestClient") RestClient opaRestClient) {
        this.opaRestClient = opaRestClient;
    }

    /**
     * Evaluate the canResendActivationForAuditUser policy against OPA.
     *
     * @param actorIsInternal      whether the acting user is internal
     * @param actorPermissions     the set of permission names the actor holds
     * @param targetIsInternal     whether the target user is internal
     * @param targetIsEnabled      whether the target user's account is enabled
     * @param targetInvitationStatus the target user's invitation status (e.g. "VERIFICATION_SUCCESS", "PENDING")
     * @return true if OPA allows the action, false otherwise
     */
    public boolean canResendActivationForAuditUser(boolean actorIsInternal,
                                                    Set<String> actorPermissions,
                                                    boolean targetIsInternal,
                                                    boolean targetIsEnabled,
                                                    String targetInvitationStatus) {
        Map<String, Object> input = Map.of(
                "actor", Map.of(
                        "is_internal", actorIsInternal,
                        "permissions", actorPermissions
                ),
                "target", Map.of(
                        "is_internal", targetIsInternal,
                        "is_enabled", targetIsEnabled,
                        "invitation_status", targetInvitationStatus != null ? targetInvitationStatus : ""
                )
        );

        Map<String, Object> requestBody = Map.of("input", input);

        log.debug("Calling OPA for canResendActivationForAuditUser with input: {}", input);

        try {
            OpaResponse response = opaRestClient.post()
                    .uri("/v1/data/authz/can_resend_activation_for_audit_user")
                    .body(requestBody)
                    .retrieve()
                    .body(OpaResponse.class);

            boolean result = response != null && Boolean.TRUE.equals(response.result());
            log.debug("OPA decision for canResendActivationForAuditUser: {}", result);
            return result;
        } catch (Exception e) {
            log.error("OPA call failed for canResendActivationForAuditUser, denying by default", e);
            return false;
        }
    }

    /**
     * Response record matching OPA's JSON response format: {"result": true/false}
     */
    public record OpaResponse(Boolean result) {}
}
