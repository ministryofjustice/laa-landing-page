package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserDetailsForm {

    @NotEmpty(message = "Enter a first name")
    private String firstName;

    @NotEmpty(message = "Enter a Last name")
    private String lastName;

    @NotEmpty(message = "Enter an Email Address")
    @Email
    private String email;

    @NotNull(message = "A Firm is required")
    @NotEmpty(message = "A Firm is required")
    private String firmId;

    private Boolean isFirmAdmin;
}
