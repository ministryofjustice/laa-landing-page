package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");

        HttpEntity<?> entity = new HttpEntity<Object>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url, method, entity, String.class);

        return Optional.ofNullable(response.getBody()).orElse(EMPTY_STRING);
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

    public static <T> Optional<List<T>> getListFromHttpSession(HttpSession session, String key, Class<T> listType) {
        Object object = session.getAttribute(key);
        if (object instanceof List<?> list && list.stream().allMatch(o -> o == null || listType.isInstance(o))) {
            return Optional.of((List<T>) list);
        } else {
            log.debug("Type mismatch: session attribute '{}' is null or not of the expected type", key);
            return Optional.empty();
        }
    }

    public static <T> Optional<Set<T>> getSetFromHttpSession(HttpSession session, String key, Class<T> setType) {
        Object object = session.getAttribute(key);
        if (object instanceof Set<?> set && set.stream().allMatch(o -> o == null || setType.isInstance(o))) {
            return Optional.of((Set<T>) set);
        } else {
            log.debug("Type mismatch: session attribute '{}' is null or not of the expected type", key);
            return Optional.empty();
        }
    }

}
