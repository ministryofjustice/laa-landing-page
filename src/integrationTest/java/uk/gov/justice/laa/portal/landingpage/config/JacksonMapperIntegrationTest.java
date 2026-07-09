package uk.gov.justice.laa.portal.landingpage.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;
import uk.gov.justice.laa.portal.landingpage.controller.BaseIntegrationTest;
import uk.gov.justice.laa.portal.landingpage.techservices.GetUserResponse;

public class JacksonMapperIntegrationTest extends BaseIntegrationTest {

    private static final String TEST_TECH_SERVICES_USER_RESPONSE =
            """
                    {
                        "success": true,
                        "user": {
                            "id": "11111111-2222-3333-4444-555555555555",
                            "displayName": "Test User",
                            "givenName": "Test",
                            "surname": "User",
                            "email": "test@test.com",
                            "alias": [
                                "test@test.com"
                            ],
                            "accountEnabled": true,
                            "createdDateTime": "1970-01-01T00:00:00Z",
                            "lastSignIn": "1970-01-01T00:00:00Z",
                            "groups": [
                                "11111111-2222-3333-4444-555555555555"
                            ],
                            "customSecurityAttributes": {
                                "GuestUserStatus": {
                                    "@odata.type": "#microsoft.graph.customSecurityAttributeValue",
                                    "DisabledReason": "UserRequest"
                                }
                            },
                            "isMailOnly": false,
                            "deleted": false
                        },
                        "verification": null
                    }
            """;


    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ObjectMapper objectMapper;

    @Test
    public void testGetUserTechServicesResponseBindsWithoutAccountEnabledProperty() {
        // Remove account enabled attribute from test JSON
        String json = TEST_TECH_SERVICES_USER_RESPONSE.replace("\"accountEnabled\": true,", "");
        // Ensure it still binds without exception with property missing
        objectMapper.readValue(json, GetUserResponse.class);
    }
}
