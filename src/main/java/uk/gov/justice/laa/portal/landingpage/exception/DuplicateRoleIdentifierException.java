package uk.gov.justice.laa.portal.landingpage.exception;

public class DuplicateRoleIdentifierException extends RuntimeException {
    public DuplicateRoleIdentifierException(String message) {
        super(message);
    }
}
