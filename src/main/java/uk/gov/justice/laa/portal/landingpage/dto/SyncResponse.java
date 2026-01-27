package uk.gov.justice.laa.portal.landingpage.dto;

/**
 * Response DTO for sync operation status.
 */
public record SyncResponse(String status, String message, boolean started) {
}
