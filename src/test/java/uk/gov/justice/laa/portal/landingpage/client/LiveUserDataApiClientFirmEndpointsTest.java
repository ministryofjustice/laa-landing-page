package uk.gov.justice.laa.portal.landingpage.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.gov.justice.laa.datauserapi.contracts.domain.DeactivateUserReason;
import uk.gov.justice.laa.datauserapi.contracts.request.BulkDeactivateFirmUsersRequest;
import uk.gov.justice.laa.datauserapi.contracts.response.CommandResult;
import uk.gov.justice.laa.datauserapi.contracts.response.FirmPageResponse;
import uk.gov.justice.laa.portal.landingpage.service.OboTokenService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class LiveUserDataApiClientFirmEndpointsTest {

    private static final String BASE = "http://data-api";

    @Mock
    private OboTokenService oboTokenService;

    private MockRestServiceServer server;
    private LiveUserDataApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE)
                .defaultHeader("Accept", "application/json");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new LiveUserDataApiClient(oboTokenService, builder.build());
        when(oboTokenService.acquireOboToken("user", "oid")).thenReturn("obo");
    }

    @Test
    void queryFirms_sendsPagingFirmAndSortParams() {
        server.expect(requestTo(BASE
                        + "/api/v1/queries/firms?page_number=1&page_size=10&sort_by=code,desc&firm=a%20b"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer obo"))
                .andExpect(header("X-Correlation-ID", "cid"))
                .andRespond(withSuccess(
                        "{\"items\":[],\"page\":{\"page_number\":1,\"page_size\":10,\"total_elements\":0,\"total_pages\":0}}",
                        MediaType.APPLICATION_JSON));

        FirmPageResponse result = client.queryFirms("user", "oid", "cid", "a b", 1, 10, "code,desc");

        assertThat(result.items()).isEmpty();
        server.verify();
    }

    @Test
    void queryFirms_omitsFirmParamWhenBlank() {
        server.expect(requestTo(BASE + "/api/v1/queries/firms?page_number=0&page_size=10&sort_by=name,asc"))
                .andRespond(withSuccess(
                        "{\"items\":[],\"page\":{\"page_number\":0,\"page_size\":10,\"total_elements\":0,\"total_pages\":0}}",
                        MediaType.APPLICATION_JSON));

        client.queryFirms("user", "oid", "cid", " ", 0, 10, "name,asc");

        server.verify();
    }

    @Test
    void queryFirmById_mapsNotFoundToClientException() {
        String id = "25966";
        server.expect(requestTo(BASE + "/api/v1/queries/firms/" + id))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"detail\":\"internal detail\"}"));

        assertThatThrownBy(() -> client.queryFirmById("user", "oid", "cid", id))
                .isInstanceOfSatisfying(UserDataApiClientException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(404);
                    assertThat(e.getMessage()).doesNotContain("internal detail");
                });
    }

    @Test
    void queryFirmOffices_mapsGatewayErrors() {
        String id = "25966";
        server.expect(requestTo(BASE + "/api/v1/queries/firms/" + id + "/offices"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> client.queryFirmOffices("user", "oid", "cid", id))
                .isInstanceOfSatisfying(UserDataApiClientException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(502));
    }

    @Test
    void bulkDeactivateFirmUsers_postsRequestBody() {
        String id = "25966";
        server.expect(requestTo(BASE + "/api/v1/commands/firms/bulk-deactivate-users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"firmId\":\"" + id + "\",\"deactivateReason\":\"CyberRisk\"}"))
                .andRespond(withSuccess("{\"success\":true,\"message\":\"done\"}", MediaType.APPLICATION_JSON));

        CommandResult result = client.bulkDeactivateFirmUsers("user", "oid", "cid",
                new BulkDeactivateFirmUsersRequest(id.toString(), DeactivateUserReason.CyberRisk));

        assertThat(result.success()).isTrue();
        server.verify();
    }

    @Test
    void bulkDeactivateFirmUsers_mapsBadRequest() {
        server.expect(requestTo(BASE + "/api/v1/commands/firms/bulk-deactivate-users"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.bulkDeactivateFirmUsers("user", "oid", "cid",
                new BulkDeactivateFirmUsersRequest(UUID.randomUUID().toString(), DeactivateUserReason.CyberRisk)))
                .isInstanceOfSatisfying(UserDataApiClientException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(400));
    }
}
