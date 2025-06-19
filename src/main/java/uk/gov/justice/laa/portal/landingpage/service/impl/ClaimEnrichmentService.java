package uk.gov.justice.laa.portal.landingpage.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.exception.ClaimEnrichmentException;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for enriching user claims with additional permissions from Entra ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimEnrichmentService {

    private final EntraUserRepository entraUserRepository;
    private final AppRepository appRepository;

    public ClaimEnrichmentResponse enrichClaims(ClaimEnrichmentRequest request) {
        log.info("Processing claim enrichment for user: {}", request.getData().getUser().getUserPrincipalName());

        try {
            // 1. Get the EntraUser from database
            EntraUser entraUser = entraUserRepository.findByUserName(request.getData().getUser().getUserPrincipalName())
                    .orElseThrow(() -> new ClaimEnrichmentException("User not found in database"));

            // 2. Get app from DB using the ID from request
            App app = appRepository.findByAppRegistrationId(UUID.fromString(request.getData().getApplication().getId()))
                    .orElseThrow(() -> new ClaimEnrichmentException("Application not found"));

            // 3. Check if user has access to this app
            boolean hasAccess = entraUser.getUserAppRegistrations().stream()
                    .anyMatch(reg -> reg.getId().equals(app.getAppRegistration().getId()));

            if (!hasAccess) {
                throw new ClaimEnrichmentException("User does not have access to this application");
            }

            // 4. Get user roles for this app from the database
            Set<String> userRoles = entraUser.getUserProfiles().stream()
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .filter(role -> role.getApp().getId().equals(app.getId()))
                    .map(AppRole::getName)
                    .collect(Collectors.toSet());

            if (userRoles.isEmpty()) {
                throw new ClaimEnrichmentException("User has no roles assigned for this application");
            }

            log.info("Successfully processed claim enrichment for user: {}", request.getData().getUser().getUserPrincipalName());

            return ClaimEnrichmentResponse.builder()
                    .success(true)
                    .roles(userRoles)
                    .appName(app.getName())
                    .message("Access granted to " + app.getName())
                    .build();

        } catch (ClaimEnrichmentException e) {
            log.error("Claim enrichment failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during claim enrichment", e);
            throw new ClaimEnrichmentException("Failed to process claim enrichment", e);
        }
    }
}