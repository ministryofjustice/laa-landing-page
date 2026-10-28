package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.justice.laa.portal.landingpage.client.CreateExternalUserResult;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.dto.EntraUserDto;
import uk.gov.justice.laa.portal.landingpage.dto.FirmDto;
import uk.gov.justice.laa.portal.landingpage.service.ExternalUserCreationApiService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class RoleBasedAccessApiCreateUserTest extends RoleBasedAccessIntegrationTest {

    @MockitoBean private DataApiFeatureFlagService flags;
    @MockitoBean private ExternalUserCreationApiService api;

    @BeforeEach
    void enableApi() {
        when(flags.isCreateUserDataApiEnabled()).thenReturn(true);
    }

    @Test
    void authorisedCreationRendersConfirmationWithoutPersistingUserLocally() throws Exception {
        MockHttpSession session = journey();
        when(api.create(any(), any(), eq(false))).thenReturn(
                new CreateExternalUserResult(true, UUID.randomUUID(), UUID.randomUUID()));
        mockMvc.perform(post("/admin/user/create/check-answers").session(session).with(csrf())
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(redirectedUrl("/admin/user/create/confirmation"));
        mockMvc.perform(get("/admin/user/create/confirmation").session(session)
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("User created")))
                .andExpect(content().string(containsString("No roles or office access have been assigned")));
        assertThat(entraUserRepository.findAll()).noneMatch(user -> "new-api-user@example.org".equals(user.getEmail()));
    }

    @Test
    void viewerCannotCreateOrSearchFirmsInCreateJourney() throws Exception {
        mockMvc.perform(post("/admin/user/create/check-answers").session(journey()).with(csrf())
                        .with(userOauth2Login(externalUserViewers.getFirst())))
                .andExpect(redirectedUrl("/not-authorised"));
        mockMvc.perform(get("/admin/user/create/firm/search").param("q", "test")
                        .with(userOauth2Login(externalUserViewers.getFirst())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.redirect").value("/not-authorised"));
        verifyNoInteractions(api);
    }

    @Test
    void missingCsrfCannotSubmitCreate() throws Exception {
        mockMvc.perform(post("/admin/user/create/check-answers").session(journey())
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl())
                        .isNotEqualTo("/admin/user/create/confirmation"));
        verifyNoInteractions(api);
    }

    @Test
    void detailsFirmAndCheckAnswersTemplatesPreserveInputAndExplainDeferredAccess() throws Exception {
        MockHttpSession session = journey();
        mockMvc.perform(get("/admin/user/create/details").session(session)
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("This answer does not grant access")));
        mockMvc.perform(get("/admin/user/create/firm").session(session)
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"selectedFirmCode\"")))
                .andExpect(content().string(containsString("value=\"00123\"")));
        mockMvc.perform(get("/admin/user/create/check-answers").session(session)
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Requested access")));
    }

    @Test
    void firmAutocompletePreservesResponseShapeAndCodes() throws Exception {
        when(api.searchFirms("Test", 10)).thenReturn(List.of(FirmDto.builder().code("00123").name("Test firm").build()));
        mockMvc.perform(get("/admin/user/create/firm/search").param("q", "Test")
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00123"))
                .andExpect(jsonPath("$[0].code").value("00123"));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 409})
    void rejectedCreationStaysInFlowAndDoesNotExposePayload(int code) throws Exception {
        when(api.create(any(), any(), eq(false))).thenThrow(new UserDataApiClientException("RAW PRIVATE DATA", code));
        MockHttpSession session = journey();
        mockMvc.perform(post("/admin/user/create/check-answers").session(session).with(csrf())
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(status().isOk())
                .andExpect(view().name("add-user-check-answers"))
                .andExpect(content().string(containsString("There is a problem")))
                .andExpect(content().string(not(containsString("RAW PRIVATE DATA"))));
        assertThat(session.getAttribute("createdUserResult")).isNull();
        assertThat(session.getAttribute("firm")).isNotNull();
    }

    @Test
    void cancellationDoesNotCallApi() throws Exception {
        MockHttpSession session = journey();
        mockMvc.perform(get("/admin/user/create/cancel/confirmation").session(session)
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/user/create/cancel").session(session)
                        .with(userOauth2Login(globalAdmins.getFirst())))
                .andExpect(redirectedUrl("/admin/users"));
        verifyNoInteractions(api);
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 404, 502, 503, 504})
    void submissionFailuresUseSharedPageBehaviourWithoutCreatingSuccessState(int code) throws Exception {
        when(api.create(any(), any(), eq(false))).thenThrow(new UserDataApiClientException("RAW PRIVATE DATA", code));
        MockHttpSession session = journey();
        var result = mockMvc.perform(post("/admin/user/create/check-answers").session(session).with(csrf())
                .with(userOauth2Login(globalAdmins.getFirst())));
        if (code == 401 || code == 403) {
            result.andExpect(redirectedUrl(code == 401 ? "/oauth2/authorization/azure" : "/not-authorised"));
        } else {
            result.andExpect(status().is(code == 404 ? 404 : 500))
                    .andExpect(view().name("errors/error-generic"))
                    .andExpect(content().string(not(containsString("RAW PRIVATE DATA"))));
        }
        assertThat(session.getAttribute("createdUserResult")).isNull();
        assertThat(session.getAttribute("user")).isNotNull();
    }

    private MockHttpSession journey() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("user", EntraUserDto.builder().email("new-api-user@example.org")
                .firstName("Test").lastName("User").build());
        session.setAttribute("firm", FirmDto.builder().code("00123").name("Test firm").build());
        session.setAttribute("isMultiFirmUser", false);
        session.setAttribute("isUserManager", true);
        return session;
    }
}
