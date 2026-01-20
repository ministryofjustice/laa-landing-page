package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserPayloadDto;
import uk.gov.justice.laa.portal.landingpage.dto.CcmsUserDetailsResponse;
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
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
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
    private final FirmRepository firmRepository;
    private final CcmsUserDetailsService ccmsUserDetailsService;

    public ClaimEnrichmentResponse enrichClaim(ClaimEnrichmentRequest request) {
        EntraUserPayloadDto userDetails =
            request.getData().getAuthenticationContext().getUser();
        String userPrincipalName = userDetails.getUserPrincipalName();
        String userId = userDetails.getId();
        String appEntraId = request.getData().getAuthenticationContext().getClientServicePrincipal().getAppId();
        List<Firm> externalFirms = List.of();
        
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
                log.info("User does not have access to this application");
                return ClaimEnrichmentResponse.builder()
                        .success(false)
                        .data(null)
                        .build();
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

            log.info("claim enrichment office ids: {}", officeIds);

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
                } else {
                    externalFirms = firms;
                }

                //External user and offices are not found - fetch all offices for the user's firm
                boolean isUnrestrictedOfficeAccess = entraUser.getUserProfiles().stream()
                        .filter(UserProfile::isActiveProfile)
                        .anyMatch(UserProfile::isUnrestrictedOfficeAccess);

                if (officeIds.isEmpty() && isUnrestrictedOfficeAccess) {
                    officeIds = firms.stream()
                            .flatMap(firm -> officeRepository.findOfficeByFirm_IdIn(List.of(firm.getId())).stream())
                            .map(Office::getCode)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                    log.info("claim enrichment empty office ids: {}", officeIds);
                }
            }

            String legacyUserId = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .map(UserProfile::getLegacyUserId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(UUID::toString)
                    .orElse(null);

            String ccmsUsername = null;
            if (isInternalUser) {
                boolean hasLegacySyncRole = entraUser.getUserProfiles().stream()
                        .filter(UserProfile::isActiveProfile)
                        .flatMap(profile -> profile.getAppRoles().stream())
                        .anyMatch(AppRole::isLegacySync);

                if (hasLegacySyncRole && legacyUserId != null) {
                    CcmsUserDetailsResponse udaResponse = ccmsUserDetailsService.getUserDetailsByLegacyUserId(legacyUserId);
                    if (udaResponse != null
                            && udaResponse.getCcmsUserDetails() != null
                            && udaResponse.getCcmsUserDetails().getUserName() != null) {
                        ccmsUsername = udaResponse.getCcmsUserDetails().getUserName();
                    }
                }
            }

            ClaimEnrichmentResponse.ResponseData responseData = getResponseData(entraUser, userRoles, officeIds, externalFirms, legacyUserId, ccmsUsername);

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
                                                                        List<String> officeIds,
                                                                        List<Firm> externalFirms,
                                                                        String legacyUserId,
                                                                        String ccmsUsername) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("USER_NAME", legacyUserId.toUpperCase());
        claims.put("USER_EMAIL", entraUser.getEmail());
        claims.put("LAA_APP_ROLES", userRoles);
        claims.put("LAA_ACCOUNTS", officeIds);
        if (ccmsUsername != null && !ccmsUsername.isEmpty()) {
            claims.put("CCMS_USERNAME", ccmsUsername);
        }

        if (!externalFirms.isEmpty()) {
            List<String> code = externalFirms.stream().map(Firm::getCode).collect(Collectors.toList());
            List<String> names = externalFirms.stream().map(Firm::getName).collect(Collectors.toList());

            claims.put("FIRM_NAME", names);
            claims.put("FIRM_CODE", code);
        }

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