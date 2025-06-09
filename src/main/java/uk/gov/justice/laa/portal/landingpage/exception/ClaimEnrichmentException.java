package uk.gov.justice.laa.portal.landingpage.exception;

public class ClaimEnrichmentException extends RuntimeException {
    public ClaimEnrichmentException(String message) {
        super(message);
    }

    public ClaimEnrichmentException(String message, Throwable cause) {
        super(message, cause);
    }
}