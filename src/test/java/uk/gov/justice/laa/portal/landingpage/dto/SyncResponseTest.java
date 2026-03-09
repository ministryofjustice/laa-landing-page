package uk.gov.justice.laa.portal.landingpage.dto;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 * Tests for SyncResponse DTO.
 */
class SyncResponseTest {

    @Test
    void shouldCreateSyncResponseWithAllFields() {
        // Given
        String status = "SUCCESS";
        String message = "Sync completed successfully";
        boolean started = true;

        // When
        SyncResponse response = new SyncResponse(status, message, started);

        // Then
        assertThat(response.status()).isEqualTo(status);
        assertThat(response.message()).isEqualTo(message);
        assertThat(response.started()).isTrue();
    }

    @Test
    void shouldCreateSyncResponseWithFailureStatus() {
        // Given
        String status = "FAILURE";
        String message = "Sync failed due to error";
        boolean started = false;

        // When
        SyncResponse response = new SyncResponse(status, message, started);

        // Then
        assertThat(response.status()).isEqualTo("FAILURE");
        assertThat(response.message()).isEqualTo("Sync failed due to error");
        assertThat(response.started()).isFalse();
    }

    @Test
    void shouldSupportRecordEquality() {
        // Given
        SyncResponse response1 = new SyncResponse("SUCCESS", "Completed", true);
        SyncResponse response2 = new SyncResponse("SUCCESS", "Completed", true);
        SyncResponse response3 = new SyncResponse("FAILURE", "Error", false);

        // Then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1).isNotEqualTo(response3);
    }

    @Test
    void shouldSupportRecordHashCode() {
        // Given
        SyncResponse response1 = new SyncResponse("SUCCESS", "Completed", true);
        SyncResponse response2 = new SyncResponse("SUCCESS", "Completed", true);

        // Then
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void shouldSupportRecordToString() {
        // Given
        SyncResponse response = new SyncResponse("IN_PROGRESS", "Processing", true);

        // When
        String toString = response.toString();

        // Then
        assertThat(toString).contains("IN_PROGRESS");
        assertThat(toString).contains("Processing");
        assertThat(toString).contains("true");
    }

    @Test
    void shouldHandleNullValues() {
        // When
        SyncResponse response = new SyncResponse(null, null, false);

        // Then
        assertThat(response.status()).isNull();
        assertThat(response.message()).isNull();
        assertThat(response.started()).isFalse();
    }
}
