package uk.gov.justice.laa.portal.landingpage.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

/**
 * Tests for DataProviderConfig.
 */
class DataProviderConfigTest {

    private DataProviderConfig config;

    @BeforeEach
    void setUp() {
        config = new DataProviderConfig();
        ReflectionTestUtils.setField(config, "dataProviderBaseUrl", "https://test-api.example.com");
        ReflectionTestUtils.setField(config, "dataProviderApiKey", "test-api-key");
        ReflectionTestUtils.setField(config, "dataProviderReqReadTimeout", 30);
        ReflectionTestUtils.setField(config, "dataProviderReqConnectTimeout", 15);
        ReflectionTestUtils.setField(config, "useLocalFile", false);
        ReflectionTestUtils.setField(config, "localFilePath", "test/path/data.json");
    }

    @Test
    void shouldCreateDataProviderRestClient() {
        // When
        RestClient restClient = config.dataProviderRestClient();

        // Then
        assertThat(restClient).isNotNull();
    }

    @Test
    void shouldReturnUseLocalFileFlag() {
        // When
        boolean useLocalFile = config.isUseLocalFile();

        // Then
        assertThat(useLocalFile).isFalse();
    }

    @Test
    void shouldReturnLocalFilePath() {
        // When
        String localFilePath = config.getLocalFilePath();

        // Then
        assertThat(localFilePath).isEqualTo("test/path/data.json");
    }

    @Test
    void shouldHandleLocalFileMode() {
        // Given
        ReflectionTestUtils.setField(config, "useLocalFile", true);

        // When
        boolean useLocalFile = config.isUseLocalFile();

        // Then
        assertThat(useLocalFile).isTrue();
    }

    @Test
    void shouldHandleCustomTimeouts() {
        // Given
        ReflectionTestUtils.setField(config, "dataProviderReqReadTimeout", 60);
        ReflectionTestUtils.setField(config, "dataProviderReqConnectTimeout", 20);

        // When
        RestClient restClient = config.dataProviderRestClient();

        // Then
        assertThat(restClient).isNotNull();
    }

    @Test
    void shouldHandleEmptyLocalFilePath() {
        // Given
        ReflectionTestUtils.setField(config, "localFilePath", "");

        // When
        String localFilePath = config.getLocalFilePath();

        // Then
        assertThat(localFilePath).isEmpty();
    }

    @Test
    void shouldHandleNullLocalFilePath() {
        // Given
        ReflectionTestUtils.setField(config, "localFilePath", null);

        // When
        String localFilePath = config.getLocalFilePath();

        // Then
        assertThat(localFilePath).isNull();
    }
}
