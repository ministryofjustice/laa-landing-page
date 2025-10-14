package uk.gov.justice.laa.portal.landingpage.forms;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiFirmUserForm implements Serializable {

    @NotEmpty(message = "Multi firm user email must be provided")
    @Email(message = "Multi firm user email must be a valid email address")
    private String email;
}
