package uk.gov.justice.laa.portal.landingpage.forms;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.validation.ConditionalPattern;
import uk.gov.justice.laa.portal.landingpage.validation.ConditionalSize;

@Data
public class UserDetailsForm implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    //Az letters include diacritics
    private static final String allowedChars = "A-Za-zÀ-ÖØ-öø-įĴ-őŔ-žǍ-ǰǴ-ǵǸ-țȞ-ȟȤ-ȳɃɆ-ɏḀ-ẞƀ-ƓƗ-ƚƝ-ơƤ-ƥƫ-ưƲ-ƶẠ-ỿ";

    @NotBlank(message = "Enter a first name")
    @ConditionalSize(min = 2, max = 99, message = "First name must be between 2-99 characters")
    @ConditionalPattern(regexp =
            "^(?:[" + allowedChars + "]+)(?:(?:[\\-']+| )[" + allowedChars + "]+)*$",
            message = "First name must not contain numbers or special characters")
    private String firstName;

    @NotBlank(message = "Enter a last name")
    @ConditionalSize(min = 2, max = 99, message = "Last name must be between 2-99 characters")
    @ConditionalPattern(regexp =
            "^(?:[" + allowedChars + "]+)(?:(?:[\\-']+| )[" + allowedChars + "]+)*$",
            message = "Last name must not contain numbers or special characters")
    private String lastName;

    @NotEmpty(message = "Enter an email address")
    @ConditionalSize(max = 254, message = "Email must not be longer than 254 characters")
    @ConditionalPattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._%-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Enter an email address in the correct format")
    private String email;

    @NotNull(message = "Select a user type")
    private Boolean userManager;

    private Boolean multiFirmUser;
}
