package uk.gov.justice.laa.portal.landingpage.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new JacksonConfig().objectMapper();
    }

    @Test
    void objectMapper_shouldHandleJavaTimeTypes() throws Exception {
        var dateTime = LocalDateTime.of(2025, 6, 23, 16, 28, 12);
        var expectedJson = "\"2025-06-23T16:28:12\"";

        var actualJson = objectMapper.writeValueAsString(dateTime);
        var deserializedDateTime = objectMapper.readValue(expectedJson, LocalDateTime.class);

        assertThat(actualJson).isEqualTo(expectedJson);
        assertThat(deserializedDateTime).isEqualTo(dateTime);
    }
}