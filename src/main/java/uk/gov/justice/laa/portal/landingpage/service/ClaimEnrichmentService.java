package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
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
    private final OfficeRepository officeRepository;

    public ClaimEnrichmentResponse enrichClaim(ClaimEnrichmentRequest request) {
        log.info("Processing claim enrichment for user: {}", request.getData().getUser().getUserPrincipalName());

        try {
            // 1. Get the EntraUser from database
            EntraUser entraUser = entraUserRepository.findByEntraId(request.getData().getUser().getId())
                    .orElseThrow(() -> new ClaimEnrichmentException("User not found in database"));

            // 2. Get app from DB using the app name from request
            App app = appRepository.findByName(request.getData().getApplication().getDisplayName())
                    .orElseThrow(() -> new ClaimEnrichmentException("Application not found"));

            // 3. Check if user has access to this app
            boolean hasAccess = entraUser.getUserProfiles().stream()
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .anyMatch(appRole ->
                            //TODO: Update Data Model to compare by ID as name may not be unique
                            //appRole.getApp().getId().equals(app.getId())
                            // && appRole.getApp().getName().equals(app.getName())
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
                    .map(office -> office.getId().toString())
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

            log.info("Successfully processed claim enrichment for user: {}", request.getData().getUser().getUserPrincipalName());

            return ClaimEnrichmentResponse.builder()
                    .success(true)
                    .message("Access granted to " + app.getName())
                    .appName(app.getName())
                    .userId(entraUser.getEntraId())
                    .email(entraUser.getEmail())
                    .roles(userRoles)
                    .officeIds(officeIds)
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