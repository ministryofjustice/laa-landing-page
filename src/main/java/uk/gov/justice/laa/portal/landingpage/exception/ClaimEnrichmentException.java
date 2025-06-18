package uk.gov.justice.laa.portal.landingpage.exception;

public class ClaimEnrichmentException extends RuntimeException {
    public ClaimEnrichmentException(String message) {
        super(message);
    }

    public ClaimEnrichmentException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClaimEnrichmentException(String message, String... args) {
        super(String.format(message, (Object) args));
    }
}