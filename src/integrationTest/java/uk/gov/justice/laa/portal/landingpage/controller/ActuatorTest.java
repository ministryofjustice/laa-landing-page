package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ActuatorTest extends BaseIntegrationTest {

    @Test
    void actuatorHealthEndpointAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void actuatorHealthEndpointAccessibleFromExternalIp() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .with(anonymous())
                        .remoteAddress("203.0.113.195"))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void shouldAllowSensitiveActuatorWhenIpIsInternal() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldBlockSensitiveActuatorWhenIpIsExternal() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .with(anonymous())
                        .remoteAddress("203.0.113.195"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/oauth2/authorization/azure"));
    }
}
