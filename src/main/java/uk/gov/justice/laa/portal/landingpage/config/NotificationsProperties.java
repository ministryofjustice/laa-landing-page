package uk.gov.justice.laa.portal.landingpage.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

/**
 * A POJO to encapsulate the properties associated with sending email notifications.
 */
@Validated
@Data
public class NotificationsProperties {

    @NotEmpty
    private String govNotifyApiKey;
    @NotEmpty
    private String portalUrl;

    @NotEmpty
    private String addNewUserEmailTemplate;

}
