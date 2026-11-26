package uk.gov.justice.laa.portal.landingpage.pact;

/**
 * Shared base for consumer Pact tests.
 *
 * <p>CONSUMER must equal this repo's pacticipant name (= the GitHub repo name).
 * PROVIDER must byte-for-byte equal the provider's {@code @Provider("...")} value in
 * laa-data-user-api — a mismatch silently creates two pacticipants and nothing ever verifies.
 */
public abstract class AbstractPactTest {

    /** This consumer's pacticipant name. */
    public static final String CONSUMER = "laa-landing-page";

    /** The provider we contract with. MUST match @Provider("...") in laa-data-user-api. */
    public static final String PROVIDER = "laa-data-user-api";

    // --- reusable format matchers: assert shape, not specific values ---
    public static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    public static final String ISO_DATETIME_REGEX =
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?(Z|[+-]\\d{2}:\\d{2})";
    public static final String ISO_DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
}
