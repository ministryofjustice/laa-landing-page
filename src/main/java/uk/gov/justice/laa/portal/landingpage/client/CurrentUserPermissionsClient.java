package uk.gov.justice.laa.portal.landingpage.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import uk.gov.justice.laa.portal.landingpage.dto.CurrentUserPermissions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CurrentUserPermissionsClient {

    private static final Pattern PERMISSION_NAME = Pattern.compile("[A-Z][A-Z0-9_]*");
    private final RestClient restClient;

    public CurrentUserPermissionsClient(@Qualifier("userDataApiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public CurrentUserPermissions load(String accessToken) {
        try {
            ResponseEntity<JsonNode> response = restClient.get()
                    .uri("/current-user/permissions")
                    .accept(MediaType.APPLICATION_JSON, MediaType.APPLICATION_PROBLEM_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .onStatus(status -> status.value() != 200, (request, error) -> {
                        throw new UserDataApiClientException("Unable to load current-user permissions",
                                error.getStatusCode().value());
                    })
                    .toEntity(JsonNode.class);
            MediaType contentType = response.getHeaders().getContentType();
            if (contentType == null || !MediaType.APPLICATION_JSON.equalsTypeAndSubtype(contentType)) {
                throw unavailable();
            }
            JsonNode body = response.getBody();
            JsonNode permissions = body == null ? null : body.get("permissions");
            if (permissions == null || !permissions.isArray()) {
                throw unavailable();
            }
            List<String> names = new ArrayList<>();
            String previous = "";
            for (JsonNode permission : permissions) {
                if (!permission.isString()) {
                    throw unavailable();
                }
                String name = permission.asString();
                if (!PERMISSION_NAME.matcher(name).matches() || previous.compareTo(name) >= 0) {
                    throw unavailable();
                }
                names.add(name);
                previous = name;
            }
            return new CurrentUserPermissions(names);
        } catch (RestClientException e) {
            // Do not attach the response or parser exception; it can contain upstream content.
            throw unavailable();
        }
    }

    private UserDataApiClientException unavailable() {
        return new UserDataApiClientException("Current-user permissions unavailable", HttpStatus.SERVICE_UNAVAILABLE.value());
    }
}
