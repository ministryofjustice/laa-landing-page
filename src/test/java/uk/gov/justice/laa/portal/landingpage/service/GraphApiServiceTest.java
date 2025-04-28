package uk.gov.justice.laa.portal.landingpage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.AppRoleAssignment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GraphApiServiceTest {

    @InjectMocks
    private GraphApiService service;               // SUT

    @Mock
    private RestTemplate restTemplate;             // injected via reflection at runtime

    @Mock
    private ObjectMapper mapper;                   // lets us stub JSON mapping errors

    @Test
    void returnsParsedAssignments_whenGraphResponds200() throws Exception {
        // --- Arrange -------------------------------------------------------
        String token = "fake-token";
        String url = "https://graph.microsoft.com/v1.0/me/appRoleAssignments";

        String json = """
                { "value":[
                  { "resourceId":"11111111-1111-1111-1111-111111111111",
                    "resourceDisplayName":"Some API" }
                ]}""";

        when(restTemplate.exchange(eq(url), eq(org.springframework.http.HttpMethod.GET),
                any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(json));

        // let ObjectMapper deserialize to a minimal tree
        when(mapper.readTree(json)).thenCallRealMethod();   // use real Jackson

        // --- Act -----------------------------------------------------------
        List<AppRoleAssignment> result = service.getAppRoleAssignments(token);

        // --- Assert --------------------------------------------------------
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResourceDisplayName()).isEqualTo("Some API");
    }

    @Test
    void returnsEmpty_whenJsonMalformed() throws Exception {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{notValidJson"));

        when(mapper.readTree(anyString()))
                .thenThrow(new RuntimeException("boom"));   // simulate parse error

        List<AppRoleAssignment> result = service.getAppRoleAssignments("t");
        assertThat(result).isEmpty();                       // graceful fallback
    }



}