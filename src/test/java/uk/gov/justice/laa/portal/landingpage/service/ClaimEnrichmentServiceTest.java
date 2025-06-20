package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.dto.EntraApplicationInfo;
import uk.gov.justice.laa.portal.landingpage.dto.EntraClaimData;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserInfo;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimEnrichmentServiceTest {

    private static final String USER_ENTRA_ID = "entra-123";
    private static final String USER_EMAIL = "test@example.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String APP_ID = "123e4567-e89b-12d3-a456-426614174000";
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
        // Setup request
        EntraUserInfo userInfo = EntraUserInfo.builder()
                .id(USER_ENTRA_ID)
                .build();
        EntraApplicationInfo appInfo = EntraApplicationInfo.builder()
                .id(APP_ID)
                .build();
        EntraClaimData data = EntraClaimData.builder()
                .user(userInfo)
                .application(appInfo)
                .build();
        request = ClaimEnrichmentRequest.builder()
                .data(data)
                .build();

        // Setup app
        app = App.builder()
                .id(UUID.fromString(APP_ID))
                .name(APP_NAME)
                .build();

        // Setup firm
        firm = Firm.builder()
                .id(FIRM_ID)
                .build();

        // Setup offices
        office1 = Office.builder()
                .id(OFFICE_ID_1)
                .firm(firm)
                .build();

        office2 = Office.builder()
                .id(OFFICE_ID_2)
                .firm(firm)
                .build();

        // Setup app role
        AppRole appRole = AppRole.builder()
                .app(app)
                .name(EXTERNAL_ROLE)
                .build();

        // Setup user profile with firm and role
        UserProfile userProfile = UserProfile.builder()
                .firm(firm)
                .appRoles(Set.of(appRole))
                .build();

        // Setup user with proper app registration and email
        entraUser = EntraUser.builder()
                .id(USER_ID)
                .entraId(USER_ENTRA_ID)
                .email(USER_EMAIL)
                .userProfiles(Set.of(userProfile))
                .build();
    }

    @Test
    void enrichClaim_Success() {
        // Arrange
        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findById(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));
        when(officeRepository.findOfficeByFirm_IdIn(List.of(FIRM_ID)))
                .thenReturn(List.of(office1, office2));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(APP_NAME, response.getAppName());
        assertEquals(Set.of(EXTERNAL_ROLE), response.getRoles());
        assertEquals(USER_ID.toString(), response.getUserId());
        assertEquals(USER_EMAIL, response.getEmail());
        assertEquals(OFFICE_ID_1 + ":" + OFFICE_ID_2, response.getOfficeIds());
        assertEquals("Access granted to " + APP_NAME, response.getMessage());
        verify(officeRepository).findOfficeByFirm_IdIn(List.of(FIRM_ID));
    }

    @Test
    void enrichClaim_UserWithMultipleFirmsAndOffices() {
        // Arrange
        UUID firm2Id = UUID.randomUUID();
        Firm firm2 = Firm.builder()
                .id(firm2Id)
                .build();

        final Office office3 = Office.builder()
                .id(UUID.randomUUID())
                .name("Office 3")
                .firm(firm2)
                .build();

        UserProfile profile1 = UserProfile.builder()
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .firm(firm)
                .build();
        UserProfile profile2 = UserProfile.builder()
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .firm(firm2)
                .build();
        entraUser.setUserProfiles(Set.of(profile1, profile2));

        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findById(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));
        when(officeRepository.findOfficeByFirm_IdIn(List.of(FIRM_ID)))
                .thenReturn(List.of(office1, office2));
        when(officeRepository.findOfficeByFirm_IdIn(List.of(firm2Id)))
                .thenReturn(List.of(office3));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertThat(response.getOfficeIds().split(":"))
            .containsExactlyInAnyOrder(
                OFFICE_ID_1.toString(),
                OFFICE_ID_2.toString(),
                office3.getId().toString()
            );
        verify(officeRepository).findOfficeByFirm_IdIn(List.of(FIRM_ID));
        verify(officeRepository).findOfficeByFirm_IdIn(List.of(firm2Id));
    }

    @Test
    void enrichClaim_InternalUser() {
        // Arrange
        AppRole internalRole = AppRole.builder()
                .name(INTERNAL_ROLE)
                .app(app)
                .build();
        // Internal users don't have firms
        UserProfile userProfile = UserProfile.builder()
                .appRoles(Set.of(internalRole))
                .firm(null)
                .userType(UserType.INTERNAL)
                .build();
        entraUser.setUserProfiles(Set.of(userProfile));

        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findById(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("", response.getOfficeIds());
        assertEquals(Set.of(INTERNAL_ROLE), response.getRoles());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }

    @Test
    void enrichClaimThrowsException_UserNotFound() {
        // Arrange
        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.empty());

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
        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("Application not found", exception.getMessage());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }

    @Test
    void enrichClaimThrowsException_UserNoAppAccess() {
        // Arrange
        entraUser.setUserProfiles(Collections.emptySet());
        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findById(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User does not have access to this application", exception.getMessage());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }

    @Test
    void enrichClaimThrowsException_ExternalUserWithFirmButNoOffices() {
        // Arrange
        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findById(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));
        when(officeRepository.findOfficeByFirm_IdIn(List.of(FIRM_ID)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User has no offices assigned for this firm", exception.getMessage());
        verify(officeRepository).findOfficeByFirm_IdIn(List.of(FIRM_ID));
    }

    @Test
    void enrichClaimThrowsException_ExternalUserWithNoFirmMapping() {
        // Arrange
        UserProfile userProfile = UserProfile.builder()
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .firm(null)
                .build();
        entraUser.setUserProfiles(Set.of(userProfile));
        
        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findById(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));

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
        UserProfile userProfile = UserProfile.builder()
                .appRoles(Set.of(AppRole.builder().name(EXTERNAL_ROLE).app(app).build()))
                .firm(null)
                .build();
        entraUser.setUserProfiles(Set.of(userProfile));
        
        when(entraUserRepository.findByEntraId(USER_ENTRA_ID)).thenReturn(Optional.of(entraUser));
        when(appRepository.findById(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User has no firm assigned", exception.getMessage());
        verify(officeRepository, never()).findOfficeByFirm_IdIn(any());
    }
}
