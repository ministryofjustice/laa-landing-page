package uk.gov.justice.laa.portal.landingpage.exception;

public class TechServicesClientException extends RuntimeException {

    private String code;
    private String[] errors;

    public TechServicesClientException(String message) {
        super(message);
    }

    public TechServicesClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public TechServicesClientException(String message, String... args) {
        super(String.format(message, (Object[]) args));
    }

    public TechServicesClientException(String message, String code) {
        super(message);
        this.code = code;
    }

    public TechServicesClientException(String message, String code, String... errors) {
        super(message);
        this.code = code;
        this.errors = errors;
    }

    public String getCode() {
        return code;
    }

    public String[] getErrors() {
        return errors;
    }
}
