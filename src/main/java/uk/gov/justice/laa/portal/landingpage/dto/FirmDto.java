package uk.gov.justice.laa.portal.landingpage.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import uk.gov.justice.laa.portal.landingpage.entity.FirmType;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FirmDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private UUID id;
    private String name;
    private String code;
    private FirmType type;
    private boolean enabled;
    private boolean skipFirmSelection;
    private boolean canChange;

    public String getDisplayName() {
        return name + (StringUtils.isNotEmpty(code) ? " (" + code + ")" : "");
    }

}
