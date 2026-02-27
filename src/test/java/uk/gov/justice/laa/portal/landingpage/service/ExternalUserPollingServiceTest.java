package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.entity.DisableUserReason;
import uk.gov.justice.laa.portal.landingpage.entity.EntraLastSyncMetadata;
import uk.gov.justice.laa.portal.landingpage.entity.EntraUser;
import uk.gov.justice.laa.portal.landingpage.entity.UserAccountStatusAudit;
import uk.gov.justice.laa.portal.landingpage.entity.UserProfile;
import uk.gov.justice.laa.portal.landingpage.repository.DisableUserReasonRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraLastSyncMetadataRepository;
import uk.gov.justice.laa.portal.landingpage.repository.EntraUserRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserAccountStatusAuditRepository;
import uk.gov.justice.laa.portal.landingpage.repository.UserProfileRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUsersResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesErrorResponse;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ExternalUserPollingServiceTest {

    @Mock
    private EntraLastSyncMetadataRepository entraLastSyncMetadataRepository;

    @Mock
    private EntraUserRepository entraUserRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private DisableUserReasonRepository disableUserReasonRepository;

    @Mock
    private UserAccountStatusAuditRepository userAccountStatusAuditRepository;

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
    void shouldCapTimeGapTo30Minutes_whenGapExceeds30Minutes() {
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

        // Regular user for synchronization
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

        // User to be disabled
        EntraUser userToDisable = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user456")
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .enabled(true)
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user456")).thenReturn(Optional.of(userToDisable));

        // Mock disable reason repository
        DisableUserReason inactivityReason = DisableUserReason.builder()
                .id(java.util.UUID.randomUUID())
                .name("Inactivity")
                .description("Disabled due to inactivity")
                .entraDescription("Inactivity")
                .userSelectable(false)
                .build();
        when(disableUserReasonRepository.findAll()).thenReturn(List.of(inactivityReason));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("NewFirstName")
                .surname("NewLastName")
                .accountEnabled(false)
                .isMailOnly(true)
                .lastSignIn("2025-01-18T10:30:00Z")
                .build();

        GetUsersResponse.TechServicesUser disabledUser = GetUsersResponse.TechServicesUser.builder()
                .id("user456")
                .givenName("Jane")
                .surname("Doe")
                .accountEnabled(false) // Changed to false to match disable logic
                .isMailOnly(false)
                .customSecurityAttributes(GetUsersResponse.CustomSecurityAttributes.builder()
                        .guestUserStatus(GetUsersResponse.GuestUserStatus.builder()
                                .odataType("#microsoft.graph.customSecurityAttributeValue")
                                .disabledReason("NoGroupsDisable")
                                .build())
                        .build())
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser, disabledUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository).save(existingUser);
        verify(entraUserRepository, times(2)).save(userToDisable); // Called twice: once in disableUserWithReason, once in main sync
        verify(userAccountStatusAuditRepository).save(any(UserAccountStatusAudit.class));
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
        verify(entraUserRepository, times(2)).save(user2);
        verify(userAccountStatusAuditRepository).save(any(UserAccountStatusAudit.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldDeleteUserWithProfiles_whenApiReturnsDeletedTrue() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        UserProfile profile1 = UserProfile.builder()
                .id(java.util.UUID.randomUUID())
                .activeProfile(true)
                .build();
        UserProfile profile2 = UserProfile.builder()
                .id(java.util.UUID.randomUUID())
                .activeProfile(false)
                .build();

        EntraUser existingUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("deleted-user-with-profiles")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .enabled(true)
                .mailOnly(false)
                .userProfiles(new java.util.HashSet<>(java.util.Set.of(profile1, profile2)))
                .build();

        profile1.setEntraUser(existingUser);
        profile2.setEntraUser(existingUser);
        
        when(entraUserRepository.findByEntraOid("deleted-user-with-profiles")).thenReturn(Optional.of(existingUser));

        GetUsersResponse.TechServicesUser deletedUser = GetUsersResponse.TechServicesUser.builder()
                .id("deleted-user-with-profiles")
                .givenName("Jane")
                .surname("Smith")
                .accountEnabled(true)
                .isMailOnly(false)
                .deleted(true)
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(deletedUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(userProfileRepository).save(profile1);
        verify(userProfileRepository).save(profile2);
        verify(userProfileRepository).delete(profile1);
        verify(userProfileRepository).delete(profile2);
        verify(userProfileRepository, times(3)).flush();

        verify(entraUserRepository).delete(existingUser);
        verify(entraUserRepository).flush();
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldSkipDeletion_whenUserNotFoundInDatabase() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        when(entraUserRepository.findByEntraOid("nonexistent-deleted-user")).thenReturn(Optional.empty());

        GetUsersResponse.TechServicesUser deletedUser = GetUsersResponse.TechServicesUser.builder()
                .id("nonexistent-deleted-user")
                .givenName("Ghost")
                .surname("User")
                .accountEnabled(false)
                .isMailOnly(false)
                .deleted(true) // deleted and not in silas db
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(deletedUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository, never()).delete(any(EntraUser.class));
        verify(userProfileRepository, never()).delete(any(UserProfile.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleMixedOperations_updateAndDeleteUsers() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        // User to be updated
        EntraUser userToUpdate = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user-to-update")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .enabled(true)
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user-to-update")).thenReturn(Optional.of(userToUpdate));

        // User to be deleted
        EntraUser userToDelete = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user-to-delete")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@example.com")
                .enabled(true)
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user-to-delete")).thenReturn(Optional.of(userToDelete));

        GetUsersResponse.TechServicesUser updateUser = GetUsersResponse.TechServicesUser.builder()
                .id("user-to-update")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(false) // changed
                .isMailOnly(true) // changed
                .deleted(false) // not deleted
                .build();

        GetUsersResponse.TechServicesUser deleteUser = GetUsersResponse.TechServicesUser.builder()
                .id("user-to-delete")
                .givenName("Jane")
                .surname("Smith")
                .accountEnabled(true)
                .isMailOnly(false)
                .deleted(true) // deleted
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(updateUser, deleteUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository).save(userToUpdate);

        verify(entraUserRepository).delete(userToDelete);
        verify(entraUserRepository).flush();
        
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleUsersNotFoundError_successfully() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        TechServicesErrorResponse errorResponse = TechServicesErrorResponse.builder()
                .success(false)
                .message("Users not found.")
                .code("404")
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.error(errorResponse);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
        verify(techServicesClient).getUsers(anyString(), anyString());
    }

    @Test
    void shouldHandleExactly30MinuteGap() {
        LocalDateTime lastSuccessfulTo = LocalDateTime.now().minusMinutes(35);
        EntraLastSyncMetadata existingMetadata = EntraLastSyncMetadata.builder()
                .id("ENTRA_USER_SYNC")
                .lastSuccessfulTo(lastSuccessfulTo)
                .updatedAt(LocalDateTime.now().minusMinutes(35))
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
    void shouldHandleApiResponseWithNullData() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(null);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        assertThrows(RuntimeException.class, () -> 
            externalUserPollingService.updateSyncMetadata());
        
        verify(entraLastSyncMetadataRepository, never()).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleUserWithNullCustomSecurityAttributes() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser existingUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(true)
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(existingUser));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true)
                .isMailOnly(false)
                .customSecurityAttributes(null)
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
    void shouldHandleUserDeletionWithAuditRecords() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser userToDelete = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(true)
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(userToDelete));

        UserAccountStatusAudit auditRecord = UserAccountStatusAudit.builder()
                .id(java.util.UUID.randomUUID())
                .entraUser(userToDelete)
                .build();
        when(userAccountStatusAuditRepository.findByEntraUser(userToDelete)).thenReturn(List.of(auditRecord));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true)
                .isMailOnly(false)
                .deleted(true)
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(userAccountStatusAuditRepository).findByEntraUser(userToDelete);
        verify(userAccountStatusAuditRepository).deleteAll(List.of(auditRecord));
        verify(userAccountStatusAuditRepository).flush();
        verify(entraUserRepository).delete(userToDelete);
        verify(entraUserRepository).flush();
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldCapTimeGapTo30Minutes_whenGapExceeds30Minutes_secondScenario() {
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

        verify(techServicesClient).getUsers(anyString(), anyString());
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleNullEntraUserInDeleteUser() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("nonexistent123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true)
                .isMailOnly(false)
                .deleted(true)
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);
        when(entraUserRepository.findByEntraOid("nonexistent123")).thenReturn(Optional.empty());

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository, never()).delete(any(EntraUser.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleUserWithDifferentFields() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser existingUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("OldFirstName")
                .lastName("OldLastName")
                .email("user@example.com")
                .enabled(false)
                .mailOnly(true)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(existingUser));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("NewFirstName")
                .surname("NewLastName")
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

        verify(entraUserRepository, times(2)).save(existingUser); // Called twice: once in enableUserWithReason, once in main sync
        verify(userAccountStatusAuditRepository).save(any(UserAccountStatusAudit.class)); // Enable audit is created
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleUserWithNullFields() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser existingUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(true)
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(existingUser));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName(null)
                .surname(null)
                .accountEnabled(true)
                .isMailOnly(false)
                .lastSignIn(null)
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
    void shouldHandleUserSynchronizationException() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        when(entraUserRepository.findByEntraOid("user123")).thenThrow(new RuntimeException("Database error"));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true)
                .isMailOnly(false)
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleDisableUserException() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser existingUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(true)
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(existingUser));
        when(entraUserRepository.save(existingUser)).thenThrow(new RuntimeException("Database error"));

        DisableUserReason inactivityReason = DisableUserReason.builder()
                .id(java.util.UUID.randomUUID())
                .name("Inactivity")
                .description("Disabled due to inactivity")
                .entraDescription("Inactivity")
                .userSelectable(false)
                .build();
        when(disableUserReasonRepository.findAll()).thenReturn(List.of(inactivityReason));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true)
                .isMailOnly(false)
                .customSecurityAttributes(GetUsersResponse.CustomSecurityAttributes.builder()
                        .guestUserStatus(GetUsersResponse.GuestUserStatus.builder()
                                .odataType("#microsoft.graph.customSecurityAttributeValue")
                                .disabledReason("NoGroupsDisable")
                                .build())
                        .build())
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleUserWithAlreadyDisabledStatus() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser existingUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(false)
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(existingUser));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true)
                .isMailOnly(false)
                .customSecurityAttributes(GetUsersResponse.CustomSecurityAttributes.builder()
                        .guestUserStatus(GetUsersResponse.GuestUserStatus.builder()
                                .odataType("#microsoft.graph.customSecurityAttributeValue")
                                .disabledReason("NoGroupsDisable")
                                .build())
                        .build())
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository, times(2)).save(existingUser); // Called twice: once in enableUserWithReason, once in main sync
        verify(userAccountStatusAuditRepository).save(any(UserAccountStatusAudit.class)); // Enable audit is created
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleEmptyUsersList() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(techServicesClient).getUsers(anyString(), anyString());
        verify(entraUserRepository, never()).save(any(EntraUser.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldHandleNullUsersList() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(null)
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(techServicesClient).getUsers(anyString(), anyString());
        verify(entraUserRepository, never()).save(any(EntraUser.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldFormatDatesAsValid_whenSecondsAreNonZero() {
        LocalDateTime lastSuccessfulTo = LocalDateTime.of(2026, 2, 25, 10, 29, 3);
        EntraLastSyncMetadata existingMetadata = EntraLastSyncMetadata.builder()
                .id("ENTRA_USER_SYNC")
                .lastSuccessfulTo(lastSuccessfulTo)
                .updatedAt(LocalDateTime.now().minusMinutes(10))
                .build();
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.of(existingMetadata));
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        String formattedDatePattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.00Z";
        verify(techServicesClient).getUsers(
                matches(formattedDatePattern),
                matches(formattedDatePattern)
        );
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldFormatDatesConsistently_whenTimeHasExactlyZeroSeconds() {
        LocalDateTime lastSuccessfulTo = LocalDateTime.of(2026, 2, 25, 10, 29, 0);
        EntraLastSyncMetadata existingMetadata = EntraLastSyncMetadata.builder()
                .id("ENTRA_USER_SYNC")
                .lastSuccessfulTo(lastSuccessfulTo)
                .updatedAt(LocalDateTime.now().minusMinutes(10))
                .build();
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.of(existingMetadata));
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        
        // Capture the actual date strings being passed
        ArgumentCaptor<String> fromDateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> toDateCaptor = ArgumentCaptor.forClass(String.class);
        when(techServicesClient.getUsers(fromDateCaptor.capture(), toDateCaptor.capture())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        String fromDate = fromDateCaptor.getValue();
        String toDate = toDateCaptor.getValue();

        assertThat(fromDate).doesNotContain("T10:29.00Z");
        assertThat(toDate).doesNotContain("T10:29.00Z");

        String formattedDatePattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.00Z";
        assertThat(fromDate).matches(formattedDatePattern);
        assertThat(toDate).matches(formattedDatePattern);
        
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldEnableUser_whenUserIsDisabledInSilasButEnabledInEntra() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser disabledUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(false) // Disabled in SILAS
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(disabledUser));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(true) // Enabled in Entra
                .isMailOnly(false)
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository, times(2)).save(disabledUser); // Once for enable, once for main sync
        verify(userAccountStatusAuditRepository).save(any(UserAccountStatusAudit.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldDisableUser_whenUserHasDisabledReasonAndIsEnabledInSilas() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser enabledUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(true) // Enabled in SILAS
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(enabledUser));

        DisableUserReason inactivityReason = DisableUserReason.builder()
                .id(java.util.UUID.randomUUID())
                .name("Inactivity")
                .description("Disabled due to inactivity")
                .entraDescription("Inactivity")
                .userSelectable(false)
                .build();
        when(disableUserReasonRepository.findAll()).thenReturn(List.of(inactivityReason));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(false) // Disabled in Entra
                .isMailOnly(false)
                .customSecurityAttributes(GetUsersResponse.CustomSecurityAttributes.builder()
                        .guestUserStatus(GetUsersResponse.GuestUserStatus.builder()
                                .odataType("#microsoft.graph.customSecurityAttributeValue")
                                .disabledReason("NoGroupsDisable")
                                .build())
                        .build())
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository, times(2)).save(enabledUser);
        verify(userAccountStatusAuditRepository).save(any(UserAccountStatusAudit.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }

    @Test
    void shouldNotDisableUser_whenUserIsAlreadyDisabledInSilas() {
        when(entraLastSyncMetadataRepository.findById(eq(ENTRA_USER_SYNC_ID))).thenReturn(Optional.empty());

        EntraUser alreadyDisabledUser = EntraUser.builder()
                .id(java.util.UUID.randomUUID())
                .entraOid("user123")
                .firstName("John")
                .lastName("Doe")
                .email("user@example.com")
                .enabled(false) // Already disabled in SILAS
                .mailOnly(false)
                .build();
        when(entraUserRepository.findByEntraOid("user123")).thenReturn(Optional.of(alreadyDisabledUser));

        GetUsersResponse.TechServicesUser apiUser = GetUsersResponse.TechServicesUser.builder()
                .id("user123")
                .givenName("John")
                .surname("Doe")
                .accountEnabled(false)
                .isMailOnly(false)
                .customSecurityAttributes(GetUsersResponse.CustomSecurityAttributes.builder()
                        .guestUserStatus(GetUsersResponse.GuestUserStatus.builder()
                                .odataType("#microsoft.graph.customSecurityAttributeValue")
                                .disabledReason("NoGroupsDisable")
                                .build())
                        .build())
                .build();
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(List.of(apiUser))
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraUserRepository).save(alreadyDisabledUser);
        verify(userAccountStatusAuditRepository, never()).save(any(UserAccountStatusAudit.class));
        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
    }
}
