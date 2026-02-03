package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.entity.EntraLastSyncMetadata;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.repository.EntraLastSyncMetadataRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUsersResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalUserPollingServiceTest {

    @Mock
    private EntraLastSyncMetadataRepository entraLastSyncMetadataRepository;

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    private TechServicesClient techServicesClient;

    @InjectMocks
    private ExternalUserPollingService externalUserPollingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(externalUserPollingService, "bufferMinutes", 5);
    }

    private static final String ENTRA_USER_SYNC_ID = "ENTRA_USER_SYNC";

    @Test
    void shouldCreateNewMetadata_whenNoExistingMetadata() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
        verify(techServicesClient).getUsers(anyString(), anyString());
    }

    @Test
    void shouldUpdateExistingMetadata_whenMetadataExists() {
        LocalDateTime lastSuccessfulTo = LocalDateTime.now().minusHours(2);
        EntraLastSyncMetadata existingMetadata = EntraLastSyncMetadata.builder()
                .id("ENTRA_USER_SYNC")
                .lastSuccessfulTo(lastSuccessfulTo)
                .updatedAt(LocalDateTime.now().minusHours(2))
                .build();
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.of(existingMetadata));
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
        verify(techServicesClient).getUsers(anyString(), anyString());
    }

    @Test
    void shouldUseBufferTime_whenCalculatingFromTime() {
        LocalDateTime lastSuccessfulTo = LocalDateTime.now().minusMinutes(30);
        EntraLastSyncMetadata existingMetadata = EntraLastSyncMetadata.builder()
                .id("ENTRA_USER_SYNC")
                .lastSuccessfulTo(lastSuccessfulTo)
                .updatedAt(LocalDateTime.now().minusMinutes(30))
                .build();
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.of(existingMetadata));
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(techServicesClient).getUsers(anyString(), anyString());
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldCapTimeGapToOneHour_whenGapExceedsSixtyMinutes() {
        LocalDateTime lastSuccessfulTo = LocalDateTime.now().minusHours(3); // 3 hours ago
        EntraLastSyncMetadata existingMetadata = EntraLastSyncMetadata.builder()
                .id("ENTRA_USER_SYNC")
                .lastSuccessfulTo(lastSuccessfulTo)
                .updatedAt(LocalDateTime.now().minusHours(3))
                .build();
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.of(existingMetadata));
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(techServicesClient).getUsers(anyString(), anyString());
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldProcessUsersSuccessfully_whenApiReturnsUsers() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        GetUsersResponse.TechServicesUser user1 = GetUsersResponse.TechServicesUser.builder()
                .id("user1")
                .displayName("John Doe")
                .mail("john@example.com")
                .build();
        GetUsersResponse.TechServicesUser user2 = GetUsersResponse.TechServicesUser.builder()
                .id("user2")
                .displayName("Jane Smith")
                .mail("jane@example.com")
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(user1, user2))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
        verify(techServicesClient).getUsers(anyString(), anyString());
    }

    @Test
    void shouldThrowException_whenApiCallFails() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        TechServicesErrorResponse errorResponse = TechServicesErrorResponse.builder()
                .success(false)
                .message("API Error")
                .code("500")
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.error(errorResponse);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        assertThrows(RuntimeException.class, () -> externalUserPollingService.updateSyncMetadata());
        verify(entraLastSyncMetadataRepository, never()).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldNotUpdateDatabase_whenApiCallFails() {
        LocalDateTime lastSuccessfulTo = LocalDateTime.now().minusHours(1);
        EntraLastSyncMetadata existingMetadata = EntraLastSyncMetadata.builder()
                .id("ENTRA_USER_SYNC")
                .lastSuccessfulTo(lastSuccessfulTo)
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.of(existingMetadata));
        
        TechServicesErrorResponse errorResponse = TechServicesErrorResponse.builder()
                .success(false)
                .message("Service Unavailable")
                .code("503")
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.error(errorResponse);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        assertThrows(RuntimeException.class, () -> externalUserPollingService.updateSyncMetadata());
    }

    @Test
    void shouldHandleNullUserList_gracefully() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(null)
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
        verify(techServicesClient).getUsers(anyString(), anyString());
    }

    @Test
    void shouldUseOneHourDefault_whenNoExistingMetadata() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(techServicesClient).getUsers(anyString(), anyString());
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldSynchronizeUserData_whenApiReturnsUsersWithData() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser existingUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("OldFirstName")
                .lastName("OldLastName")
                .email("user@example.com")
                .enabled(true)
                .mailOnly(false)
                .lastLoginDate(LocalDateTime.of(2025, 1, 1, 10, 0))
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(existingUser));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("NewFirstName")
                .surname("NewLastName")
                .accountEnabled(false)
                .isMailOnly(true)
                .lastSignIn("2025-01-18T10:30:00Z")
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository).save(existingUser);
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldNotUpdateLastLoginDate_whenApiReturnsNullLastSignIn() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        LocalDateTime existingLoginDate = LocalDateTime.of(2025, 1, 1, 10, 0);
        EntraUser existingUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(true)
                .mailOnly(false)
                .lastLoginDate(existingLoginDate)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(existingUser));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true)
                .isMailOnly(false)
                .lastSignIn(null) // null lastSignIn
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository).save(existingUser);
    }

    @Test
    void shouldSkipUserUpdate_whenUserNotFoundInDatabase() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        when(entraUserRepository.findByEntraOid("nonexistent123")).thenReturn(Optional.empty());

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("nonexistent123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true)
                .isMailOnly(false)
                .lastSignIn("2025-01-18T10:30:00Z")
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository, never()).save(any(EntraUser.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleMultipleUsers_inSingleSyncOperation() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        // Setup multiple existing users
        EntraUser user1 = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user1")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .enabled(true)
                .mailOnly(false)
                .build();
        EntraUser user2 = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user2")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .enabled(false)
                .mailOnly(true)
                .build();
        
        when(entraUserRepository.findByEntraOid("user1")).thenReturn(Optional.of(user1));
        when(entraUserRepository.findByEntraOid("user2")).thenReturn(Optional.of(user2));

        GetUsersResponse.TechServicesUser apiUser1 = GetUsersResponse.TechServicesUser.builder()
                .id("user1")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(false) // changed
                .isMailOnly(true) // changed
                .lastSignIn("2025-01-18T10:30:00Z")
                .build();
        GetUsersResponse.TechServicesUser apiUser2 = GetUsersResponse.TechServicesUser.builder()
                .id("user2")
                .givenName("Jane")
                .surname("Smith")
                .accountEnabled(true) // changed
                .isMailOnly(false) // changed
                .lastSignIn("2025-01-19T15:45:00Z")
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser1, apiUser2))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository).save(user1);
        verify(entraUserRepository).save(user2);
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }
}
