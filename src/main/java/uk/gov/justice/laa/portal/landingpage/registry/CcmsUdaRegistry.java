package uk.gov.justice.laa.portal.landingpage.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConfig;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConnectionConfigProperties;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CcmsUdaRegistry {
    private final CcmsConnectionConfigProperties properties;
    private final Map<String, String> udaBaseUrlCache = new ConcurrentHashMap<>();
    private final Map<String, String> udaApiKeyCache = new ConcurrentHashMap<>();

    public Optional<String> getUdaBaseUrl(String appEntraObjectId) {

        Map<String, CcmsConfig> activeConfigs = properties.getActiveConfigsByAppId();

        if (!activeConfigs.containsKey(appEntraObjectId)) {
            return Optional.empty();
        }

        String udaBaseUrl = udaBaseUrlCache.computeIfAbsent(appEntraObjectId, name -> {
            CcmsConfig config = activeConfigs.get(name);
            return config.getUda().getBaseUrl();
        });

        return Optional.of(udaBaseUrl);
    }


    public Optional<String> getUdaApiKey(String appEntraObjectId) {

        Map<String, CcmsConfig> activeConfigs = properties.getActiveConfigsByAppId();

        if (!activeConfigs.containsKey(appEntraObjectId)) {
            return Optional.empty();
        }

        String udaApiKey = udaApiKeyCache.computeIfAbsent(appEntraObjectId, name -> {
            CcmsConfig config = activeConfigs.get(name);
            return config.getUda().getApi().getKey();
        });

        return Optional.of(udaApiKey);
    }

}
