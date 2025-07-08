package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EditUserDetailsForm {
    @Size(max = 99, message = "First name must not be longer than 99 characters")
    @NotEmpty(message = "Enter a first name")
    private String firstName;

    @Size(max = 99, message = "Last name must not be longer than 99 characters")
    @NotEmpty(message = "Enter a last name")
    private String lastName;

    private String email;
}
