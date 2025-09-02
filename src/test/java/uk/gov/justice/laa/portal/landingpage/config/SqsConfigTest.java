package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SqsConfigTest {

    private SqsConfig sqsConfig;

    @BeforeEach
    void setUp() {
        sqsConfig = new SqsConfig();
    }

    @Test
    void shouldCreateSqsClient_withRegionFromArn() {
        String validArn = "arn:aws:sqs:eu-west-2:123456789012:test-queue";
        ReflectionTestUtils.setField(sqsConfig, "queueArn", validArn);

        SqsClient sqsClient = sqsConfig.sqsClient();

        assertThat(sqsClient).isNotNull();
    }

    @Test
    void shouldCreateSqsClient_withDefaultRegionWhenArnIsNone() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", "none");

        SqsClient sqsClient = sqsConfig.sqsClient();

        assertThat(sqsClient).isNotNull();
    }

    @Test
    void shouldReturnNone_whenQueueArnIsNone() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", "none");

        String queueUrl = sqsConfig.sqsQueueUrl();

        assertThat(queueUrl).isEqualTo("none");
    }

    @Test
    void shouldReturnNone_whenQueueArnIsNoneCaseInsensitive() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", "NONE");

        String queueUrl = sqsConfig.sqsQueueUrl();

        assertThat(queueUrl).isEqualTo("none");
    }

    @Test
    void shouldParseStandardQueueArn_toCorrectQueueUrl() {
        String standardArn = "arn:aws:sqs:us-east-1:123456789012:my-queue";
        ReflectionTestUtils.setField(sqsConfig, "queueArn", standardArn);

        String queueUrl = sqsConfig.sqsQueueUrl();

        assertThat(queueUrl).isEqualTo("https://sqs.us-east-1.amazonaws.com/123456789012/my-queue");
    }

    @Test
    void shouldThrowException_whenArnIsNull() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", null);

        assertThatThrownBy(() -> sqsConfig.sqsQueueUrl())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid SQS ARN format: null");
    }

    @Test
    void shouldThrowException_whenArnDoesNotStartWithSqsPrefix() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", "arn:aws:s3:eu-west-2:123456789012:bucket");

        assertThatThrownBy(() -> sqsConfig.sqsQueueUrl())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid SQS ARN format: arn:aws:s3:eu-west-2:123456789012:bucket");
    }

    @Test
    void shouldThrowException_whenArnHasIncorrectNumberOfParts() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", "arn:aws:sqs:eu-west-2:123456789012");

        assertThatThrownBy(() -> sqsConfig.sqsQueueUrl())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid SQS ARN format, expected 6 parts: arn:aws:sqs:eu-west-2:123456789012");
    }

    @Test
    void shouldThrowException_whenArnHasTooManyParts() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", "arn:aws:sqs:eu-west-2:123456789012:queue:extra");

        assertThatThrownBy(() -> sqsConfig.sqsQueueUrl())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid SQS ARN format, expected 6 parts: arn:aws:sqs:eu-west-2:123456789012:queue:extra");
    }

    @Test
    void shouldThrowException_whenArnIsEmpty() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", "");

        assertThatThrownBy(() -> sqsConfig.sqsQueueUrl())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid SQS ARN format: ");
    }

    @Test
    void shouldThrowException_whenArnIsInvalidFormat() {
        ReflectionTestUtils.setField(sqsConfig, "queueArn", "invalid-arn-format");

        assertThatThrownBy(() -> sqsConfig.sqsQueueUrl())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid SQS ARN format: invalid-arn-format");
    }
}
