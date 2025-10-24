package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.justice.laa.portal.landingpage.validation.ConditionalPattern;
import uk.gov.justice.laa.portal.landingpage.validation.ConditionalSize;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiFirmUserForm implements Serializable {

    @NotEmpty(message = "Multi firm user email must be provided")
    @ConditionalSize(max = 254, message = "Email must not be longer than 254 characters")
    @ConditionalPattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._%-]*@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "Enter an email address in the correct format")
    private String email;
}
