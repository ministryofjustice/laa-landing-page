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
                    .filter(profile -> profile.getAppRoles() != null)
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .filter(role -> role.getApp().equals(app))
                    .map(AppRole::getName)
                    .collect(Collectors.toSet());

            if (userRoles.isEmpty()) {
                throw new ClaimEnrichmentException("User has no roles assigned for this application");
            }

            //5. Get Office IDs
            String officeIds = entraUser.getUserProfiles().stream()
                    .filter(profile -> profile.getFirm() != null)
                    .map(UserProfile::getFirm)
                    .map(Firm::getId)
                    .flatMap(firmId -> officeRepository.findOfficeByFirm_IdIn(List.of(firmId)).stream())
                    .map(office -> office.getId().toString())
                    .distinct()
                    .collect(Collectors.joining(":"));

            boolean isInternalUser = userRoles.contains("INTERNAL");
            boolean hasNoFirm = entraUser.getUserProfiles().stream()
                    .noneMatch(profile -> profile.getFirm() != null);

            if (!isInternalUser) {
                if (hasNoFirm) {
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
                    .roles(userRoles)
                    .userId(entraUser.getId().toString())
                    .email(entraUser.getEmail())
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