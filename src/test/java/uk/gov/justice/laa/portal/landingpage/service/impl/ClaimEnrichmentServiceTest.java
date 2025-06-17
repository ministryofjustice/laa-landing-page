package uk.gov.justice.laa.portal.landingpage.service.impl;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentRequest;
import uk.gov.justice.laa.portal.landingpage.dto.ClaimEnrichmentResponse;
import uk.gov.justice.laa.portal.landingpage.entity.App;
import uk.gov.justice.laa.portal.landingpage.entity.AppRegistration;
import uk.gov.justice.laa.portal.landingpage.entity.AppRole;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.exception.ClaimEnrichmentException;
import uk.gov.justice.laa.portal.landingpage.repository.AppRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.service.EntraIdService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClaimEnrichmentServiceTest {

    @Mock
    private EntraIdService entraIdService;

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    private AppRepository appRepository;

    @InjectMocks
    private ClaimEnrichmentService service;

    private ClaimEnrichmentRequest request;
    private User mockUser;
    private EntraUser mockEntraUser;
    private App mockApp;
    private AppRegistration mockAppRegistration;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        request = new ClaimEnrichmentRequest();
        request.setUserId("test-user");
        request.setToken("test-token");
        request.setTargetAppId("123e4567-e89b-12d3-a456-426614174000");

        mockUser = new User();
        mockUser.setUserPrincipalName("test@example.com");

        mockAppRegistration = AppRegistration.builder()
            .id(UUID.fromString(request.getTargetAppId()))
            .build();

        mockEntraUser = EntraUser.builder()
                .email("test@example.com")
                .userAppRegistrations(Set.of(mockAppRegistration))
                .build()
        ;

        mockApp = App.builder()
                        .appRegistration(mockAppRegistration)
                        .name("Test App")
                        .build();

        Set<AppRole> roles = new HashSet<>();
        AppRole role = AppRole.builder()
                .name("ROLE_USER")
                .build();

        roles.add(role);
        mockApp.setAppRoles(roles);
    }

    @Test
    void enrichClaims_Success() {
        // Arrange
        when(entraIdService.getUserByPrincipalName(request.getToken())).thenReturn(mockUser);
        when(entraUserRepository.findByEmail(mockUser.getUserPrincipalName())).thenReturn(Optional.of(mockEntraUser));
        when(entraIdService.getUserGroupMemberships(request.getToken())).thenReturn(List.of("ROLE_USER"));
        when(appRepository.findByAppRegistrationId(any(UUID.class))).thenReturn(Optional.of(mockApp));

        // Act
        ClaimEnrichmentResponse response = service.enrichClaims(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getRoles());
        assertTrue(response.getRoles().contains("ROLE_USER"));
        assertNotNull(response.getMessage());

        verify(entraIdService).getUserByPrincipalName(request.getToken());
        verify(entraUserRepository).findByEmail(mockUser.getUserPrincipalName());
        verify(entraIdService).getUserGroupMemberships(request.getToken());
        verify(appRepository).findByAppRegistrationId(any(UUID.class));
    }

    @Test
    void enrichClaims_UserNotFoundInEntraId() {
        // Arrange
        when(entraIdService.getUserByPrincipalName(request.getToken())).thenReturn(null);

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> service.enrichClaims(request)
        );
        assertEquals("User not found in Entra ID", exception.getMessage());
    }

    @Test
    void enrichClaims_UserNotFoundInDatabase() {
        // Arrange
        when(entraIdService.getUserByPrincipalName(request.getToken())).thenReturn(mockUser);
        when(entraUserRepository.findByEmail(mockUser.getUserPrincipalName())).thenReturn(Optional.empty());

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> service.enrichClaims(request)
        );
        assertEquals("User not found in database", exception.getMessage());
    }

    @Test
    void enrichClaims_AppNotFound() {
        // Arrange
        when(entraIdService.getUserByPrincipalName(request.getToken())).thenReturn(mockUser);
        when(entraUserRepository.findByEmail(mockUser.getUserPrincipalName())).thenReturn(Optional.of(mockEntraUser));
        when(entraIdService.getUserGroupMemberships(request.getToken())).thenReturn(List.of("ROLE_USER"));
        when(appRepository.findByAppRegistrationId(any(UUID.class))).thenReturn(Optional.empty());

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> service.enrichClaims(request)
        );
        assertEquals("Application not found", exception.getMessage());
    }

    @Test
    void enrichClaims_UserNoAccessToApp() {
        // Arrange
        mockEntraUser.setUserAppRegistrations(new HashSet<>()); // Empty set = no access
        when(entraIdService.getUserByPrincipalName(request.getToken())).thenReturn(mockUser);
        when(entraUserRepository.findByEmail(mockUser.getUserPrincipalName())).thenReturn(Optional.of(mockEntraUser));
        when(entraIdService.getUserGroupMemberships(request.getToken())).thenReturn(List.of("ROLE_USER"));
        when(appRepository.findByAppRegistrationId(any(UUID.class))).thenReturn(Optional.of(mockApp));

        // Act & Assert
        ClaimEnrichmentException exception = assertThrows(
            ClaimEnrichmentException.class,
            () -> service.enrichClaims(request)
        );
        assertEquals("User does not have access to this application", exception.getMessage());
    }
}
