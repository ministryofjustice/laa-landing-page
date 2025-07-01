package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDetailsForm {

    @NotEmpty(message = "Select a firm")
    private String firmId;

    private Boolean isFirmAdmin;

    @Size(max = 99, message = "First name must not be longer than 99 characters")
    @NotEmpty(message = "Enter a first name")
    private String firstName;

    @Size(max = 99, message = "Last name must not be longer than 99 characters")
    @NotEmpty(message = "Enter a last name")
    private String lastName;

    @Size(max = 254, message = "Email must not be longer than 254 characters")
    @NotEmpty(message = "Enter an email address")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._%+-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Enter an email address in the correct format")
    private String email;
}
