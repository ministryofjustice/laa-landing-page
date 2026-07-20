package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Central configuration for UI labels and terminology used across Thymeleaf templates.
 *
 * Provides a single location to define and update labels, ensuring consistency across all
 * pages and eliminating duplicated hardcoded strings in templates.
 *
 * HOW TO ADD A NEW LABEL
 *   1. Add a field with a sensible default value to this class.
 *   2. Add the corresponding property to ui-labels.properties using the app.ui.labels.* prefix.
 *   3. Reference it in templates via the uiLabels model attribute, e.g. th:text="${uiLabels.fieldName}".
 *
 * NAMING CONVENTIONS
 *   Java fields        : camelCase              e.g. officeAccountNumber
 *   Properties keys    : kebab-case             e.g. app.ui.labels.office-account-number
 *   Template references: ${uiLabels.fieldName}  e.g. ${uiLabels.officeAccountNumber}
 *
 * FALLBACK BEHAVIOUR
 *   Every field must have a default value set as a field initialiser. If a property is missing
 *   the default is used automatically.
 *
 * ENVIRONMENT-SPECIFIC OVERRIDES
 *   Override any label in a profile-specific properties file (e.g. application-local.properties)
 *   using the same app.ui.labels.* key. No code changes required in consuming templates.
 */
@Configuration
@ConfigurationProperties(prefix = "app.ui.labels")
@Data
public class UiLabelsProperties {

    /**
     * Label used to describe the unique account number associated with a provider office.
     * Displayed as a column heading and as visually-hidden text alongside office codes.
     *
     * <p>Default: {@code "Office account number"}</p>
     */
    private String officeAccountNumber = "Office account number";

    /**
     * Label for the name of a provider firm.
     *
     * <p>Default: {@code "Firm name"}</p>
     */
    private String firmName = "Firm name";

    /**
     * Label for a user's first name, used in summary lists and table headers.
     *
     * <p>Default: {@code "First name"}</p>
     */
    private String firstName = "First name";

    /**
     * Label for a user's last name, used in summary lists and table headers.
     *
     * <p>Default: {@code "Last name"}</p>
     */
    private String lastName = "Last name";

    /**
     * Label for a user's email address, used in summary lists and table headers.
     *
     * <p>Default: {@code "Email"}</p>
     */
    private String email = "Email";

    /**
     * Label for the user type classification (e.g. Internal / External).
     *
     * <p>Default: {@code "User type"}</p>
     */
    private String userType = "User type";

    /**
     * Generic status column heading used in user and firm tables.
     *
     * <p>Default: {@code "Status"}</p>
     */
    private String status = "Status";
}
