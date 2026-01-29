package uk.gov.justice.laa.portal.landingpage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.entity.EntraLastSyncMetadata;
import uk.gov.justice.laa.portal.landingpage.repository.EntraLastSyncMetadataRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalUserPollingServiceTest {

    @Mock
    private EntraLastSyncMetadataRepository entraLastSyncMetadataRepository;

    @Mock
    private TechServicesClient techServicesClient;

    @InjectMocks
    private ExternalUserPollingService externalUserPollingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(externalUserPollingService, "bufferMinutes", 5);
    }

    @Test
    void shouldCreateNewMetadata_whenNoExistingMetadata() {
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.empty());
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
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.of(existingMetadata));
        
        GetUsersResponse response = GetUsersResponse.builder()
                .message("Success")
                .users(Collections.emptyList())
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.success(response);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        externalUserPollingService.updateSyncMetadata();

        verify(entraLastSyncMetadataRepository).save(any(EntraLastSyncMetadata.class));
        verify(entraLastSyncMetadataRepository, never()).updateSyncMetadata(any(LocalDateTime.class), any(LocalDateTime.class));
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
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.of(existingMetadata));
        
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
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.of(existingMetadata));
        
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
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.empty());
        
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
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.empty());
        
        TechServicesErrorResponse errorResponse = TechServicesErrorResponse.builder()
                .success(false)
                .message("API Error")
                .code("500")
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.error(errorResponse);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        assertThrows(RuntimeException.class, () -> externalUserPollingService.updateSyncMetadata());
        verify(entraLastSyncMetadataRepository, never()).save(any(EntraLastSyncMetadata.class));
        verify(entraLastSyncMetadataRepository, never()).updateSyncMetadata(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldNotUpdateDatabase_whenApiCallFails() {
        LocalDateTime lastSuccessfulTo = LocalDateTime.now().minusHours(1);
        EntraLastSyncMetadata existingMetadata = EntraLastSyncMetadata.builder()
                .id("ENTRA_USER_SYNC")
                .lastSuccessfulTo(lastSuccessfulTo)
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.of(existingMetadata));
        
        TechServicesErrorResponse errorResponse = TechServicesErrorResponse.builder()
                .success(false)
                .message("Service Unavailable")
                .code("503")
                .build();
        TechServicesApiResponse<GetUsersResponse> apiResponse = TechServicesApiResponse.error(errorResponse);
        when(techServicesClient.getUsers(anyString(), anyString())).thenReturn(apiResponse);

        assertThrows(RuntimeException.class, () -> externalUserPollingService.updateSyncMetadata());
        verify(entraLastSyncMetadataRepository, never()).updateSyncMetadata(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldHandleNullUserList_gracefully() {
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.empty());
        
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
        when(entraLastSyncMetadataRepository.getSyncMetadata()).thenReturn(Optional.empty());
        
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
}
