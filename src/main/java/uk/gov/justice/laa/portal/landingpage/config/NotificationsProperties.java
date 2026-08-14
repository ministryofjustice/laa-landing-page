package uk.gov.justice.laa.portal.landingpage.config;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

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
    @NotEmpty
    private String delegateFirmAccessEmailTemplate;
    @NotEmpty
    private String revokeFirmAccessEmailTemplate;
    @NotEmpty
    private String userAccessChangeEmailTemplate;
    @NotEmpty
    private String existingUserEmailTemplate;
    @NotEmpty
    private String reactivationEmailTemplate;

}
