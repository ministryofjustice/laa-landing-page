package uk.gov.justice.laa.portal.landingpage.controller;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;
import uk.gov.justice.laa.portal.landingpage.service.RoleChangeNotificationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OneTimeTaskControllerTest {

    @Mock
    private RoleChangeNotificationService roleChangeNotificationService;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private OneTimeTaskController controller;

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
}

