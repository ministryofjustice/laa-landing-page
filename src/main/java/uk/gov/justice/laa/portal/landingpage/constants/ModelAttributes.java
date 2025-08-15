package uk.gov.justice.laa.portal.landingpage.constants;

/**
 * Constants for model attribute names used across the application.
 * This ensures consistency and prevents typos when setting model attributes.
 */
public final class ModelAttributes {

    /**
     * Model attribute name for page title used in templates.
     */
    public static final String PAGE_TITLE = "pageTitle";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ModelAttributes() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
