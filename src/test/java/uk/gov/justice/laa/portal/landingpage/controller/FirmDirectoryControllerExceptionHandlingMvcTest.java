package uk.gov.justice.laa.portal.landingpage.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import uk.gov.justice.laa.portal.landingpage.client.UserDataApiClientException;
import uk.gov.justice.laa.portal.landingpage.config.DataApiFeatureFlagService;
import uk.gov.justice.laa.portal.landingpage.service.AccessControlService;
import uk.gov.justice.laa.portal.landingpage.service.LoginService;
import uk.gov.justice.laa.portal.landingpage.service.UserAccountStatusService;
import uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProvider;
import uk.gov.justice.laa.portal.landingpage.service.firmdirectory.FirmDirectoryProviderResolver;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Exercises the data API exception handler through Spring MVC, so the handler's return value
 * is resolved by the framework rather than inspected directly.
 */
@ExtendWith(MockitoExtension.class)
class FirmDirectoryControllerExceptionHandlingMvcTest {

    private static final String FIRM_URL = "/admin/firmDirectory/25966";

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private UserAccountStatusService userAccountStatusService;
    @Mock
    private LoginService loginService;
    @Mock
    private DataApiFeatureFlagService dataApiFeatureFlagService;
    @Mock
    private FirmDirectoryProviderResolver firmDirectoryProviderResolver;
    @Mock
    private FirmDirectoryProvider firmDirectoryProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FirmDirectoryController controller = new FirmDirectoryController(accessControlService,
                userAccountStatusService, loginService, dataApiFeatureFlagService, firmDirectoryProviderResolver);
        ReflectionTestUtils.setField(controller, "firmDirectoryEnabled", true);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(new InternalResourceViewResolver("/", ".html"))
                .build();
        when(firmDirectoryProviderResolver.resolve()).thenReturn(firmDirectoryProvider);
    }

    @Test
    void apiForbiddenRedirectsToNotAuthorised() throws Exception {
        when(firmDirectoryProvider.getFirm("25966")).thenThrow(new UserDataApiClientException("x", 403));

        mockMvc.perform(get(FIRM_URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/not-authorised"));
    }

    @Test
    void apiNotFoundRendersNotFoundPage() throws Exception {
        when(firmDirectoryProvider.getFirm("25966")).thenThrow(new UserDataApiClientException("x", 404));

        mockMvc.perform(get(FIRM_URL))
                .andExpect(status().isNotFound())
                .andExpect(view().name("errors/error-404"));
    }

    @Test
    void apiServiceFailureRendersGenericErrorPage() throws Exception {
        when(firmDirectoryProvider.getFirm("25966")).thenThrow(new UserDataApiClientException("x", 503));

        mockMvc.perform(get(FIRM_URL))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("errors/error-generic"));
    }
}
