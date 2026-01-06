package uk.gov.justice.laa.portal.landingpage.forms;

import java.io.Serial;
import java.io.Serializable;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiFirmForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @NotNull(message = "You must select whether this user requires access to multiple firms")
    private Boolean multiFirmUser;
}
