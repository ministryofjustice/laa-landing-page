package uk.gov.justice.laa.portal.landingpage.config.ccms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CcmsConfig {

    @NotBlank
    private String appEntraObjectId;
    @NotNull
    private UserConfig user;
    @NotNull
    private UdaConfig uda;

    public boolean isValid() {
        return appEntraObjectId != null
                && !appEntraObjectId.isBlank()
                && !"NONE".equalsIgnoreCase(appEntraObjectId);
    }
}
