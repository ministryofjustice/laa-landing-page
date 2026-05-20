package uk.gov.justice.laa.portal.landingpage.registry;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConfig;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConnectionConfigProperties;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SqsClientRegistry {
    private final CcmsConnectionConfigProperties properties;
    private final Map<String, SqsClient> sqsClientsCache = new ConcurrentHashMap<>();
    private final Map<String, String> sqsClientsQueueUrlCache = new ConcurrentHashMap<>();

    public Optional<SqsClient> getSqsClient(String appEntraObjectId) {

        Map<String, CcmsConfig> activeConfigs = properties.getActiveConfigsByAppId();

        if (!activeConfigs.containsKey(appEntraObjectId)) {
            return Optional.empty();
        }

        SqsClient client = sqsClientsCache.computeIfAbsent(appEntraObjectId, name -> {
            CcmsConfig config = activeConfigs.get(name);
            return SqsClient.builder().region(Region.of(config.getUser().getApi().getSqs().parseRegionFromArn())).build();
        });

        return Optional.of(client);
    }

    public Optional<String> getSqsQueueUrl(String appEntraObjectId) {

        Map<String, CcmsConfig> activeConfigs = properties.getActiveConfigsByAppId();

        if (!activeConfigs.containsKey(appEntraObjectId)) {
            return Optional.empty();
        }

        String queueUrl = sqsClientsQueueUrlCache.computeIfAbsent(appEntraObjectId, name -> {
            CcmsConfig config = activeConfigs.get(name);
            return config.getUser().getApi().getSqs().parseSqsQueueUrlFromArn();
        });

        return Optional.of(queueUrl);
    }

}

