package uk.gov.justice.laa.portal.landingpage.service;

import com.microsoft.graph.models.AppRole;
import com.microsoft.graph.models.ServicePrincipal;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.serviceprincipals.ServicePrincipalsRequestBuilder;
import com.microsoft.graph.serviceprincipals.getbyids.GetByIdsPostRequestBody;
import com.microsoft.graph.serviceprincipals.getbyids.GetByIdsPostResponse;
import com.microsoft.graph.serviceprincipals.getbyids.GetByIdsRequestBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.justice.laa.portal.landingpage.model.LaaApplication;
import uk.gov.justice.laa.portal.landingpage.utils.RestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphApiServiceTest {

    private static final String APP_ROLE_ASSIGNMENT =
            "{\"@odata.context\": \"https://graph.microsoft.com/v1.0/$metadata#appRoleAssignments\","
            + "\"value\": [ {"
            + "\"id\": \"hxjTSK1fc02p9Tw1bmigOH5fJdqoSSNInDHAF7MOyaA\","
            + "\"deletedDateTime\": null,"
            + "\"appRoleId\": \"8b2071cd-015a-4025-8052-1c0dba2d3f64\","
            + "\"createdDateTime\": \"2017-07-31T17:45:18.9559566Z\","
            + "\"principalDisplayName\": \"Megan Bowen\","
            + "\"principalId\": \"48d31887-5fad-4d73-a9f5-3c356e68a038\","
            + "\"principalType\": \"User\","
            + "\"resourceDisplayName\": \"MOD Demo Platform UnifiedApiConsumer\","
            + "\"resourceId\": \"b7dc5c41-68a7-4416-ad5d-14a887f1c665\" }]}";

    @Mock
    private GraphServiceClient client;
    @Mock
    private ServicePrincipalsRequestBuilder servicePrincipalsRequestBuilder;
    @Mock
    private GetByIdsPostResponse getByIdsPostResponse;
    @Mock
    private GetByIdsRequestBuilder getByIdsRequestBuilder;
    @Mock
    private ApplicationContext context;
    @InjectMocks
    private GraphApiService service;

    private MockedStatic<RestUtils> mockedRestUtils;

    @BeforeAll
    static void init() {
        LaaApplication laaApp1 = LaaApplication.builder().id("a36da3a3bfb6ba026c255cc64a68f87f527bf11dbfc9e4b94730f78e977c0a4f").title("App One").build();
        LaaApplication laaApp2 = LaaApplication.builder().id("b21b9c1a0611a09a0158d831b765ffe6ded9103a1ecdbc87c706c4ce44d07be7").title("App Two").build();
        LaaApplication laaApp3 = LaaApplication.builder().id("a32d05f19e64840bf256a7128483db941410e4f86bae5c1d4a03c9514c2266a4").title("App Two").build();
        List<LaaApplication> laaApplications = List.of(laaApp1, laaApp2, laaApp3);
        ReflectionTestUtils.setField(LaaAppDetailsStore.class, "laaApplications", laaApplications);
    }

    @AfterAll
    public static void tearDown() {
        ReflectionTestUtils.setField(LaaAppDetailsStore.class, "laaApplications", null);
    }

    @BeforeEach
    void setUp() {
        mockedRestUtils = mockStatic(RestUtils.class);
    }

    @AfterEach
    void cleanUp() {
        if (mockedRestUtils != null) {
            mockedRestUtils.close();
        }
    }

    @Test
    void getUserAppsAndRolesNoAppRoleAssignments() {
        mockedRestUtils.when(() -> RestUtils.callGraphApi(anyString(), anyString())).thenReturn("");

        assertThat(service.getUserAppsAndRoles("")).isEmpty();
    }

    @Test
    void getUserAppsAndRolesNoServicePrincipals() {
        mockedRestUtils.when(() -> RestUtils.callGraphApi(anyString(), anyString()))
                .thenReturn(APP_ROLE_ASSIGNMENT)
                .thenReturn("");

        when(client.servicePrincipals()).thenReturn(servicePrincipalsRequestBuilder);
        when(servicePrincipalsRequestBuilder.getByIds()).thenReturn(getByIdsRequestBuilder);
        when(getByIdsRequestBuilder.post(any(GetByIdsPostRequestBody.class))).thenReturn(getByIdsPostResponse);
        when(getByIdsPostResponse.getValue()).thenReturn(null);
        when(context.getBean(anyString(), anyString())).thenReturn(client);

        assertThat(service.getUserAppsAndRoles("")).isEmpty();
    }

    @Test
    void getUserAppsAndRole() {
        ServicePrincipal servicePrincipal = new ServicePrincipal();
        servicePrincipal.setId("b7dc5c41-68a7-4416-ad5d-14a887f1c665");
        servicePrincipal.setAppId("870c4f2e-85b6-4d43-bdda-6ed9a579b725");

        AppRole appRole = new AppRole();
        appRole.setId(UUID.fromString("8b2071cd-015a-4025-8052-1c0dba2d3f64"));
        servicePrincipal.setAppRoles(List.of(appRole));

        mockedRestUtils.when(() -> RestUtils.callGraphApi(anyString(), anyString()))
                .thenReturn(APP_ROLE_ASSIGNMENT)
                .thenReturn("");

        when(client.servicePrincipals()).thenReturn(servicePrincipalsRequestBuilder);
        when(servicePrincipalsRequestBuilder.getByIds()).thenReturn(getByIdsRequestBuilder);
        when(getByIdsRequestBuilder.post(any(GetByIdsPostRequestBody.class))).thenReturn(getByIdsPostResponse);
        when(getByIdsPostResponse.getValue()).thenReturn(List.of(servicePrincipal));
        when(context.getBean(anyString(), anyString())).thenReturn(client);

        List<LaaApplication> results = service.getUserAppsAndRoles("token");

        assertThat(results).isNotEmpty();
        assertThat(results).hasSize(1);

        LaaApplication result = results.getFirst();
        assertThat(result.getId()).isEqualTo("870c4f2e-85b6-4d43-bdda-6ed9a579b725");
        assertThat(result.getTitle()).isEqualTo("App One");

        Set<AppRole> appRoles = result.getRole();
        assertThat(appRoles).isNotEmpty();
        assertThat(appRoles.iterator().next().getId()).isEqualTo(appRole.getId());
    }

}
