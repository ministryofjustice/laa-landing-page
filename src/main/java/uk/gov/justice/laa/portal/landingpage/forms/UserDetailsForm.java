package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserDetailsForm {

    @NotEmpty(message = "Enter a first name")
    private String firstName;

    @NotEmpty(message = "Enter a last name")
    private String lastName;

    @NotEmpty(message = "Enter an email address")
    @Email
    private String email;

    @NotNull(message = "A firm is required")
    @NotEmpty(message = "A firm is required")
    private String firmId;

    private Boolean isFirmAdmin;
}
