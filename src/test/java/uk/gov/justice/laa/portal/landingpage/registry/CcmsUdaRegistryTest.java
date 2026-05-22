package uk.gov.justice.laa.portal.landingpage.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConfig;
import uk.gov.justice.laa.portal.landingpage.config.ccms.CcmsConnectionConfigProperties;
import uk.gov.justice.laa.portal.landingpage.config.ccms.UdaConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CcmsUdaRegistry Tests")
class CcmsUdaRegistryTest {

    @Mock
    private CcmsConnectionConfigProperties properties;

    @InjectMocks
    private CcmsUdaRegistry ccmsUdaRegistry;

    private CcmsConfig ccmsConfig;
    private Map<String, CcmsConfig> activeConfigs;
    private static final String TEST_APP_ENTRA_OID = "test-app-entra-oid-123";

    @BeforeEach
    void setUp() {
        activeConfigs = new HashMap<>();

        // Initialize UDA config
        UdaConfig.Api apiConfig = new UdaConfig.Api();
        apiConfig.setKey("test-api-key");

        UdaConfig udaConfig = new UdaConfig();
        udaConfig.setBaseUrl("http://uda-host:8080");
        udaConfig.setApi(apiConfig);

        // Initialize CCMS config
        ccmsConfig = new CcmsConfig();
        ccmsConfig.setAppEntraObjectId(TEST_APP_ENTRA_OID);
        ccmsConfig.setUda(udaConfig);
    }

    @Test
    @DisplayName("getUdaBaseUrl should return Optional with base URL when app ID exists in active configs")
    void testGetUdaBaseUrl_WhenAppIdExists_ReturnsOptionalWithBaseUrl() {
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID);

        assertTrue(result.isPresent());
        assertEquals("http://uda-host:8080", result.get());
    }

    @Test
    @DisplayName("getUdaBaseUrl should return empty Optional when app ID does not exist in active configs")
    void testGetUdaBaseUrl_WhenAppIdNotExists_ReturnsEmptyOptional() {
        activeConfigs.put("other-app-id", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = ccmsUdaRegistry.getUdaBaseUrl("non-existent-app-id");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getUdaBaseUrl should return empty Optional when active configs is empty")
    void testGetUdaBaseUrl_WhenActiveConfigsEmpty_ReturnsEmptyOptional() {
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getUdaBaseUrl should cache result and not recompute on subsequent calls")
    void testGetUdaBaseUrl_CachesResultOnSubsequentCalls() {
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        // First call
        Optional<String> firstResult = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID);

        // Second call
        Optional<String> secondResult = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID);

        assertTrue(firstResult.isPresent());
        assertTrue(secondResult.isPresent());
        assertEquals(firstResult.get(), secondResult.get());
        // getActiveConfigsByAppId should be called twice (once per method call)
        verify(properties, times(2)).getActiveConfigsByAppId();
    }

    @Test
    @DisplayName("getUdaBaseUrl should handle multiple app IDs correctly")
    void testGetUdaBaseUrl_WithMultipleAppIds_ReturnsCorrectUrlForEachId() {
        CcmsConfig ccmsConfig2 = new CcmsConfig();
        ccmsConfig2.setAppEntraObjectId("test-app-entra-oid-456");
        UdaConfig udaConfig2 = new UdaConfig();
        udaConfig2.setBaseUrl("http://uda-host-2:8080");
        UdaConfig.Api apiConfig2 = new UdaConfig.Api();
        apiConfig2.setKey("test-api-key-2");
        udaConfig2.setApi(apiConfig2);
        ccmsConfig2.setUda(udaConfig2);

        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        activeConfigs.put("test-app-entra-oid-456", ccmsConfig2);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result1 = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID);
        Optional<String> result2 = ccmsUdaRegistry.getUdaBaseUrl("test-app-entra-oid-456");

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals("http://uda-host:8080", result1.get());
        assertEquals("http://uda-host-2:8080", result2.get());
    }

    @Test
    @DisplayName("getUdaApiKey should return Optional with API key when app ID exists in active configs")
    void testGetUdaApiKey_WhenAppIdExists_ReturnsOptionalWithApiKey() {
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID);

        assertTrue(result.isPresent());
        assertEquals("test-api-key", result.get());
    }

    @Test
    @DisplayName("getUdaApiKey should return empty Optional when app ID does not exist in active configs")
    void testGetUdaApiKey_WhenAppIdNotExists_ReturnsEmptyOptional() {
        activeConfigs.put("other-app-id", ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = ccmsUdaRegistry.getUdaApiKey("non-existent-app-id");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getUdaApiKey should return empty Optional when active configs is empty")
    void testGetUdaApiKey_WhenActiveConfigsEmpty_ReturnsEmptyOptional() {
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getUdaApiKey should cache result and not recompute on subsequent calls")
    void testGetUdaApiKey_CachesResultOnSubsequentCalls() {
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        // First call
        Optional<String> firstResult = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID);

        // Second call
        Optional<String> secondResult = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID);

        assertTrue(firstResult.isPresent());
        assertTrue(secondResult.isPresent());
        assertEquals(firstResult.get(), secondResult.get());
        // getActiveConfigsByAppId should be called twice (once per method call)
        verify(properties, times(2)).getActiveConfigsByAppId();
    }

    @Test
    @DisplayName("getUdaApiKey should handle multiple app IDs correctly")
    void testGetUdaApiKey_WithMultipleAppIds_ReturnsCorrectKeyForEachId() {
        CcmsConfig ccmsConfig2 = new CcmsConfig();
        ccmsConfig2.setAppEntraObjectId("test-app-entra-oid-456");
        UdaConfig udaConfig2 = new UdaConfig();
        udaConfig2.setBaseUrl("http://uda-host-2:8080");
        UdaConfig.Api apiConfig2 = new UdaConfig.Api();
        apiConfig2.setKey("test-api-key-2");
        udaConfig2.setApi(apiConfig2);
        ccmsConfig2.setUda(udaConfig2);

        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        activeConfigs.put("test-app-entra-oid-456", ccmsConfig2);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result1 = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID);
        Optional<String> result2 = ccmsUdaRegistry.getUdaApiKey("test-app-entra-oid-456");

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals("test-api-key", result1.get());
        assertEquals("test-api-key-2", result2.get());
    }

    @Test
    @DisplayName("getUdaBaseUrl and getUdaApiKey should work independently with different caches")
    void testGetUdaBaseUrl_And_GetUdaApiKey_WorkIndependently() {
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> baseUrl = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID);
        Optional<String> apiKey = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID);

        assertTrue(baseUrl.isPresent());
        assertTrue(apiKey.isPresent());
        assertEquals("http://uda-host:8080", baseUrl.get());
        assertEquals("test-api-key", apiKey.get());
    }

    @Test
    @DisplayName("getUdaBaseUrl should handle empty string base URL")
    void testGetUdaBaseUrl_WithEmptyBaseUrl_ReturnsOptionalWithEmptyString() {
        ccmsConfig.getUda().setBaseUrl("");
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID);

        assertTrue(result.isPresent());
        assertEquals("", result.get());
    }

    @Test
    @DisplayName("getUdaApiKey should handle empty string API key")
    void testGetUdaApiKey_WithEmptyApiKey_ReturnsOptionalWithEmptyString() {
        ccmsConfig.getUda().getApi().setKey("");
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> result = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID);

        assertTrue(result.isPresent());
        assertEquals("", result.get());
    }

    @Test
    @DisplayName("getUdaBaseUrl should be case-sensitive for app ID lookup")
    void testGetUdaBaseUrl_CaseSensitiveForAppId() {
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> resultLowerCase = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID);
        Optional<String> resultUpperCase = ccmsUdaRegistry.getUdaBaseUrl(TEST_APP_ENTRA_OID.toUpperCase());

        assertTrue(resultLowerCase.isPresent());
        assertFalse(resultUpperCase.isPresent());
    }

    @Test
    @DisplayName("getUdaApiKey should be case-sensitive for app ID lookup")
    void testGetUdaApiKey_CaseSensitiveForAppId() {
        activeConfigs.put(TEST_APP_ENTRA_OID, ccmsConfig);
        when(properties.getActiveConfigsByAppId()).thenReturn(activeConfigs);

        Optional<String> resultLowerCase = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID);
        Optional<String> resultUpperCase = ccmsUdaRegistry.getUdaApiKey(TEST_APP_ENTRA_OID.toUpperCase());

        assertTrue(resultLowerCase.isPresent());
        assertFalse(resultUpperCase.isPresent());
    }
}

