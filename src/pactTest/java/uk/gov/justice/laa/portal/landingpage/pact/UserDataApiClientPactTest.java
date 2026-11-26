package uk.gov.justice.laa.portal.landingpage.pact;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTest;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.datauserapi.contracts.domain.UserType;
import uk.gov.justice.laa.portal.landingpage.client.LiveUserDataApiClient;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

@PactConsumerTest
@PactTestFor(providerName = AbstractPactTest.PROVIDER, pactVersion = PactSpecVersion.V3)
class UserDataApiClientPactTest extends AbstractPactTest {

    private static final String USER_OID = "00000000-0000-0000-0000-000000000001";
    private static final String CORRELATION_ID = "00000000-0000-0000-0000-000000000099";
    private static final String OBO_TOKEN = "pact-test-token";

    @Pact(consumer = CONSUMER)
    RequestResponsePact signedInUserProfile(PactDslWithProvider builder) {
        return builder.given("the signed-in user has a profile")
                .uponReceiving("a request for the signed-in user's profile")
                .method("GET").path("/api/v1/me")
                .matchHeader("Authorization", "Bearer .+", "Bearer " + OBO_TOKEN)
                .matchHeader("X-Correlation-ID", UUID_REGEX, CORRELATION_ID)
                .willRespondWith().status(200)
                .body(newJsonBody(profile -> {
                    profile.stringMatcher("userProfileId", UUID_REGEX, "00000000-0000-0000-0000-000000000002");
                    profile.stringMatcher("userEntraObjectId", UUID_REGEX, USER_OID);
                    profile.stringType("email", "jane.doe@example.com");
                    profile.stringType("fullName", "Jane Doe");
                    profile.stringValue("userType", "EXTERNAL");
                    profile.booleanType("activeProfile", false);
                    profile.booleanType("multiFirmUser", false);
                    profile.booleanType("hasAppRoles", false);
                    profile.booleanType("unrestrictedOfficeAccess", false);
                    profile.booleanType("lastSyncSuccessful", true);
                }).build()).toPact();
    }

    @Test
    @PactTestFor(pactMethod = "signedInUserProfile")
    void meReturnsTheSignedInUsersProfile(MockServer mockServer) {
        OboTokenService tokens = mock(OboTokenService.class);
        when(tokens.acquireOboToken("user-access-token", USER_OID)).thenReturn(OBO_TOKEN);
        var client = new LiveUserDataApiClient(tokens, RestClient.builder().baseUrl(mockServer.getUrl()).build());

        var result = client.me("user-access-token", USER_OID, CORRELATION_ID);

        assertThat(result.userProfileId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(result.userEntraObjectId()).isEqualTo(UUID.fromString(USER_OID));
        assertThat(result.email()).isEqualTo("jane.doe@example.com");
        assertThat(result.fullName()).isEqualTo("Jane Doe");
        assertThat(result.userType()).isEqualTo(UserType.EXTERNAL);
    }
}
