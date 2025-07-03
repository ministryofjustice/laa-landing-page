package uk.gov.justice.laa.portal.landingpage.exception;

/**
 * An exception for handling cases where a user tries to move forward in a user details form (usually by manipulating the URL)
 * without fully completing all details
 */
public class CreateUserDetailsIncompleteException extends RuntimeException {

    public CreateUserDetailsIncompleteException() {}

    public CreateUserDetailsIncompleteException(String message, Object... args) {
        super(String.format(message, args));
    }

}
