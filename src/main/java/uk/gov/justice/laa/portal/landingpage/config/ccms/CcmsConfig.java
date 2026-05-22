package uk.gov.justice.laa.portal.landingpage.config.ccms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.utils.MaskUtil;

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

    @Override
    public String toString() {
        return "CcmsConfig{" + "appEntraObjectId='" + MaskUtil.mask(appEntraObjectId) + '\'' + ", user=" + user.toString() + ", uda=" + uda.toString() + '}';
    }
}
