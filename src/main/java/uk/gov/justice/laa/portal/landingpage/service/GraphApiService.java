package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.callGraphApi;
import static uk.gov.justice.laa.portal.landingpage.utils.RestUtils.postGraphApi;

@Service
public class GraphApiService {

    public static final String GRAPH_URL = "https://graph.microsoft.com/v1.0";

    Logger logger = LoggerFactory.getLogger(this.getClass());

    public User getUserProfile(String accessToken) {
        String url = GRAPH_URL + "/me";
        try {
            String jsonResponse = callGraphApi(accessToken, url);
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


            return objectMapper.readValue(jsonResponse, User.class);
        } catch (Exception e) {
            logger.error("Unexpected error processing user profile", e);
        }
        return null;
    }

    public void logoutUser(String accessToken) {
        String url = GRAPH_URL + "/me" + "/revokeSignInSessions";
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            postGraphApi(accessToken, url, body);
        } catch (Exception e) {
            logger.error("Unexpected error processing logout", e);
        }
    }

}
