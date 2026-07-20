package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Central configuration for UI labels and terminology used across Thymeleaf templates.
 *
 * <p>This class provides a single location to define and update labels that appear in the UI,
 * ensuring consistency across all pages and eliminating duplication of hardcoded strings in
 * templates.</p>
 *
 * <h2>How to add a new label</h2>
 * <ol>
 *   <li>Add a field with a sensible default value to this class.</li>
 *   <li>Add the corresponding property to {@code ui-labels.properties} using the
 *       {@code app.ui.labels.*} prefix.</li>
 *   <li>Reference it in Thymeleaf templates via the {@code uiLabels} model attribute,
 *       e.g. {@code th:text="${uiLabels.officeAccountNumber}"}.</li>
 * </ol>
 *
 * <h2>Naming conventions</h2>
 * <ul>
 *   <li>Java fields: {@code camelCase} (e.g. {@code officeAccountNumber})</li>
 *   <li>Properties file keys: {@code kebab-case} under {@code app.ui.labels.*}
 *       (e.g. {@code app.ui.labels.office-account-number})</li>
 *   <li>Thymeleaf references: {@code ${uiLabels.fieldName}}
 *       (e.g. {@code ${uiLabels.officeAccountNumber}})</li>
 * </ul>
 *
 * <h2>Fallback behaviour</h2>
 * <p>Every field must have a default value set as a field initialiser. If a property is missing
 * or not defined in the active environment, the default value is used automatically and no
 * error or warning is raised.</p>
 *
 * <h2>Environment-specific overrides</h2>
 * <p>Override any label in a profile-specific properties file (e.g.
 * {@code application-local.properties}, {@code application-test.properties}) using the same
 * {@code app.ui.labels.*} key.</p>
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
