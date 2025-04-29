package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.AppRoleAssignment;
import com.microsoft.graph.models.User;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class GraphApiServiceTest {

    private static final String APP_ROLE_JSON =
            "{" + "\"value\":[{" + "\"resourceId\":\"11111111-1111-1111-1111-111111111111\"," + "\"resourceDisplayName\":\"Test App\"}]}";
    private static final String USER_PROFILE_JSON =
            "{\"displayName\":\"Test User\",\"userPrincipalName\":\"test@example.com\"}";
    private static final String SIGN_IN_JSON =
            "{\"signInActivity\":{\"lastSignInDateTime\":\"2024-01-01T10:00:00Z\"}}";
    private static final String MEMBER_OF_JSON =
            "{\"value\":[{\"displayName\":\"Role A\",\"description\":\"Role A description\"}]}";

    // Placeholder test - technically works but needs to be refactored to Arrange Act Assert
    @Test
    void endpointsAreParsedCorrectly() {
        try (MockedConstruction<RestTemplate> ignored = mockConstruction(RestTemplate.class, (mock, ctx) -> when(mock.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            if (url.contains("appRoleAssignments")) {
                return new ResponseEntity<>(APP_ROLE_JSON, HttpStatus.OK);
            } else if (url.contains("signInActivity")) {
                return new ResponseEntity<>(SIGN_IN_JSON, HttpStatus.OK);
            } else if (url.contains("memberOf")) {
                return new ResponseEntity<>(MEMBER_OF_JSON, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(USER_PROFILE_JSON, HttpStatus.OK);
            }
        }))) {

            GraphApiService service = new GraphApiService();
            String token = "dummy";

            // 1. App‑role assignments
            List<AppRoleAssignment> assignments = service.getAppRoleAssignments(token);
            assertEquals(1, assignments.size());
            assertEquals("Test App", assignments.getFirst().getResourceDisplayName());

            // 2. User profile
            User profile = service.getUserProfile(token);
            assertEquals("Test User", profile.getDisplayName());

            // 3. Last sign‑in time
            LocalDateTime signIn = service.getLastSignInTime(token);
            assertEquals(2024, signIn.getYear());

            // 4. Groups / roles
            List<AppRole> roles = service.getUserAssignedApps(token);
            assertEquals(1, roles.size());
            assertEquals("Role A", roles.getFirst().getDisplayName());
        }
    }
}
