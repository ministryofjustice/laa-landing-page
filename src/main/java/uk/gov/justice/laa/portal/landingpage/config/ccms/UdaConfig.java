package uk.gov.justice.laa.portal.landingpage.config.ccms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UdaConfig {
    @NotBlank
    private String baseUrl;
    @NotNull
    private Api api;

    @Data
    public static class Api {
        @NotBlank
        private String key;
    }
}
