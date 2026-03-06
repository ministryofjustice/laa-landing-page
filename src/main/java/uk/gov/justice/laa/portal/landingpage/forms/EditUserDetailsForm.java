package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.validation.ConditionalPattern;
import uk.gov.justice.laa.portal.landingpage.validation.ConditionalSize;

import java.io.Serial;
import java.io.Serializable;

@Data
public class EditUserDetailsForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 99, message = "First name must not be longer than 99 characters")
    @NotEmpty(message = "Enter a first name")
    private String firstName;

    @Size(max = 99, message = "Last name must not be longer than 99 characters")
    @NotEmpty(message = "Enter a last name")
    private String lastName;

    @NotEmpty(message = "Enter an email address")
    @ConditionalSize(max = 254, message = "Email must not be longer than 254 characters")
    @ConditionalPattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._%-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Enter an email address in the correct format")
    private String email;
}
