package uk.gov.justice.laa.portal.landingpage.exception;

import org.springframework.http.ResponseEntity;

public class TechServicesClientException extends RuntimeException {

    private String code;
    private String[] errors;
    private ResponseEntity<?> responseEntity;

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

    public TechServicesClientException(ResponseEntity<?> responseEntity) {
        super("There was an error when sending a request to Tech Services.");
        this.responseEntity = responseEntity;
    }

    public String getCode() {
        return code;
    }

    public String[] getErrors() {
        return errors;
    }

    public ResponseEntity<?> getResponseEntity() {
        return responseEntity;
    }
}
