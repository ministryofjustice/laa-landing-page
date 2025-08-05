package uk.gov.justice.laa.portal.landingpage.client;

/**
 * Exception thrown when CCMS API communication fails
 */
public class CcmsApiException extends RuntimeException {

    public CcmsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
