package uk.gov.justice.laa.portal.landingpage.service.impl;

import com.microsoft.graph.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.exception.ClaimEnrichmentException;
import uk.gov.justice.laa.portal.landingpage.service.ClaimEnrichmentInterface;
import uk.gov.justice.laa.portal.landingpage.service.EntraIdService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service implementation for enriching user claims with additional permissions from Entra ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimEnrichmentService implements ClaimEnrichmentInterface {

    private final EntraIdService entraIdService;

    @Override
    public ClaimEnrichmentResponse enrichClaims(ClaimEnrichmentRequest request) {
        log.info("Processing claim enrichment for user: {}", request.getUserId());

        // 1. Validate the token
        if (!entraIdService.validateToken(request.getToken())) {
            throw new ClaimEnrichmentException("Invalid or expired token");
        }

        // 2. Get authenticated user details
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ClaimEnrichmentException("User not authenticated");
        }

        // 3. Extract user info from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userPrincipalName = jwt.getClaim("preferred_username");

        if (userPrincipalName == null || userPrincipalName.isBlank()) {
            throw new ClaimEnrichmentException("User principal name not found in token");
        }

        try {
            // 4. Get user details from Entra ID
            CompletableFuture<User> userFuture = entraIdService.getUserByPrincipalName(userPrincipalName);
            User user = userFuture.join();

            if (user == null) {
                throw new ClaimEnrichmentException("User not found in Entra ID");
            }

            // 5. Get user's group memberships (permissions)
            List<String> userGroups = entraIdService.getUserGroupMemberships(user.getId()).join();

            // 6. Map groups to application permissions (simplified example)
            List<String> permissions = mapGroupsToPermissions(userGroups);

            log.info("Successfully enriched claims for user: {}", userPrincipalName);

            return ClaimEnrichmentResponse.builder()
                    .success(true)
                    .permissions(permissions)
                    .message("Claims enriched successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error enriching claims for user {}: {}", userPrincipalName, e.getMessage(), e);
            throw new ClaimEnrichmentException("Failed to enrich claims: " + e.getMessage(), e);
        }
    }

    /**
     * Map Entra ID groups to application permissions.
     * This is a simplified example - in a real application, you would likely
     * have a more sophisticated mapping, possibly stored in a database.
     */
    private List<String> mapGroupsToPermissions(List<String> groupIds) {
        // This is a simplified example - in a real application, you would
        // map group IDs to application-specific permissions, possibly using a database
        return groupIds.stream()
                .map(groupId -> {
                    // Example mapping - replace with your actual logic
                    if (groupId.equals("admin-group-id")) {
                        return "ROLE_ADMIN";
                    } else if (groupId.equals("user-group-id")) {
                        return "ROLE_USER";
                    } else {
                        return "ROLE_BASIC";
                    }
                })
                .distinct()
                .toList();
    }
}