package uk.gov.justice.laa.portal.landingpage.exception;

public class OfficeAssignmentException extends RuntimeException {

    public OfficeAssignmentException() {
        super();
    }

    public OfficeAssignmentException(String message) {
        super(message);
    }

    public OfficeAssignmentException(String message, Throwable cause) {
        super(message, cause);
    }

}