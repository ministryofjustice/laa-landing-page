package uk.gov.justice.laa.portal.landingpage.client;

/**
 * A User Data API failure with a status code for callers to map to the appropriate UX
 * without exposing response bodies.
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
