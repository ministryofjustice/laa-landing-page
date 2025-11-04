package uk.gov.justice.laa.portal.landingpage.utils;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SessionUtils {


    /**
     * Safely retrieves a List<String> from the session using the given key.
     * If the attribute is missing or not a List, returns an empty list.
     *
     * @param session the HttpSession object
     * @param key the session attribute key
     * @return a List<String> from the session or an empty list
     */
    public static List<String> getStringListFromSession(HttpSession session, String key) {
        Object attribute = session.getAttribute(key);
        if (attribute instanceof List<?>) {
            return ((List<?>) attribute).stream()
                    .filter(item -> item instanceof String)
                    .map(item -> (String) item)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

