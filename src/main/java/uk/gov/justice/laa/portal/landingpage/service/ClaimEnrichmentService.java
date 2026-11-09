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
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;
import uk.gov.justice.laa.portal.landingpage.entity.Office;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.exception.ClaimEnrichmentException;
import uk.gov.justice.laa.portal.landingpage.exception.UserNotFoundException;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.FirmRepository;
import uk.gov.justice.laa.portal.landingpage.repository.OfficeRepository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    public ClaimEnrichmentResponse enrichClaim(ClaimEnrichmentRequest request) {
        EntraUserPayloadDto userDetails =
            request.getData().getAuthenticationContext().getUser();
        String userPrincipalName = userDetails.getUserPrincipalName();
        String userId = userDetails.getId();
        String appEntraId = request.getData().getAuthenticationContext().getClientServicePrincipal().getAppId();
        List<Firm> externalFirms = List.of();
        
        log.info("Claim enrichment initiated for OID: {}", userId);

        try {
            // 1. Get the EntraUser from database
            EntraUser entraUser = entraUserRepository.findByEntraOid(userId)
                    .orElseThrow(() -> {
                        log.warn("User not found for the given entra id: {}", userId);
                        return new UserNotFoundException(
                                String.format("User not found for the given entra id: %s", userId));
                    });

            // 2. Get app from DB using the app entra id from request
            App app = appRepository.findByEntraAppId(appEntraId)
                    .orElseThrow(() -> new ClaimEnrichmentException("Application not found"));

            if (!app.isEnabled()) {
                log.info("App disabled on SiLAS, so user cannot access the application");
                return ClaimEnrichmentResponse.builder()
                        .success(false)
                        .data(null)
                        .build();
            }

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
                    .map(AppRole::getRoleIdentifier)
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
                    log.info("No direct office assignments found for user. Retrieving offices from associated firms.");
                    officeIds = firms.stream()
                            .flatMap(firm -> officeRepository.findOfficeByFirm_IdIn(List.of(firm.getId())).stream())
                            .map(Office::getCode)
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                    log.info("claim enrichment fallback office ids from firms: {}", officeIds);
                }
            }

            Optional<UserProfile> userProfileOptional = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .findFirst();

            String profileId = userProfileOptional
                    .map(UserProfile::getId)
                    .map(UUID::toString)
                    .orElse(null);

            String legacyUserId = entraUser.getUserProfiles().stream()
                    .filter(UserProfile::isActiveProfile)
                    .map(UserProfile::getLegacyUserId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .map(UUID::toString)
                    .orElse(null);

            Optional<Set<String>> cccdAgfsAccountsClaim = Optional.empty();

            if (userProfileOptional.isPresent()) {
                UserProfile activeProfile = userProfileOptional.get();
                if (activeProfile.getUserType() == UserType.EXTERNAL && "Claim for Crown Court Defence".equals(app.getName())) {
                    cccdAgfsAccountsClaim = resolveCccdAgfsClaim(activeProfile);
                }
            }

            ClaimEnrichmentResponse.ResponseData responseData = getResponseData(entraUser, userRoles, officeIds, externalFirms, profileId, legacyUserId, cccdAgfsAccountsClaim);

            return ClaimEnrichmentResponse.builder()
                    .success(true)
                    .data(responseData)
                    .build();

        } catch (ClaimEnrichmentException e) {
            log.error("Claim enrichment failed: {}", e.getMessage());
            throw e;
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException(String.format("User not found for the given entra id: %s", userId));
        } catch (Exception e) {
            log.error("Unexpected error during claim enrichment", e);
            throw new ClaimEnrichmentException("Failed to process claim enrichment", e);
        }
    }

    private Optional<Set<String>> resolveCccdAgfsClaim(UserProfile activeProfile) {
        if (activeProfile.getFirm() == null || activeProfile.getFirm().getType() == null) {
            return Optional.empty();
        }
        Firm firm = activeProfile.getFirm();
        if (firm.getType() == FirmType.LEGAL_SERVICES_PROVIDER) {
            String advocateName = firm.getName() + "-ADVOCATE";
            Optional<Firm> advocateFirmOptional = Optional.ofNullable(firmRepository.findFirmByName(advocateName));
            if (advocateFirmOptional.isEmpty()) {
                return Optional.empty();
            }
            Firm advocateFirm = advocateFirmOptional.get();
            if (advocateFirm.getType() != FirmType.ADVOCATE || !advocateFirm.getEnabled()) {
                return Optional.empty();
            }
            Set<Office> offices = advocateFirm.getOffices();
            Set<String> officeCodes = offices.stream().map(Office::getCode).collect(Collectors.toSet());
            if (officeCodes.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(officeCodes);
            }

        } else if (firm.getType() == FirmType.CHAMBERS) {
            Set<String> officeCodes = firm.getChildFirms().stream()
                    .filter(Firm::getEnabled)
                    .filter(childFirm -> childFirm.getType() == FirmType.ADVOCATE)
                    .flatMap(childFirm -> childFirm.getOffices().stream())
                    .map(Office::getCode)
                    .collect(Collectors.toSet());
            if (officeCodes.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(officeCodes);
            }
        } else if (firm.getType() == FirmType.ADVOCATE) {
            Set<String> officeCodes = firm.getOffices().stream()
                    .map(Office::getCode)
                    .collect(Collectors.toSet());
            if (officeCodes.isEmpty()) {
                return Optional.empty();
            }
            if (activeProfile.isUnrestrictedOfficeAccess()) {
                return Optional.of(officeCodes);
            } else {
                Set<String> userOfficeCodes = activeProfile.getOffices().stream()
                        .map(Office::getCode)
                        .collect(Collectors.toSet());
                Set<String> intersection = new HashSet<>(officeCodes);
                intersection.retainAll(userOfficeCodes);
                if (intersection.isEmpty()) {
                    return Optional.empty();
                } else {
                    return Optional.of(intersection);
                }
            }

        }
        return Optional.empty();
    }

    private static ClaimEnrichmentResponse.ResponseData getResponseData(EntraUser entraUser,
                                                                        List<String> userRoles,
                                                                        List<String> officeIds,
                                                                        List<Firm> externalFirms,
                                                                        String profileId,
                                                                        String legacyUserId,
                                                                        Optional<Set<String>> cccdAgfsAccountsClaim) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("USER_NAME", legacyUserId.toUpperCase());
        claims.put("USER_EMAIL", entraUser.getEmail());
        claims.put("LAA_APP_ROLES", userRoles);
        claims.put("LAA_ACCOUNTS", officeIds);
        if (profileId != null && !profileId.isEmpty()) {
            claims.put("LAA_PROFILE_ID", profileId);
        }
        if (legacyUserId != null && !legacyUserId.isEmpty()) {
            claims.put("LAA_CCMS_USERNAME", legacyUserId.toUpperCase());
        }

        cccdAgfsAccountsClaim.ifPresent(claim -> claims.put("CCCD_AGFS_ACCOUNTS", claim));

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
