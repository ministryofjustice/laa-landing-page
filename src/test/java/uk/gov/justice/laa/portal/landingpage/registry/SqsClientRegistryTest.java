package uk.gov.justice.laa.portal.landingpage.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConfig;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConnectionConfigProperties;
import uk.gov.justice.laa.portal.landingpage.config.ccms.UserConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SqsClientRegistry Tests")
class SqsClientRegistryTest {

    @Mock
    private CcmsConnectionConfigProperties properties;

    @InjectMocks
    private SqsClientRegistry sqsClientRegistry;

    private CcmsConfig ccmsConfig;
    private UserConfig.Sqs sqsConfig;
    private Map<String, CcmsConfig> activeConfigs;

    @BeforeEach
    void setUp() {
        activeConfigs = new HashMap<>();

        // Initialize SQS config
        sqsConfig = new UserConfig.Sqs();
        sqsConfig.setArn("arn:aws:sqs:us-east-1:123456789012:test-queue");

        // Initialize User config
        UserConfig userConfig = new UserConfig();
        UserConfig.Api apiConfig = new UserConfig.Api();
        apiConfig.setSqs(sqsConfig);
        userConfig.setApi(apiConfig);

        // Initialize CCMS config
        ccmsConfig = new CcmsConfig();
        ccmsConfig.setAppEntraObjectId("app-entra-oid-123");
        ccmsConfig.setUser(userConfig);
    }


    @Test
    @DisplayName("getSqsClient should return empty Optional when app ID does not exist in active configs")
    void testGetSqsClient_WhenAppIdNotExists_ReturnsEmptyOptional() {
        activeConfigs.put("other-app-id", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<SqsClient> result = sqsClientRegistry.getSqsClient("non-existent-app-id");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getSqsClient should return empty Optional when active configs is empty")
    void testGetSqsClient_WhenActiveConfigsEmpty_ReturnsEmptyOptional() {
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<SqsClient> result = sqsClientRegistry.getSqsClient("app-entra-oid-123");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getSqsClient should cache SqsClient instance on subsequent calls with same app ID")
    void testGetSqsClient_CachesSqsClientOnSubsequentCalls() {
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        try (MockedStatic<SqsClient> sqsClientMock = mockStatic(SqsClient.class)) {
            SqsClient mockClient = mock(SqsClient.class);
            var builderMock = mock(software.amazon.awssdk.services.sqs.SqsClientBuilder.class);

            when(builderMock.region(any(Region.class))).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(mockClient);
            sqsClientMock.when(SqsClient::builder).thenReturn(builderMock);

            Optional<SqsClient> firstResult = sqsClientRegistry.getSqsClient("app-entra-oid-123");
            Optional<SqsClient> secondResult = sqsClientRegistry.getSqsClient("app-entra-oid-123");

            assertTrue(firstResult.isPresent());
            assertTrue(secondResult.isPresent());
            assertEquals(firstResult.get(), secondResult.get());
        }
    }

    @Test
    @DisplayName("getSqsClient should handle multiple app IDs with separate cache entries")
    void testGetSqsClient_WithMultipleAppIds_ReturnsDifferentClientsForEachId() {
        CcmsConfig ccmsConfig2 = new CcmsConfig();
        ccmsConfig2.setAppEntraObjectId("app-entra-oid-456");
        UserConfig userConfig2 = new UserConfig();
        UserConfig.Api apiConfig2 = new UserConfig.Api();
        UserConfig.Sqs sqsConfig2 = new UserConfig.Sqs();
        sqsConfig2.setArn("arn:aws:sqs:eu-west-1:987654321098:another-queue");
        apiConfig2.setSqs(sqsConfig2);
        userConfig2.setApi(apiConfig2);
        ccmsConfig2.setUser(userConfig2);

        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        activeConfigs.put("app-entra-oid-456", ccmsConfig2);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        try (MockedStatic<SqsClient> sqsClientMock = mockStatic(SqsClient.class)) {
            SqsClient mockClient1 = mock(SqsClient.class);
            SqsClient mockClient2 = mock(SqsClient.class);
            var builderMock = mock(software.amazon.awssdk.services.sqs.SqsClientBuilder.class);

            when(builderMock.region(any(Region.class))).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(mockClient1).thenReturn(mockClient2);
            sqsClientMock.when(SqsClient::builder).thenReturn(builderMock);

            Optional<SqsClient> result1 = sqsClientRegistry.getSqsClient("app-entra-oid-123");
            Optional<SqsClient> result2 = sqsClientRegistry.getSqsClient("app-entra-oid-456");

            assertTrue(result1.isPresent());
            assertTrue(result2.isPresent());
        }
    }

    @Test
    @DisplayName("getSqsClient should be case-sensitive for app ID lookup")
    void testGetSqsClient_CaseSensitiveForAppId() {
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<SqsClient> resultLowerCase = sqsClientRegistry.getSqsClient("app-entra-oid-123");
        Optional<SqsClient> resultUpperCase = sqsClientRegistry.getSqsClient("APP-ENTRA-OID-123");

        assertTrue(resultLowerCase.isPresent());
        assertFalse(resultUpperCase.isPresent());
    }

    @Test
    @DisplayName("getSqsQueueUrl should return Optional with queue URL when app ID exists in active configs")
    void testGetSqsQueueUrl_WhenAppIdExists_ReturnsOptionalWithQueueUrl() {
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");

        assertTrue(result.isPresent());
        assertEquals("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue", result.get());
    }

    @Test
    @DisplayName("getSqsQueueUrl should return empty Optional when app ID does not exist in active configs")
    void testGetSqsQueueUrl_WhenAppIdNotExists_ReturnsEmptyOptional() {
        activeConfigs.put("other-app-id", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = sqsClientRegistry.getSqsQueueUrl("non-existent-app-id");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getSqsQueueUrl should return empty Optional when active configs is empty")
    void testGetSqsQueueUrl_WhenActiveConfigsEmpty_ReturnsEmptyOptional() {
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getSqsQueueUrl should cache result and not recompute on subsequent calls with same app ID")
    void testGetSqsQueueUrl_CachesResultOnSubsequentCalls() {
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> firstResult = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");
        Optional<String> secondResult = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");

        assertTrue(firstResult.isPresent());
        assertTrue(secondResult.isPresent());
        assertEquals(firstResult.get(), secondResult.get());
        assertEquals("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue", firstResult.get());
    }

    @Test
    @DisplayName("getSqsQueueUrl should handle multiple app IDs with separate cache entries")
    void testGetSqsQueueUrl_WithMultipleAppIds_ReturnsCorrectUrlForEachId() {
        CcmsConfig ccmsConfig2 = new CcmsConfig();
        ccmsConfig2.setAppEntraObjectId("app-entra-oid-456");
        UserConfig userConfig2 = new UserConfig();
        UserConfig.Api apiConfig2 = new UserConfig.Api();
        UserConfig.Sqs sqsConfig2 = new UserConfig.Sqs();
        sqsConfig2.setArn("arn:aws:sqs:eu-west-1:987654321098:another-queue");
        apiConfig2.setSqs(sqsConfig2);
        userConfig2.setApi(apiConfig2);
        ccmsConfig2.setUser(userConfig2);

        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        activeConfigs.put("app-entra-oid-456", ccmsConfig2);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result1 = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");
        Optional<String> result2 = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-456");

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue", result1.get());
        assertEquals("https://sqs.eu-west-1.amazonaws.com/987654321098/another-queue", result2.get());
    }

    @Test
    @DisplayName("getSqsQueueUrl should be case-sensitive for app ID lookup")
    void testGetSqsQueueUrl_CaseSensitiveForAppId() {
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> resultLowerCase = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");
        Optional<String> resultUpperCase = sqsClientRegistry.getSqsQueueUrl("APP-ENTRA-OID-123");

        assertTrue(resultLowerCase.isPresent());
        assertFalse(resultUpperCase.isPresent());
    }

    @Test
    @DisplayName("getSqsQueueUrl should handle 'none' ARN and return 'none' as queue URL")
    void testGetSqsQueueUrl_WithNoneArn_ReturnsNone() {
        sqsConfig.setArn("none");
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");

        assertTrue(result.isPresent());
        assertEquals("none", result.get());
    }

    @Test
    @DisplayName("getSqsClient should handle 'none' ARN and return 'none' as region")
    void testGetSqsClient_WithNoneArn_CreatesClientWithNoneRegion() {
        sqsConfig.setArn("none");
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        try (MockedStatic<SqsClient> sqsClientMock = mockStatic(SqsClient.class)) {
            SqsClient mockClient = mock(SqsClient.class);
            var builderMock = mock(software.amazon.awssdk.services.sqs.SqsClientBuilder.class);

            when(builderMock.region(any(Region.class))).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(mockClient);
            sqsClientMock.when(SqsClient::builder).thenReturn(builderMock);

            Optional<SqsClient> result = sqsClientRegistry.getSqsClient("app-entra-oid-123");

            assertTrue(result.isPresent());
            verify(builderMock).region(any(Region.class));
        }
    }

    @Test
    @DisplayName("getSqsQueueUrl should throw exception for invalid ARN format")
    void testGetSqsQueueUrl_WithInvalidArn_ThrowsException() {
        sqsConfig.setArn("invalid-arn-format");
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        try {
            sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid SQS ARN format"));
        }
    }

    @Test
    @DisplayName("getSqsClient should throw exception for invalid ARN format")
    void testGetSqsClient_WithInvalidArn_ThrowsException() {
        sqsConfig.setArn("invalid-arn-format");
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        try (MockedStatic<SqsClient> sqsClientMock = mockStatic(SqsClient.class)) {
            var builderMock = mock(software.amazon.awssdk.services.sqs.SqsClientBuilder.class);
            sqsClientMock.when(SqsClient::builder).thenReturn(builderMock);

            try {
                sqsClientRegistry.getSqsClient("app-entra-oid-123");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("Invalid SQS ARN format"));
            }
        }
    }

    @Test
    @DisplayName("getSqsQueueUrl should throw exception for malformed ARN with wrong number of parts")
    void testGetSqsQueueUrl_WithMalformedArn_ThrowsException() {
        sqsConfig.setArn("arn:aws:sqs:us-east-1:123456789012");  // Missing queue name
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        try {
            sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid SQS ARN format"));
        }
    }

    @Test
    @DisplayName("getSqsClient should throw exception for malformed ARN with wrong number of parts")
    void testGetSqsClient_WithMalformedArn_ThrowsException() {
        sqsConfig.setArn("arn:aws:sqs:us-east-1:123456789012");  // Missing queue name
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        try (MockedStatic<SqsClient> sqsClientMock = mockStatic(SqsClient.class)) {
            var builderMock = mock(software.amazon.awssdk.services.sqs.SqsClientBuilder.class);
            sqsClientMock.when(SqsClient::builder).thenReturn(builderMock);

            try {
                sqsClientRegistry.getSqsClient("app-entra-oid-123");
            } catch (IllegalArgumentException e) {
                assertTrue(e.getMessage().contains("Invalid SQS ARN format"));
            }
        }
    }

    @Test
    @DisplayName("getSqsQueueUrl and getSqsClient should work independently with different caches")
    void testGetSqsQueueUrl_And_GetSqsClient_WorkIndependently() {
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        try (MockedStatic<SqsClient> sqsClientMock = mockStatic(SqsClient.class)) {
            SqsClient mockClient = mock(SqsClient.class);
            var builderMock = mock(software.amazon.awssdk.services.sqs.SqsClientBuilder.class);

            when(builderMock.region(any(Region.class))).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(mockClient);
            sqsClientMock.when(SqsClient::builder).thenReturn(builderMock);

            Optional<SqsClient> clientResult = sqsClientRegistry.getSqsClient("app-entra-oid-123");
            Optional<String> urlResult = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");

            assertTrue(clientResult.isPresent());
            assertTrue(urlResult.isPresent());
            assertEquals("https://sqs.us-east-1.amazonaws.com/123456789012/test-queue", urlResult.get());
        }
    }

    @Test
    @DisplayName("getSqsQueueUrl should correctly parse region from ARN")
    void testGetSqsQueueUrl_ParsesRegionCorrectly() {
        sqsConfig.setArn("arn:aws:sqs:ap-southeast-2:111222333444:test-queue");
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");

        assertTrue(result.isPresent());
        assertEquals("https://sqs.ap-southeast-2.amazonaws.com/111222333444/test-queue", result.get());
    }

    @Test
    @DisplayName("getSqsQueueUrl should correctly parse account ID from ARN")
    void testGetSqsQueueUrl_ParsesAccountIdCorrectly() {
        sqsConfig.setArn("arn:aws:sqs:us-west-2:999888777666:my-queue");
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");

        assertTrue(result.isPresent());
        assertEquals("https://sqs.us-west-2.amazonaws.com/999888777666/my-queue", result.get());
    }

    @Test
    @DisplayName("getSqsQueueUrl should correctly parse queue name from ARN")
    void testGetSqsQueueUrl_ParsesQueueNameCorrectly() {
        sqsConfig.setArn("arn:aws:sqs:eu-central-1:123456789012:my-special-queue");
        activeConfigs.put("app-entra-oid-123", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = sqsClientRegistry.getSqsQueueUrl("app-entra-oid-123");

        assertTrue(result.isPresent());
        assertEquals("https://sqs.eu-central-1.amazonaws.com/123456789012/my-special-queue", result.get());
    }
}

