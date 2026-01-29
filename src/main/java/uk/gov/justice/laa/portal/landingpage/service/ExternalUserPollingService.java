package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.laa.portal.landingpage.entity.EntraLastSyncMetadata;
import uk.gov.justice.laa.portal.landingpage.repository.EntraLastSyncMetadataRepository;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUsersResponse;
import uk.gov.justice.laa.portal.landingpage.techservices.TechServicesApiResponse;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalUserPollingService {

    private static final String ENTRA_USER_SYNC_ID = "ENTRA_USER_SYNC";
    
    private final EntraLastSyncMetadataRepository entraLastSyncMetadataRepository;
    private final TechServicesClient techServicesClient;
    
    @Value("${app.entra.sync.buffer.minutes:5}")
    private int bufferMinutes;

    /**
     * Updates the sync metadata with current timestamp and last successful sync time.
     * This method is called by the scheduler to track sync operations.
     * It calls the Tech Services API to get users and updates the metadata accordingly.
     * Only updates the database on successful API calls.
     */
    @Transactional
    public void updateSyncMetadata() {
        LocalDateTime toTime = LocalDateTime.now();
        
        try {
            Optional<EntraLastSyncMetadata> existingMetadata = entraLastSyncMetadataRepository.getSyncMetadata();
            
            LocalDateTime fromTime;
            if (existingMetadata.isPresent() && existingMetadata.get().getLastSuccessfulTo() != null) {
                fromTime = existingMetadata.get().getLastSuccessfulTo().minusMinutes(bufferMinutes);
            } else {
                fromTime = toTime.minusHours(1);
            }

            // Cap the time gap to maximum 1 hour if difference is more than 60 minutes
            long minutesBetween = ChronoUnit.MINUTES.between(fromTime, toTime);
            if (minutesBetween > 60) {
                fromTime = toTime.minus(1, ChronoUnit.HOURS);
            }

            String fromDateTime = fromTime + "Z";
            String toDateTime = toTime + "Z";
            
            log.debug("Calling Tech Services API to get users from {} to {} (gap: {} minutes)", 
                     fromDateTime, toDateTime, ChronoUnit.MINUTES.between(fromTime, toTime));
            TechServicesApiResponse<GetUsersResponse> response = techServicesClient.getUsers(fromDateTime, toDateTime);
            
            if (response.isSuccess()) {
                GetUsersResponse usersResponse = response.getData();
                int userCount = usersResponse.getUser() != null ? usersResponse.getUser().size() : 0;
                log.info("Successfully retrieved {} users from Tech Services", userCount);

                LocalDateTime syncCompletedTime = LocalDateTime.now();

                if (existingMetadata.isPresent()) {
                    entraLastSyncMetadataRepository.updateSyncMetadata(syncCompletedTime, toTime);
                    log.info("Successfully updated EntraLastSyncMetadata: updatedAt={}, lastSuccessfulTo={}", syncCompletedTime, toTime);
                } else {
                    // Create new record if it doesn't exist
                    EntraLastSyncMetadata newMetadata = EntraLastSyncMetadata.builder()
                            .id(ENTRA_USER_SYNC_ID)
                            .updatedAt(toTime)
                            .lastSuccessfulTo(toTime)
                            .build();
                    
                    entraLastSyncMetadataRepository.save(newMetadata);
                    log.info("Created new EntraLastSyncMetadata record: updatedAt={}, lastSuccessfulTo={}", syncCompletedTime, toTime);
                }
            } else {
                log.warn("Failed to retrieve users from Tech Services: {}", response.getError().getMessage());
                throw new RuntimeException("Tech Services API call failed: " + response.getError().getMessage());
            }
        } catch (Exception e) {
            log.error("Error during sync process", e);
            throw e;
        }
    }
}
