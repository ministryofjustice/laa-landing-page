package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.EntraAuthenticationContext;
import uk.gov.justice.laa.portal.landingpage.dto.EntraClaimData;
import uk.gov.justice.laa.portal.landingpage.dto.EntraServicePrincipalDto;
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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimEnrichmentServiceTest {

    private static final String USER_ENTRA_ID = "entra-123";
    private static final String USER_EMAIL = "test@example.com";
    private static final UUID LEGACY_USER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String APP_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String ENTRA_APP_ID = "EntraAppID";
    private static final String APP_NAME = "Test App";
    private static final String EXTERNAL_ROLE = "USER_ROLE";
    private static final String INTERNAL_ROLE = "INTERNAL";
    private static final UUID OFFICE_ID_1 = UUID.randomUUID();
    private static final UUID OFFICE_ID_2 = UUID.randomUUID();
    private static final UUID FIRM_ID = UUID.randomUUID();

    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private AppRepository appRepository;
    @Mock
    private OfficeRepository officeRepository;
    @InjectMocks
    private ClaimEnrichmentService claimEnrichmentService;

    private ClaimEnrichmentRequest request;
    private EntraUser entraUser;
    private App app;
    private Office office1;
    private Office office2;
    private Firm firm;

    @BeforeEach
    void setUp() {
        // Setup test data for request object
        EntraUserPayloadDto userInfo = EntraUserPayloadDto.builder()
                .id(USER_ENTRA_ID)
                .build();

        EntraServicePrincipalDto clientServicePrincipal = EntraServicePrincipalDto.builder()
                .appDisplayName(APP_NAME)
                .appId(ENTRA_APP_ID)
                .build();

        EntraAuthenticationContext authContext = EntraAuthenticationContext.builder()
                .user(userInfo)
                .clientServicePrincipal(clientServicePrincipal)
                .build();
                
        EntraClaimData data = EntraClaimData.builder()
                .authenticationContext(authContext)
                .tenantId("test-tenant-id")
                .build();
                
        request = ClaimEnrichmentRequest.builder()
                .data(data)
                .build();

        app = App.builder()
                .id(UUID.fromString(APP_ID))
                .entraAppId(ENTRA_APP_ID)
                .name(APP_NAME)
                .build();

        firm = Firm.builder()
                .id(FIRM_ID)
                .build();

        // Setup test data for offices
        office1 = Office.builder()
                .id(OFFICE_ID_1)
                .firm(firm)
                .code("Office 1 Code")
                .build();
        office2 = Office.builder()
                .id(OFFICE_ID_2)
                .firm(firm)
                .code("Office 2 Code")
                .build();

        AppRole appRole = AppRole.builder()
                .app(app)
                .name(EXTERNAL_ROLE)
                .build();

        UserProfile userProfile = UserProfile.builder()
                .activeProfile(true)
                .firm(firm)
                .legacyUserId(LEGACY_USER_ID)
                .appRoles(Set.of(appRole))
                .unrestrictedOfficeAccess(true)
                .build();

        entraUser = EntraUser.builder()
                .id(USER_ID)
                .entraOid(USER_ENTRA_ID)
                .email(USER_EMAIL)
                .userProfiles(Set.of(userProfile))
                .build();
    }

    @Test
    void enrichClaim_Success_When_UnrestrictedOfficeAccess_is_false() {
        // Arrange
        UserProfile profile1 = UserProfile.builder().activeProfile(true)
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .legacyUserId(LEGACY_USER_ID)
                .firm(firm)
                .unrestrictedOfficeAccess(false)
                .build();
        entraUser.setUserProfiles(Set.of(profile1));

        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.of(app));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals("microsoft.graph.onTokenIssuanceStartResponseData", response.getData().getOdataType());
        
        // Verify actions
        assertNotNull(response.getData().getActions());
        assertEquals(1, response.getData().getActions().size());
        assertTrue(response.isSuccess());
        
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().get(0);
        assertEquals("microsoft.graph.tokenIssuanceStart.provideClaimsForToken", action.getOdataType());
        
        // Verify claims
        Map<String, Object> claims = action.getClaims();
        assertNotNull(claims);
        assertEquals(LEGACY_USER_ID.toString().toUpperCase(), claims.get("USER_NAME"));
        assertEquals(USER_EMAIL, claims.get("USER_EMAIL"));
        assertEquals(List.of(EXTERNAL_ROLE), claims.get("LAA_APP_ROLES"));
        assertEquals(List.of(), claims.get("LAA_ACCOUNTS"));
        
        verify(officeRepository, times(0)).findOfficeByFirm_IdIn(List.of(FIRM_ID));
    }

    @Test
    void enrichClaim_Success() {
        // Arrange
        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.of(app));
        when(officeRepository.findOfficeByFirm_IdIn(List.of(FIRM_ID))).thenReturn(List.of(office1, office2));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals("microsoft.graph.onTokenIssuanceStartResponseData", response.getData().getOdataType());

        // Verify actions
        assertNotNull(response.getData().getActions());
        assertEquals(1, response.getData().getActions().size());
        assertTrue(response.isSuccess());

        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().get(0);
        assertEquals("microsoft.graph.tokenIssuanceStart.provideClaimsForToken", action.getOdataType());

        // Verify claims
        Map<String, Object> claims = action.getClaims();
        assertNotNull(claims);
        assertEquals(LEGACY_USER_ID.toString().toUpperCase(), claims.get("USER_NAME"));
        assertEquals(USER_EMAIL, claims.get("USER_EMAIL"));
        assertEquals(List.of(EXTERNAL_ROLE), claims.get("LAA_APP_ROLES"));
        assertEquals(List.of(office1.getCode(), office2.getCode()), claims.get("LAA_ACCOUNTS"));

        verify(officeRepository).findOfficeByFirm_IdIn(List.of(FIRM_ID));
    }

    @Test
    void enrichClaim_ExternalUserWithMultipleFirmsAndOffices() {
        // Arrange
        UUID firm2Id = UUID.randomUUID();
        Firm firm2 = Firm.builder()
                .id(firm2Id)
                .build();

        Office.Address address = Office.Address.builder()
                .addressLine1("addressLine1")
                .city("city")
                .postcode("pst_code")
                .build();
        final Office office3 = Office.builder()
                .id(UUID.randomUUID())
                .address(address)
                .code("Office 3 Code")
                .firm(firm2)
                .build();

        UserProfile profile1 = UserProfile.builder().activeProfile(true)
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .legacyUserId(LEGACY_USER_ID)
                .firm(firm)
                .unrestrictedOfficeAccess(true)
                .build();
        UserProfile profile2 = UserProfile.builder()
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .legacyUserId(LEGACY_USER_ID)
                .firm(firm2)
                .unrestrictedOfficeAccess(true)
                .build();
        entraUser.setUserProfiles(Set.of(profile1, profile2));

        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.of(app));
        when(officeRepository.findOfficeByFirm_IdIn(List.of(FIRM_ID))).thenReturn(List.of(office1, office2));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals("microsoft.graph.onTokenIssuanceStartResponseData", response.getData().getOdataType());
        
        // Verify actions
        assertNotNull(response.getData().getActions());
        assertEquals(1, response.getData().getActions().size());
        
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().get(0);
        assertEquals("microsoft.graph.tokenIssuanceStart.provideClaimsForToken", action.getOdataType());
        
        // Verify claims
        Map<String, Object> claims = action.getClaims();
        assertNotNull(claims);
        assertEquals(LEGACY_USER_ID.toString().toUpperCase(), claims.get("USER_NAME"));
        assertEquals(USER_EMAIL, claims.get("USER_EMAIL"));
        assertEquals(List.of(EXTERNAL_ROLE), claims.get("LAA_APP_ROLES"));
        assertThat(((List<String>) claims.get("LAA_ACCOUNTS")).contains(office1.getCode()));
        assertThat(((List<String>) claims.get("LAA_ACCOUNTS")).contains(office2.getCode()));
        assertThat(((List<String>) claims.get("LAA_ACCOUNTS")).contains(office3.getCode()));

        verify(officeRepository).findOfficeByFirm_IdIn(List.of(FIRM_ID));
    }

    @Test
    void enrichClaim_InternalUser() {
        // Arrange
        AppRole internalRole = AppRole.builder()
                .name(INTERNAL_ROLE)
                .app(app)
                .build();
        // Internal users don't have firms
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .appRoles(Set.of(internalRole))
                .legacyUserId(LEGACY_USER_ID)
                .firm(null)
                .userType(UserType.INTERNAL)
                .build();
        entraUser.setUserProfiles(Set.of(userProfile));

        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.of(app));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());
        assertEquals("microsoft.graph.onTokenIssuanceStartResponseData", response.getData().getOdataType());
        
        // Verify actions
        assertNotNull(response.getData().getActions());
        assertEquals(1, response.getData().getActions().size());
        
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().get(0);
        assertEquals("microsoft.graph.tokenIssuanceStart.provideClaimsForToken", action.getOdataType());
        
        // Verify claims
        Map<String, Object> claims = action.getClaims();
        assertNotNull(claims);
        assertEquals(LEGACY_USER_ID.toString().toUpperCase(), claims.get("USER_NAME"));
        assertEquals(USER_EMAIL, claims.get("USER_EMAIL"));
        assertEquals(List.of(INTERNAL_ROLE), claims.get("LAA_APP_ROLES"));
        assertEquals(Collections.emptyList(), claims.get("LAA_ACCOUNTS"));
        
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }

    @Test
    void enrichClaimThrowsException_UserNotFound() {
        // Arrange
        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.empty());

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User not found in database", exception.getMessage());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }

    @Test
    void enrichClaimThrowsException_AppNotFound() {
        // Arrange
        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("Application not found", exception.getMessage());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }

    @Test
    void enrichClaim_UserNoAppAccess_ReturnsUnsuccessfulResponse() {
        // Arrange
        entraUser.setUserProfiles(Collections.emptySet());
        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.of(app));

        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        assertNotNull(response);
        assertEquals(false, response.isSuccess());
        assertEquals(null, response.getData());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }

    @Test
    void enrichClaimThrowsException_ExternalUserWithFirmButNoOffices() {
        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.of(app));
        when(officeRepository.findOfficeByFirm_IdIn(List.of(FIRM_ID))).thenReturn(List.of(office1, office2));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        ClaimEnrichmentResponse.ResponseAction action = response.getData().getActions().get(0);
        Map<String, Object> claims = action.getClaims();

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals(LEGACY_USER_ID.toString().toUpperCase(), claims.get("USER_NAME"));
        assertEquals(USER_EMAIL, claims.get("USER_EMAIL"));
        assertEquals(List.of(EXTERNAL_ROLE), claims.get("LAA_APP_ROLES"));
        assertEquals(List.of(office1.getCode(), office2.getCode()), claims.get("LAA_ACCOUNTS"));

        verify(officeRepository).findOfficeByFirm_IdIn(List.of(FIRM_ID));
    }

    @Test
    void enrichClaimThrowsException_ExternalUserWithNoFirmMapping() {
        // Arrange
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .firm(null)
                .build();
        entraUser.setUserProfiles(Set.of(userProfile));

        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.of(app));

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User has no firm assigned", exception.getMessage());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }

    @Test
    void enrichClaimThrowsException_ExternalUserWithNoFirm() {
        // Arrange
        UserProfile userProfile = UserProfile.builder().activeProfile(true)
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .firm(null)
                .build();
        entraUser.setUserProfiles(Set.of(userProfile));

        when(entraUserRepository.findByEntraOid(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByEntraAppId(anyString())).thenReturn(Optional.of(app));

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User has no firm assigned", exception.getMessage());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }
}
