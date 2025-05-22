package uk.gov.justice.laa.portal.landingpage.utils;

import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Utility class for making rest calls
 */
@Slf4j
public class RestUtils {

    public static final String EMPTY_STRING = "";

    public static String callGraphApi(String accessToken, String url) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Accept", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

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
            log.warn("Type mismatch: session attribute '{}' is null or not of the expected type", key);
            return Optional.empty();
        }
    }

    public static <T> Optional<List<T>> getListFromHttpSession(HttpSession session, String key, Class<T> listType) {
        Object object = session.getAttribute(key);
        if (object instanceof List<?> list && list.stream().allMatch(o -> o == null || listType.isInstance(o))) {
            return Optional.of((List<T>) list);
        } else {
            log.warn("Type mismatch: session attribute '{}' is null or not of the expected type", key);
            return Optional.empty();
        }
    }



}
