package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.EntraClaimAction; // Added for parsing incoming actions
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserPayloadDto;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.Firm;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.ClaimEnrichmentException;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.util.List;
import java.util.Map; // Added for handling dynamic claims
import java.util.stream.Collectors;

/**
 * Service for handling claim enrichment requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimEnrichmentService {

    private final EntraUserRepository entraUserRepository;
    private final AppRepository appRepository;
    private final OfficeRepository officeRepository;

    /**
     * Enriches the claims for a given user with additional permissions.
     *
     * @param request The claim enrichment request containing user and token information
     * @return ResponseEntity containing the enriched claims response
     */
    public ClaimEnrichmentResponse enrichClaim(ClaimEnrichmentRequest request) {
        EntraUserPayloadDto userDetails =
            request.getData().getAuthenticationContext().getUser();
        String userPrincipalName = userDetails.getUserPrincipalName();
        String userId = userDetails.getId();
        String appDisplayName = request.getData().getAuthenticationContext()
            .getClientServicePrincipal().getAppDisplayName();
        
        log.info("Processing claim enrichment for user: {}", userPrincipalName);

        try {
            // New: Check for and process incoming claims from Entra ID if present
            if (request.getData().getActions() != null && !request.getData().getActions().isEmpty()) {
                for (EntraClaimAction action : request.getData().getActions()) {
                    // Assuming the action type for custom claims is "microsoft.graph.issueCustomClaims"
                    if ("microsoft.graph.issueCustomClaims".equals(action.getOdataType())) {
                        Map<String, Object> incomingClaims = action.getClaims();
                        if (incomingClaims != null) {
                            log.info("Received incoming claims from Entra ID: {}", incomingClaims);
                            // TODO: Add logic here to process or utilize these incomingClaims.
                            // For example, you might:
                            // - Extract specific claims like "extension_xxxx_myclaim"
                            // - Merge them with claims from your database
                            // - Perform validation based on incoming claims
                            // - This part depends heavily on your specific business requirements.
                        }
                    }
                }
            }

            // 1. Get the EntraUser from database
            EntraUser entraUser = entraUserRepository.findByEntraUserId(userId)
                    .orElseThrow(() -> new ClaimEnrichmentException("User not found in database"));

            // 2. Get app from DB using the app name from request
            App app = appRepository.findByName(appDisplayName)
                    .orElseThrow(() -> new ClaimEnrichmentException("Application not found"));

            // 3. Check if user has access to this app
            boolean hasAccess = entraUser.getUserProfiles().stream()
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .anyMatch(appRole ->
                            //TODO: Update Data Model to compare by ID as name may not be unique
                            // Reverted to original logic based on user's request
                            appRole.getApp().getName().equals(app.getName())
                    );

            if (!hasAccess) {
                throw new ClaimEnrichmentException("User does not have access to this application");
            }

            // 4. Get user roles for this app from the database
            List<String> userRoles = entraUser.getUserProfiles().stream()
                    .filter(profile -> profile.getAppRoles() != null)
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .filter(role ->
                            //TODO: Update Data Model to compare by ID as name may not be unique
                            role.getApp().getId().equals(app.getId())
                                    && role.getApp().getName().equals(app.getName())
                    )
                    .map(AppRole::getName)
                    .collect(Collectors.toList());

            if (userRoles.isEmpty()) {
                throw new ClaimEnrichmentException("User has no roles assigned for this application");
            }

            //5. Get Office IDs
            //TODO: officeIds should be updated to officeCode when Data Model Updated
            List<String> officeIds = entraUser.getUserProfiles().stream()
                    .filter(profile -> profile.getFirm() != null)
                    .map(UserProfile::getFirm)
                    .map(Firm::getId)
                    .flatMap(firmId -> officeRepository.findOfficeByFirm_IdIn(List.of(firmId)).stream())
                    .map(office -> office.getId().toString()) // Reverted to original logic based on user's request
                    .distinct()
                    .collect(Collectors.toList());

            boolean isInternalUser = entraUser.getUserProfiles().stream()
                    .anyMatch(profile -> profile.getUserType() == UserType.INTERNAL);
            
            if (!isInternalUser) {
                boolean hasFirm = entraUser.getUserProfiles().stream()
                        .anyMatch(profile -> profile.getFirm() != null);

                if (!hasFirm) {
                    throw new ClaimEnrichmentException("User has no firm assigned");
                }
                if (officeIds.isEmpty()) {
                    throw new ClaimEnrichmentException("User has no offices assigned for this firm");
                }
            }

            log.info("Successfully processed claim enrichment for user: {}", userPrincipalName);

            return ClaimEnrichmentResponse.builder()
                    .success(true)
                    .message("Access granted to " + app.getName())
                    .correlationId(request.getData().getAuthenticationContext().getCorrelationId())
                    .user_name(userDetails.getDisplayName())
                    .user_email(entraUser.getEmail())
                    .laa_app_roles(userRoles)
                    .laa_accounts(officeIds)
                    .build();

        } catch (ClaimEnrichmentException e) {
            log.error("Claim enrichment failed for user {}: {}", userPrincipalName, e.getMessage());
            throw e;
        } catch (Exception e) {
            // Updated: Log full exception for better debugging
            log.error("Unexpected error during claim enrichment for user {}:", userPrincipalName, e);
            throw new ClaimEnrichmentException("Failed to process claim enrichment due to an unexpected error", e);
        }
    }
}