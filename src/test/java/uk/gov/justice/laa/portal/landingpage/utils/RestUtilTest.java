package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestUtilTest {

    private static final String DUMMY_ACCESS_TOKEN = "test-token";
    private static final String DUMMY_URL = "https://graph.microsoft.com/v1.0/test";

    @Mock
    private RestClient mockRestClient;
    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock
    private RestClient.RequestBodySpec requestBodySpec;
    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Captor
    private ArgumentCaptor<String> headerCaptor;
    @Captor
    private ArgumentCaptor<Object> bodyCaptor;

    private MockHttpSession mockHttpSession;

    @BeforeEach
    void setUp() {
        mockHttpSession = new MockHttpSession();
    }

    @AfterEach
    void tearDown() {
        // Forces the mock back to its pristine, unstubbed state
        Mockito.reset(mockRestClient, requestBodyUriSpec, responseSpec, requestBodySpec);
    }

    private void setupMockRestClientChain() {
        lenient().when(mockRestClient.method(any(HttpMethod.class))).thenReturn(requestBodyUriSpec);
        lenient().when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.accept(any(MediaType.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.body(any())).thenReturn(requestBodySpec);
        lenient().when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void getGraphApi_whenApiCallIsSuccessfulAndBodyIsPresent_returnsResponseBody() {
        setupMockRestClientChain();
        String expectedResponseBody = "{\"data\":\"success\"}";
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(expectedResponseBody);

        // 💡 Intercept RestClient.create() static call to inject our mock blueprint
        try (MockedStatic<RestClient> staticRestClient = Mockito.mockStatic(RestClient.class)) {
            staticRestClient.when(RestClient::create).thenReturn(mockRestClient);

            String actualResponseBody = RestUtils.getGraphApi(DUMMY_ACCESS_TOKEN, DUMMY_URL);

            assertThat(actualResponseBody).isEqualTo(expectedResponseBody);
            verify(mockRestClient).method(HttpMethod.GET);
            verify(requestBodyUriSpec).uri(DUMMY_URL);
            verify(requestBodySpec).header(eq("Authorization"), headerCaptor.capture());
            assertThat(headerCaptor.getValue()).isEqualTo("Bearer " + DUMMY_ACCESS_TOKEN);
        }
    }

    @Test
    void getGraphApi_whenApiReturnsSuccessfulButNullBody_returnsEmptyString() {
        setupMockRestClientChain();

        try (MockedStatic<RestClient> staticRestClient = Mockito.mockStatic(RestClient.class)) {
            staticRestClient.when(RestClient::create).thenReturn(mockRestClient);

            String actualResponseBody = RestUtils.getGraphApi(DUMMY_ACCESS_TOKEN, DUMMY_URL);

            assertThat(actualResponseBody).isEqualTo(RestUtils.EMPTY_STRING);
        }
    }

    @Test
    void postGraphApi_whenApiCallIsSuccessfulAndBodyIsPresent_returnsResponseBody() {
        setupMockRestClientChain();
        String expectedResponseBody = "{\"data\":\"success\"}";
        when(responseSpec.body(String.class)).thenReturn(expectedResponseBody);

        try (MockedStatic<RestClient> staticRestClient = Mockito.mockStatic(RestClient.class)) {
            staticRestClient.when(RestClient::create).thenReturn(mockRestClient);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("data", "payload");

            String actualResponseBody = RestUtils.postGraphApi(DUMMY_ACCESS_TOKEN, DUMMY_URL, body);

            assertThat(actualResponseBody).isEqualTo(expectedResponseBody);
            verify(mockRestClient).method(HttpMethod.POST);
            verify(requestBodySpec).body(bodyCaptor.capture());

            MultiValueMap<String, String> payload = (MultiValueMap<String, String>) bodyCaptor.getValue();
            assertThat(payload).containsKey("data");
            assertThat(payload.get("data")).containsExactly("payload");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // HTTP SESSION UNIT TESTS (UNTOUCHED AND RETAINED AS IS)
    // ─────────────────────────────────────────────────────────────────
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
        List<String> list = new ArrayList<>();
        list.add("TestValue1");
        list.add(null);
        list.add("TestValue3");
        Object o = list;
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(o);
        Optional<List<String>> stringList = RestUtils.getListFromHttpSession(mockHttpSession, "test", String.class);
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

    @Test
    void testGettingObjectFromSessionHandlesEmpty() {
        HttpSession mockHttpSession = Mockito.mock(HttpSession.class);
        Mockito.when(mockHttpSession.getAttribute("test")).thenReturn(new ArrayList<>());
        Optional<List<String>> returnedList = RestUtils.getListFromHttpSession(mockHttpSession, "test", String.class);
        Assertions.assertTrue(returnedList.isPresent());
        Assertions.assertTrue(returnedList.get().isEmpty());
    }

    @Test
    void testGettingSetFromSessionReturnsPopulatedOptionalWhenTypeIsCorrect() {
        Object o = Set.of("TestValue1", "TestValue2", "TestValue3");
        mockHttpSession.setAttribute("test", o);
        Optional<Set<String>> stringSet = RestUtils.getSetFromHttpSession(mockHttpSession, "test", String.class);
        Assertions.assertTrue(stringSet.isPresent());
        Assertions.assertEquals(3, stringSet.get().size());
    }

    @Test
    void testGettingSettFromSessionReturnsPopulatedOptionalWhenTypeIsCorrectAndListContainsNulls() {
        Set<String> set = new HashSet<>();
        set.add("TestValue1");
        set.add(null);
        set.add("TestValue3");
        Object o = set;
        mockHttpSession.setAttribute("test", o);
        Optional<Set<String>> stringList = RestUtils.getSetFromHttpSession(mockHttpSession, "test", String.class);
        Assertions.assertTrue(stringList.isPresent());
        Assertions.assertEquals(3, stringList.get().size());
    }

    @Test
    void testGettingSetFromSessionReturnsEmptyOptionalWhenTypeIsIncorrect() {
        Object o = Set.of("TestValue1", "TestValue2", "TestValue3");
        mockHttpSession.setAttribute("test", o);
        Optional<Set<Integer>> integerSet = RestUtils.getSetFromHttpSession(mockHttpSession, "test", Integer.class);
        Assertions.assertTrue(integerSet.isEmpty());
    }

    @Test
    void testGettingSetFromSessionHandlesNulls() {
        mockHttpSession.removeAttribute("test");
        Optional<Set<String>> returnedInt = RestUtils.getSetFromHttpSession(mockHttpSession, "test", String.class);
        Assertions.assertTrue(returnedInt.isEmpty());
    }

    @Test
    void testGettingSetObjectFromSessionHandlesEmpty() {
        mockHttpSession.setAttribute("test", new HashSet<>());
        Optional<Set<String>> returnedSet = RestUtils.getSetFromHttpSession(mockHttpSession, "test", String.class);
        Assertions.assertTrue(returnedSet.isPresent());
        Assertions.assertTrue(returnedSet.get().isEmpty());
    }
}
