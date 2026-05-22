package uk.gov.justice.laa.portal.landingpage.config.ccms;


import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.justice.laa.portal.landingpage.utils.MaskUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "ccms")
@Data
@Slf4j
public class CcmsConnectionConfigProperties {
    private final List<CcmsConfig> configs = new ArrayList<>();

    public Map<String, CcmsConfig> getActiveConfigsByAppId() {
        return configs.stream()
                .filter(CcmsConfig::isValid)
                .collect(Collectors.toMap(CcmsConfig::getAppEntraObjectId, c -> c, (c1, c2) -> c1));
    }

    @PostConstruct
    public void logActiveConfigs() {
        Map<String, CcmsConfig> activeConfigs = getActiveConfigsByAppId();
        if (activeConfigs.isEmpty()) {
            log.info("CCMS: No active CCMS configurations found.");
        } else {
            log.info("CCMS: Active CCMS configurations:");
            activeConfigs.forEach((appId, config) -> log.info("CCMS: App ID: " + MaskUtil.mask(appId) + ", Config: " + config.toString()));
        }
    }
}
