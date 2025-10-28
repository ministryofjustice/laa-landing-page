package uk.gov.justice.laa.portal.landingpage.forms;

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
public class ConvertToMultiFirmForm implements Serializable {

    @NotNull(message = "You must select whether to convert this user to multi-firm access")
    private boolean convertToMultiFirm;
}
