package uk.gov.justice.laa.portal.landingpage.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestUtilTest {

    private static final String DUMMY_ACCESS_TOKEN = "test-token";
    private static final String DUMMY_URL = "https://graph.microsoft.com/v1.0/test";

    @Mock
    private RestTemplate mockRestTemplate;
    @Captor
    private ArgumentCaptor<HttpEntity<String>> httpEntityCaptor;

    @Test
    void callGraphApi_whenApiCallIsSuccessfulAndBodyIsPresent_returnsResponseBody() {

        // Arrange
        String expectedResponseBody = "{\"data\":\"success\"}";
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(expectedResponseBody, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = Mockito.mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(eq(DUMMY_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(mockResponseEntity))) {

            // Act
            String actualResponseBody = RestUtils.callGraphApi(DUMMY_ACCESS_TOKEN, DUMMY_URL);

            // Assert
            assertThat(actualResponseBody).isEqualTo(expectedResponseBody);
            RestTemplate constructedRestTemplate = mockedConstruction.constructed().get(0);
            verify(constructedRestTemplate).exchange(
                    eq(DUMMY_URL),
                    eq(HttpMethod.GET),
                    httpEntityCaptor.capture(),
                    eq(String.class)
            );
            HttpHeaders capturedHeaders = httpEntityCaptor.getValue().getHeaders();
            assertThat(capturedHeaders.getFirst("Authorization")).isEqualTo("Bearer " + DUMMY_ACCESS_TOKEN);
            assertThat(capturedHeaders.getFirst("Accept")).isEqualTo("application/json");
        }
    }

    @Test
    void callGraphApi_whenApiReturnsSuccessfulButNullBody_returnsEmptyString() {

        // Arrange
        ResponseEntity<String> mockResponseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        try (MockedConstruction<RestTemplate> mockedConstruction = Mockito.mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(eq(DUMMY_URL), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(mockResponseEntity))) {
            // Act
            String actualResponseBody = RestUtils.callGraphApi(DUMMY_ACCESS_TOKEN, DUMMY_URL);

            // Assert
            assertThat(actualResponseBody).isEqualTo(RestUtils.EMPTY_STRING);

            RestTemplate constructedRestTemplate = mockedConstruction.constructed().get(0);
            verify(constructedRestTemplate).exchange(
                    eq(DUMMY_URL),
                    eq(HttpMethod.GET),
                    any(HttpEntity.class),
                    eq(String.class)
            );
        }
    }

}