package uk.gov.justice.laa.portal.landingpage.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import uk.gov.justice.laa.portal.landingpage.service.RoleChangeNotificationService;
import uk.gov.justice.laa.portal.landingpage.service.UserService;

@ExtendWith(MockitoExtension.class)
class OneTimeTaskControllerTest {

    @Mock
    private RoleChangeNotificationService roleChangeNotificationService;

    @Mock
    private UserService userService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private OneTimeTaskController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void testOneTimeTasks_ShouldTriggerSyncAndRedirect() {
        // Act
        RedirectView result = controller.oneTimeTasks(redirectAttributes);

        // Assert
        verify(roleChangeNotificationService, times(1)).ccmsRoleSync();
        verify(redirectAttributes, times(1))
                .addFlashAttribute(eq("successMessage"), eq("CCMS Role Sync has been triggered successfully"));
        assertEquals("/admin/users", result.getUrl());
    }

    @Test
    void ccmsRoleSync_getRequest_returns405MethodNotAllowed() throws Exception {
        mockMvc.perform(get("/admin/ccms-role-sync"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void updateProfileStatus_getRequest_returns405MethodNotAllowed() throws Exception {
        mockMvc.perform(get("/admin/update-profile-status"))
                .andExpect(status().isMethodNotAllowed());
    }
}
