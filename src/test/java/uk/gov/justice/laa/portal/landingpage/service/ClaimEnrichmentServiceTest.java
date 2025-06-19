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
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.exception.ClaimEnrichmentException;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimEnrichmentServiceTest {

    private static final String USER_PRINCIPAL = "test@example.com";
    private static final String APP_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String APP_NAME = "Test App";
    private static final String ROLE_NAME = "USER_ROLE";

    @Mock
    private EntraUserRepository entraUserRepository;
    @Mock
    private AppRepository appRepository;
    @InjectMocks
    private ClaimEnrichmentService claimEnrichmentService;
    private ClaimEnrichmentRequest request;
    private EntraUser entraUser;
    private App app;

    @BeforeEach
    void setUp() {
        // Setup request
        EntraUserInfo userInfo = EntraUserInfo.builder()
                .userPrincipalName(USER_PRINCIPAL)
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

        // Setup app registration
        AppRegistration appRegistration = AppRegistration.builder()
                .id(UUID.fromString(APP_ID))
                .build();
        
        // Setup app
        app = App.builder()
                .id(UUID.randomUUID())
                .appRegistration(appRegistration)
                .name(APP_NAME)
                .build();

        // Setup app role
        AppRole appRole = AppRole.builder()
                .name(ROLE_NAME)
                .app(app)
                .build();

        // Setup user profile
        UserProfile userProfile = UserProfile.builder()
                .appRoles(Set.of(appRole))
                .build();

        // Setup user with proper app registration
        entraUser = EntraUser.builder()
                .id(UUID.randomUUID())
                .userName(USER_PRINCIPAL)
                .userAppRegistrations(Set.of(appRegistration))
                .userProfiles(Set.of(userProfile))
                .build();
    }

    @Test
    void enrichClaim_Success() {
        // Arrange
        when(entraUserRepository.findByUserName(USER_PRINCIPAL)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByAppRegistrationId(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));

        // Act
        ClaimEnrichmentResponse response = claimEnrichmentService.enrichClaim(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(APP_NAME, response.getAppName());
        assertEquals(Set.of(ROLE_NAME), response.getRoles());
        assertEquals("Access granted to " + APP_NAME, response.getMessage());
    }

    @Test
    void enrichClaim_UserNotFound() {
        // Arrange
        when(entraUserRepository.findByUserName(USER_PRINCIPAL)).thenReturn(Optional.empty());

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User not found in database", exception.getMessage());
    }

    @Test
    void enrichClaim_AppNotFound() {
        // Arrange
        when(entraUserRepository.findByUserName(USER_PRINCIPAL)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByAppRegistrationId(any())).thenReturn(Optional.empty());

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("Application not found", exception.getMessage());
    }

    @Test
    void enrichClaim_UserNoAppAccess() {
        // Arrange
        entraUser.setUserAppRegistrations(Collections.emptySet()); // Remove app access
        when(entraUserRepository.findByUserName(USER_PRINCIPAL)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByAppRegistrationId(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User does not have access to this application", exception.getMessage());
    }

    @Test
    void enrichClaim_UserNoRoles() {
        // Arrange
        entraUser.setUserProfiles(Collections.emptySet()); // Remove all roles
        when(entraUserRepository.findByUserName(USER_PRINCIPAL)).thenReturn(Optional.of(entraUser));
        when(appRepository.findByAppRegistrationId(UUID.fromString(APP_ID))).thenReturn(Optional.of(app));

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> claimEnrichmentService.enrichClaim(request)
        );
        assertEquals("User has no roles assigned for this application", exception.getMessage());
    }
}
