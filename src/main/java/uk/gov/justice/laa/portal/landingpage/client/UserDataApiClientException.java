package uk.gov.justice.laa.portal.landingpage.client;

/**
 * Thrown when an HTTP call to laa-data-user-api fails with a 4xx or 5xx status.
 * Callers map the status code to the appropriate user-facing state without exposing response bodies.
 */
public class UserDataApiClientException extends RuntimeException {

    private final int statusCode;

    public UserDataApiClientException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public UserDataApiClientException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
