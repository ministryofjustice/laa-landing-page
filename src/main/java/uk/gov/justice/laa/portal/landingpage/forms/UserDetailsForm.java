package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;
import uk.gov.justice.laa.portal.landingpage.validation.ConditionalPattern;
import uk.gov.justice.laa.portal.landingpage.validation.ConditionalSize;

import java.io.Serializable;

@Data
public class UserDetailsForm implements Serializable {

    @NotEmpty(message = "Enter a first name")
    @ConditionalSize(min = 2, max = 99, message = "First name must be between 2-99 characters")
    @ConditionalPattern(regexp = "^[A-Za-z](?:[A-Za-z \\-']*[A-Za-z])?$", message = "First name must not contain numbers or special characters")
    private String firstName;

    @NotEmpty(message = "Enter a last name")
    @ConditionalSize(min = 2, max = 99, message = "Last name must be between 2-99 characters")
    @ConditionalPattern(regexp = "^[A-Za-z](?:[A-Za-z \\-']*[A-Za-z])?$", message = "Last name must not contain numbers or special characters")
    private String lastName;

    @NotEmpty(message = "Enter an email address")
    @ConditionalSize(max = 254, message = "Email must not be longer than 254 characters")
    @ConditionalPattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._%-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Enter an email address in the correct format")
    private String email;

    @NotNull(message = "Select a user type")
    private Boolean userManager;
}
