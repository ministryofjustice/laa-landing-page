package uk.gov.justice.laa.portal.landingpage.config.ccms;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "ccms")
@Data
public class CcmsConnectionConfigProperties {
    private final List<CcmsConfig> configs = new ArrayList<>();

    public Map<String, CcmsConfig> getActiveConfigsByAppId() {
        return configs.stream()
                .filter(CcmsConfig::isValid)
                .collect(Collectors.toMap(CcmsConfig::getAppEntraObjectId, c -> c, (c1, c2) -> c1));
    }
}
