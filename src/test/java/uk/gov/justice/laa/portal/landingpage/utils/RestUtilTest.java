package uk.gov.justice.laa.portal.landingpage.utils;

import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import jakarta.servlet.http.HttpSession;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Test
    void testGettingObjectFromSessionReturnsPopulatedOptionalWhenTypeIsCorrect() {
        Object o = "TestValue";
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(o);
        Optional<String> returnedString = RestUtils.getObjectFromHttpSession(mockHttpSession, "test", String.class);
        Assertions.assertTrue(returnedString.isPresent());
        Assertions.assertEquals(o, returnedString.get());
    }

    @Test
    void testGettingObjectFromSessionReturnsEmptyOptionalWhenTypeIsIncorrect() {
        Object o = "TestValue";
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(o);
        Optional<Integer> returnedInt = RestUtils.getObjectFromHttpSession(mockHttpSession, "test", Integer.class);
        Assertions.assertTrue(returnedInt.isEmpty());
    }

    @Test
    void testGettingObjectFromSessionHandlesNulls() {
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(null);
        Optional<Integer> returnedInt = RestUtils.getObjectFromHttpSession(mockHttpSession, "test", Integer.class);
        Assertions.assertTrue(returnedInt.isEmpty());
    }

    @Test
    void testGettingListFromSessionReturnsPopulatedOptionalWhenTypeIsCorrect() {
        Object o = List.of("TestValue1", "TestValue2", "TestValue3");
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(o);
        Optional<List<String>> stringList = RestUtils.getListFromHttpSession(mockHttpSession, "test", String.class);
        Assertions.assertTrue(stringList.isPresent());
        Assertions.assertEquals(3, stringList.get().size());
    }

    @Test
    void testGettingListFromSessionReturnsPopulatedOptionalWhenTypeIsCorrectAndListContainsNulls() {
        // Given

        // Setup list with some null values
        List<String> list = new ArrayList<>();
        list.add("TestValue1");
        list.add(null);
        list.add("TestValue3");
        Object o = list;
        // Have httpSession return above list as object.
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(o);
        // When
        Optional<List<String>> stringList = RestUtils.getListFromHttpSession(mockHttpSession, "test", String.class);

        // Then
        Assertions.assertTrue(stringList.isPresent());
        Assertions.assertEquals(3, stringList.get().size());
    }

    @Test
    void testGettingListFromSessionReturnsEmptyOptionalWhenTypeIsIncorrect() {
        Object o = List.of("TestValue1", "TestValue2", "TestValue3");
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(o);
        Optional<List<Integer>> integerList = RestUtils.getListFromHttpSession(mockHttpSession, "test", Integer.class);
        Assertions.assertTrue(integerList.isEmpty());
    }

    @Test
    void testGettingListFromSessionHandlesNulls() {
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(null);
        Optional<List<String>> returnedInt = RestUtils.getListFromHttpSession(mockHttpSession, "test", String.class);
        Assertions.assertTrue(returnedInt.isEmpty());
    }

}