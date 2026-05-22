package uk.gov.justice.laa.portal.landingpage.registry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConfig;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConnectionConfigProperties;
import uk.gov.justice.laa.portal.landingpage.utils.MaskUtil;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsClientRegistry {
    private final CcmsConnectionConfigProperties properties;
    private final Map<String, SqsClient> sqsClientsCache = new ConcurrentHashMap<>();
    private final Map<String, String> sqsClientsQueueUrlCache = new ConcurrentHashMap<>();

    public Optional<SqsClient> getSqsClient(String appEntraObjectId) {

        Map<String, CcmsConfig> activeConfigs = properties.getActiveConfigsByAppId();

        log.info("Active configs: {}", activeConfigs);

        if (!activeConfigs.containsKey(appEntraObjectId)) {
            log.info("SQS Client Requested for {} is empty", MaskUtil.mask(appEntraObjectId));
            return Optional.empty();
        }

        SqsClient client = sqsClientsCache.computeIfAbsent(appEntraObjectId, name -> {
            CcmsConfig config = activeConfigs.get(name);
            return SqsClient.builder().region(Region.of(config.getUser().getApi().getSqs().parseRegionFromArn())).build();
        });

        log.info("SQS Client Requested for {} is {}", MaskUtil.mask(appEntraObjectId), MaskUtil.mask(client.toString()));
        return Optional.of(client);
    }

    public Optional<String> getSqsQueueUrl(String appEntraObjectId) {

        Map<String, CcmsConfig> activeConfigs = properties.getActiveConfigsByAppId();

        log.info("Active configs: {}", activeConfigs);
        log.info("SQS URL Requested for : {}", MaskUtil.mask(appEntraObjectId));

        if (!activeConfigs.containsKey(appEntraObjectId)) {
            log.info("SQS URL Requested for {} is empty", MaskUtil.mask(appEntraObjectId));
            return Optional.empty();
        }

        String queueUrl = sqsClientsQueueUrlCache.computeIfAbsent(appEntraObjectId, name -> {
            CcmsConfig config = activeConfigs.get(name);
            return config.getUser().getApi().getSqs().parseSqsQueueUrlFromArn();
        });

        log.info("SQS URL Requested for {} is {}", MaskUtil.mask(appEntraObjectId), MaskUtil.mask(queueUrl));
        return Optional.of(queueUrl);
    }

}

