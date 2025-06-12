package uk.gov.justice.laa.portal.landingpage.service.impl;

import com.microsoft.graph.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.exception.ClaimEnrichmentException;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.ClaimEnrichmentInterface;
import uk.gov.justice.laa.portal.landingpage.service.EntraIdService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for enriching user claims with additional permissions from Entra ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimEnrichmentService implements ClaimEnrichmentInterface {

    private final EntraIdService entraIdService;
    private final EntraUserRepository entraUserRepository;
    private final AppRepository appRepository;

    @Override
    public ClaimEnrichmentResponse enrichClaims(ClaimEnrichmentRequest request) {
        log.info("Processing claim enrichment for user: {}", request.getUserId());

        try {
            // 1. Get user details from Entra ID using the token
            User user = entraIdService.getUserByPrincipalName(request.getToken());
            if (user == null) {
                throw new ClaimEnrichmentException("User not found in Entra ID");
            }

            // 2. Get the EntraUser from database
            EntraUser entraUser = entraUserRepository.findByEmail(user.getUserPrincipalName())
                .orElseThrow(() -> new ClaimEnrichmentException("User not found in database"));

            // 3. Get user's group memberships (permissions)
            List<String> userGroups = entraIdService.getUserGroupMemberships(request.getToken());

            // 4. Get app by ID from request
            App app = appRepository.findByAppRegistrationId(UUID.fromString(request.getTargetAppId()))
                .orElseThrow(() -> new ClaimEnrichmentException("Application not found"));

            // 5. Check if user has access to this app
            boolean hasAccess = entraUser.getUserAppRegistrations().stream()
                .anyMatch(reg -> reg.getId().equals(app.getAppRegistration().getId()));

            if (!hasAccess) {
                throw new ClaimEnrichmentException("User does not have access to this application");
            }

            // 6. Get all roles and permissions for this user and app
            Map<String, Object> accessInfo = mapGroupsToPermissions(request.getTargetAppId(), userGroups);

            log.info("Successfully processed claim enrichment for user: {}", user.getUserPrincipalName());

            return ClaimEnrichmentResponse.builder()
                .success(true)
                .roles((Set<String>) accessInfo.get("roles"))
                .appName((String) accessInfo.get("appName"))
                .message("Access granted to " + accessInfo.get("appName"))
                .build();

        } catch (ClaimEnrichmentException e) {
            log.error("Claim enrichment failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during claim enrichment", e);
            throw new ClaimEnrichmentException("Failed to process claim enrichment", e);
        }
    }

    private Map<String, Object> mapGroupsToPermissions(String targetAppId, List<String> groupIds) {
        // Get the app from database
        App app = appRepository.findByAppRegistrationId(UUID.fromString(targetAppId))
                .orElseThrow(() -> new ClaimEnrichmentException("Application not found"));

        // Get all roles for this app
        Set<AppRole> appRoles = app.getAppRoles();

        // Map group IDs to role names
        Set<String> userRoles = appRoles.stream()
                .filter(role -> groupIds.contains(role.getName()))
                .map(AppRole::getName)
                .collect(Collectors.toSet());

        // In this simplified model, permissions are the same as roles
        // You can add more granular permission logic here if needed later
        Set<String> permissions = new HashSet<>(userRoles);

        return Map.of(
                "appId", targetAppId,
                "appName", app.getName(),
                "roles", userRoles,
                "permissions", new ArrayList<>(permissions),
                "hasAccess", !userRoles.isEmpty()
        );
    }
}