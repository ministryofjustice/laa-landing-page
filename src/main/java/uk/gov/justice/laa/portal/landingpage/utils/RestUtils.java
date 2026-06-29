package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for making rest calls
 */
@Slf4j
public class RestUtils {

    public static final String EMPTY_STRING = "";

    public static String getGraphApi(String accessToken, String url) {
        return callGraphApi(accessToken, url, HttpMethod.GET, null);
    }

    public static String postGraphApi(String accessToken, String url, MultiValueMap<String, String> body) {
        return callGraphApi(accessToken, url, HttpMethod.POST, body);
    }

    protected static String callGraphApi(String accessToken, String url, HttpMethod method, MultiValueMap<String, String> body) {
        try {
            RestClient restClient = RestClient.create();
            RestClient.RequestBodySpec requestSpec = restClient.method(method)
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON);

            if (body != null && !body.isEmpty()) {
                requestSpec.contentType(MediaType.APPLICATION_JSON).body(body);
            }

            String responseBody = requestSpec.retrieve()
                    .body(String.class);

            return Optional.ofNullable(responseBody).orElse(EMPTY_STRING);

        } catch (Exception e) {
            log.error("Graph API call failed [{} {}]: {}", method, url, e.getMessage());
            return EMPTY_STRING;
        }
    }

    /**
     * Gets a given object from the http session while ensuring type safety
     * @param session The http session being queried
     * @param key The key value to query
     * @param type The expected type to be returned
     * @return An optional which may or may not contain the requested object.
     * @param <T> the expected type.
     */
    public static <T> Optional<T> getObjectFromHttpSession(HttpSession session, String key, Class<T> type) {
        Object object = session.getAttribute(key);
        if (type.isInstance(object)) {
            return Optional.of(type.cast(object));
        } else {
            log.debug("Type mismatch: session attribute '{}' is null or not of the expected type", key);
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<List<T>> getListFromHttpSession(HttpSession session, String key, Class<T> elementType) {
        Object value = session.getAttribute(key);

        if (value instanceof List<?> list
                && (list.isEmpty() || list.stream().allMatch(o -> o == null || elementType.isInstance(o)))) {
            return Optional.of(new ArrayList<>((List<T>) list));
        }

        log.debug("Session attribute '{}' is missing, not a List, or contains elements not of type {}",
                key, elementType.getSimpleName());
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<Set<T>> getSetFromHttpSession(HttpSession session, String key, Class<T> elementType) {
        Object object = session.getAttribute(key);
        if (object instanceof Set<?> set
                && set.stream().allMatch(o -> o == null || elementType.isInstance(o))) {
            return Optional.of((Set<T>) set);
        }

        log.debug("Session attribute '{}' is missing, not a Set, or contains elements not of type {}",
                key, elementType.getSimpleName());

        return Optional.empty();
    }
}
