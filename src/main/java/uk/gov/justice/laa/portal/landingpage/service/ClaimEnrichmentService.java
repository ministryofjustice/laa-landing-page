package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
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
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimEnrichmentService {

    private final EntraUserRepository entraUserRepository;
    private final AppRepository appRepository;
    private final OfficeRepository officeRepository;

    public ClaimEnrichmentResponse enrichClaim(ClaimEnrichmentRequest request) {
        EntraUserPayloadDto userDetails =
            request.getData().getAuthenticationContext().getUser();
        String userPrincipalName = userDetails.getUserPrincipalName();
        String userId = userDetails.getId();
        String appDisplayName = request.getData().getAuthenticationContext()
            .getClientServicePrincipal().getAppDisplayName();
        
        log.info("Processing claim enrichment for user: {}", userPrincipalName);

        try {
            // 1. Get the EntraUser from database
            EntraUser entraUser = entraUserRepository.findByEntraOid(userId)
                    .orElseThrow(() -> new ClaimEnrichmentException("User not found in database"));

            // 2. Get app from DB using the app name from request
            App app = appRepository.findByName(appDisplayName)
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
                    .distinct()
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

            log.info("Successfully processed claim enrichment for user: {}", userPrincipalName);

            ClaimEnrichmentResponse.ResponseData responseData = getResponseData(userDetails, entraUser, userRoles, officeIds);

            return ClaimEnrichmentResponse.builder()
                    .data(responseData)
                    .build();

        } catch (ClaimEnrichmentException e) {
            log.error("Claim enrichment failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during claim enrichment", e);
            throw new ClaimEnrichmentException("Failed to process claim enrichment", e);
        }
    }

    private static ClaimEnrichmentResponse.ResponseData getResponseData(EntraUserPayloadDto userDetails,
                                                                        EntraUser entraUser,
                                                                        List<String> userRoles,
                                                                        List<String> officeIds) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("user_name", userDetails.getDisplayName());
        claims.put("user_email", entraUser.getEmail());
        claims.put("laa_app_roles", userRoles);
        claims.put("laa_accounts", officeIds);

        ClaimEnrichmentResponse.ResponseAction action = ClaimEnrichmentResponse.ResponseAction.builder()
                .odataType("microsoft.graph.tokenIssuanceStart.provideClaimsForToken")
                .claims(claims)
                .build();

        ClaimEnrichmentResponse.ResponseData responseData = ClaimEnrichmentResponse.ResponseData.builder()
                .odataType("microsoft.graph.onTokenIssuanceStartResponseData")
                .actions(List.of(action))
                .build();

        return responseData;
    }
}