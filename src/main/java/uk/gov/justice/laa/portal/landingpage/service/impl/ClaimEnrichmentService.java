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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        // Validate the token
        if (!entraIdService.validateToken(request.getToken())) {
            throw new ClaimEnrichmentException("Invalid or expired token");
        }

        // Get authenticated context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ClaimEnrichmentException("User not authenticated");
        }

        // Extract user info from JWT
        Jwt jwt = (Jwt) authentication.getPrincipal();
        String userPrincipalName = jwt.getClaim("preferred_username");

        if (userPrincipalName == null || userPrincipalName.isBlank()) {
            throw new ClaimEnrichmentException("User principal name not found in token");
        }

        try {
            // Get user details from Entra ID using the access token from the request
            User user = entraIdService.getUserByPrincipalName(request.getToken());

            if (user == null) {
                throw new ClaimEnrichmentException("User not found in Entra ID");
            }

            // Get user's group memberships (permissions)
            // Map groups to application permissions for the target app
            Map<String, Object> accessInfo = mapGroupsToPermissions(
                    request.getTargetAppId(),
                    entraIdService.getUserGroupMemberships(request.getToken())
            );
            log.info("Successfully enriched claims for user: {}", userPrincipalName);

            return ClaimEnrichmentResponse.builder()
                    .success(true)
                    .permissions((List<String>) accessInfo.get("permissions"))
                    .message("Access granted to " + accessInfo.get("appName"))
                    .build();

        } catch (Exception e) {
            log.error("Error enriching claims for user {}: {}", userPrincipalName, e.getMessage(), e);
            throw new ClaimEnrichmentException("Failed to enrich claims: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> mapGroupsToPermissions(String targetAppId, List<String> groupIds) {
    // Dummy data: Map of app IDs to their configuration
    Map<String, Map<String, Object>> dummyApps = Map.of(
        "app1", Map.of(
            "name", "Test App 1",
            "requiredRoles", Set.of("case_manager", "admin"),
            "permissions", List.of("cases:read", "cases:write", "reports:view")
        ),
        "app2", Map.of(
            "name", "Some App",
            "requiredRoles", Set.of("billing", "admin"),
            "permissions", List.of("invoices:view", "payments:process"))
    );

    // Dummy data: Map of group IDs to their roles
    Map<String, Set<String>> groupRoles = Map.of(
        "grp_case_managers", Set.of("case_manager"),
        "grp_billing_team", Set.of("billing"),
        "grp_analysts", Set.of("analyst"),
        "grp_admins", Set.of("admin", "case_manager", "billing", "analyst")
    );

    // 1. Collect all roles from user's groups
    Set<String> userRoles = groupIds.stream()
        .flatMap(groupId -> groupRoles.getOrDefault(groupId, Set.of()).stream())
        .collect(Collectors.toSet());

    // 2. Check if target app exists
    if (!dummyApps.containsKey(targetAppId)) {
        throw new ClaimEnrichmentException("Application not found: " + targetAppId);
    }

    Map<String, Object> appConfig = dummyApps.get(targetAppId);
    Set<String> requiredRoles = (Set<String>) appConfig.get("requiredRoles");
    List<String> appPermissions = (List<String>) appConfig.get("permissions");

    // 3. Check if user has any of the required roles
    boolean hasAccess = userRoles.stream()
        .anyMatch(requiredRoles::contains);

    if (!hasAccess) {
        throw new ClaimEnrichmentException("Insufficient permissions for application: " + targetAppId);
    }

    // 4. Return user's access details for the app
    return Map.of(
        "appId", targetAppId,
        "appName", appConfig.get("name"),
        "roles", userRoles,
        "permissions", appPermissions,
        "hasAccess", true
    );
}
}