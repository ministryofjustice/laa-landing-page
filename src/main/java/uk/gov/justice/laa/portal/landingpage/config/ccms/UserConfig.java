package uk.gov.justice.laa.portal.landingpage.config.ccms;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.laa.portal.landingpage.utils.MaskUtil;

@Data
@Slf4j
public class UserConfig {
    @NotNull
    private Management management;
    @NotNull
    private Api api;

    @Override
    public String toString() {
        return "UserConfig{" + "management=" + management.toString() + ", api=" + api.toString() + '}';
    }

    @Data
    public static class Management {
        @NotNull
        private ApiDetails api;

        @Override
        public String toString() {
            return "Management{" + "api=" + api.toString() + '}';
        }
    }

    @Data
    public static class Api {
        @NotNull
        private Sqs sqs;

        @Override
        public String toString() {
            return "Api{" + "sqs=" + sqs.toString() + '}';
        }
    }

    @Data
    @Slf4j
    public static class Sqs {
        @NotBlank
        private String arn;

        public String parseSqsQueueUrlFromArn() {
            if ("none".equalsIgnoreCase(arn)) {
                log.info("queue ARN is set to 'none', skipping");
                return "none";
            }

            String queueUrl = parseQueueUrlFromArn();
            log.info("SQS queue URL from ARN: {}", queueUrl);
            return queueUrl;
        }

        public String parseRegionFromArn() {
            if ("none".equalsIgnoreCase(arn)) {
                return "none";
            }

            if (arn == null || !arn.startsWith("arn:aws:sqs:")) {
                throw new IllegalArgumentException("Invalid SQS ARN format: " + arn);
            }

            String[] parts = arn.split(":");
            if (parts.length != 6) {
                throw new IllegalArgumentException("Invalid SQS ARN format, expected 6 parts: " + arn);
            }

            return parts[3];
        }

        private String parseQueueUrlFromArn() {
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

        @Override
        public String toString() {
            return "Sqs{" + "arn='" + MaskUtil.mask(arn) + '\'' + '}';
        }
    }

    @Data
    public static class ApiDetails {
        @NotBlank
        private String baseUrl;
        @NotBlank
        private String key;

        @Override
        public String toString() {
            return "ApiDetails{" + "baseUrl='" + MaskUtil.mask(baseUrl) + '\'' + ", key='" + MaskUtil.mask(key) + '\'' + '}';
        }
    }
}
