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
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.ClaimEnrichmentException;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.UUID;

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
        String appEntraId = request.getData().getAuthenticationContext().getClientServicePrincipal().getAppId();
        
        log.info("Processing claim enrichment for user: {}", userPrincipalName);

        try {
            // 1. Get the EntraUser from database
            EntraUser entraUser = entraUserRepository.findByEntraOid(userId)
                    .orElseThrow(() -> new ClaimEnrichmentException("User not found in database"));

            // 2. Get app from DB using the app entra id from request
            App app = appRepository.findByEntraAppId(appEntraId)
                    .orElseThrow(() -> new ClaimEnrichmentException("Application not found"));

            // 3. Check if user has access to this app
            boolean hasAccess = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .anyMatch(appRole -> appRole.getApp().getId().equals(app.getId()));

            if (!hasAccess) {
                throw new ClaimEnrichmentException("User does not have access to this application");
            }

            // 4. Get user roles for this app from the database
            List<String> userRoles = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .filter(profile -> profile.getAppRoles() != null)
                    .flatMap(profile -> profile.getAppRoles().stream())
                    .filter(role -> role.getApp().getId().equals(app.getId()))
                    .map(AppRole::getName)
                    .distinct()
                    .collect(Collectors.toList());

            if (userRoles.isEmpty()) {
                throw new ClaimEnrichmentException("User has no roles assigned for this application");
            }

            //5. Get Office codes associated to the user
            List<String> officeIds = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .map(UserProfile::getOffices)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(Office::getCode)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            boolean isInternalUser = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .anyMatch(profile -> profile.getUserType() == UserType.INTERNAL);

            if (!isInternalUser) {
                List<Firm> firms = entraUser.getUserProfiles().stream()
                        .filter(UserProfile::isActiveProfile)
                        .map(UserProfile::getFirm)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());

                if (firms.isEmpty()) {
                    throw new ClaimEnrichmentException("User has no firm assigned");
                }

                //External user and offices are not found - fetch all offices for the user's firm
                if (officeIds.isEmpty()) {
                    officeIds = firms.stream()
                            .flatMap(firm -> officeRepository.findOfficeByFirm_IdIn(List.of(firm.getId())).stream())
                            .map(Office::getCode)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                }
            }

            ClaimEnrichmentResponse.ResponseData responseData = getResponseData(entraUser, userRoles, officeIds);

            return ClaimEnrichmentResponse.builder()
                    .success(true)
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

    private static ClaimEnrichmentResponse.ResponseData getResponseData(EntraUser entraUser,
                                                                        List<String> userRoles,
                                                                        List<String> officeIds) {

        String legacyUserId = entraUser.getUserProfiles().stream()
                .filter(UserProfile::isActiveProfile)
                .map(UserProfile::getLegacyUserId)
                .filter(Objects::nonNull)
                .findFirst()
                .map(UUID::toString)
                .orElse(null);

        Map<String, Object> claims = new HashMap<>();
        claims.put("USER_NAME", legacyUserId);
        claims.put("USER_EMAIL", entraUser.getEmail());
        claims.put("LAA_APP_ROLES", userRoles);
        claims.put("LAA_ACCOUNTS", officeIds);

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