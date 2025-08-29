package uk.gov.justice.laa.portal.landingpage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@Slf4j
public class SqsConfig {

    @Value("${ccms.user.api.sqs.arn}")
    private String queueArn;

    @Value("${ccms.sqs.region}")
    private String awsRegion;

    @Bean
    public SqsClient sqsClient() {
        log.info("Creating SQS client");
        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public String sqsQueueUrl() {
        if ("none".equalsIgnoreCase(queueArn)) {
            log.info("queue ARN is set to 'none', skipping");
            return "none";
        }
        
        String queueUrl = parseQueueUrlFromArn(queueArn);
        log.info("SQS queue URL from ARN: {}", queueUrl);
        return queueUrl;
    }

    private String parseQueueUrlFromArn(String arn) {
        if (arn == null || !arn.startsWith("arn:aws:sqs:")) {
            throw new IllegalArgumentException("Invalid SQS ARN format: " + arn);
        }

        String[] parts = arn.split(":");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid SQS ARN format, expected 6 parts: " + arn);
        }

        String region = parts[3];
        String accountId = parts[4];
        String queueName = parts[5];

        return String.format("https://sqs.%s.amazonaws.com/%s/%s", region, accountId, queueName);
    }
}
