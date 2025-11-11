package uk.gov.justice.laa.portal.landingpage.exception;

/**
 * Exception thrown when attempting to delete the last firm profile of a
 * multi-firm user.
 * Multi-firm users must maintain at least one firm profile.
 */
public class LastFirmProfileDeletionException extends RuntimeException {

    public LastFirmProfileDeletionException() {
        super("Cannot delete the last firm profile. User must have at least one profile.");
    }

    public LastFirmProfileDeletionException(String message) {
        super(message);
    }
}
