package uk.gov.justice.laa.portal.landingpage.config.ccms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.justice.laa.portal.landingpage.utils.MaskUtil;

@Data
public class UdaConfig {
    @NotBlank
    private String baseUrl;
    @NotNull
    private Api api;

    @Override
    public String toString() {
        return "UdaConfig{" + "baseUrl='" + MaskUtil.mask(baseUrl) + '\'' + ", api=" + api.toString() + '}';
    }

    @Data
    public static class Api {
        @NotBlank
        private String key;

        @Override
        public String toString() {
            return "Api{" + "key='" + MaskUtil.mask(key) + '\'' + '}';
        }
    }
}
