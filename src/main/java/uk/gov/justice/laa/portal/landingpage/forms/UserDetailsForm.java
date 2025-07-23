package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.entity.UserType;

@Data
public class UserDetailsForm {

    @NotEmpty(message = "Select a firm")
    private String firmId;

    @Size(min = 2, max = 99, message = "First name must be between 2-99 characters")
    @NotEmpty(message = "Enter a first name")
    @Pattern(regexp = "^[A-Za-z](?:[A-Za-z \\-']*[A-Za-z])?$", message = "First name must not contain numbers or special characters")
    private String firstName;

    @Size(min = 2, max = 99, message = "Last name must be between 2-99 characters")
    @NotEmpty(message = "Enter a last name")
    @Pattern(regexp = "^[A-Za-z](?:[A-Za-z \\-']*[A-Za-z])?$", message = "Last name must not contain numbers or special characters")
    private String lastName;

    @Size(max = 254, message = "Email must not be longer than 254 characters")
    @NotEmpty(message = "Enter an email address")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._%+-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Enter an email address in the correct format")
    private String email;

    @NotNull(message = "Select a user type")
    private UserType userType;
}
