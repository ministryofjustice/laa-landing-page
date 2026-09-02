package uk.gov.justice.laa.portal.landingpage.config;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Binds {@code app.ui.labels.*} properties from {@code ui-labels.properties} and exposes them
 * to all Thymeleaf templates via the {@code uiLabels} model attribute. See that file for
 * guidance on adding labels, naming conventions, and environment-specific overrides.
 */
@Component
@ConfigurationProperties(prefix = "app.ui.labels")
@Data
public class UiLabelsProperties implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Unique account number associated with a provider office. */
    private String officeAccountNumber = "Office account number";

    /** Name of a provider firm. */
    private String firmName = "Firm name";

    /** User's first name. */
    private String firstName = "First name";

    /** User's last name. */
    private String lastName = "Last name";

    /** User's email address. */
    private String email = "Email";

    /** User type classification (e.g. Internal / External). */
    private String userType = "User type";

    /** Status column heading used in user and firm tables. */
    private String status = "Status";

    /** Delete reason labels keyed by reason code. */
    private Map<String, String> deleteReasons = new HashMap<>();

}
