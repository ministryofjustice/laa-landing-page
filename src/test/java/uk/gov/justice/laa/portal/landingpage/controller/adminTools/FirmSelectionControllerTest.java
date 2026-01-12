package uk.gov.justice.laa.portal.landingpage.controller.adminTools;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.forms.RolesForm;

import static org.junit.jupiter.api.Assertions.*;

class FirmSelectionControllerTest {

    @Test
    void selectUserGet() {

        // Act
        String view = FirmSelectionController.(id, 1, new RolesForm(), null, authentication, model, httpSession);
    }

    @Test
    void selectUserPost() {
    }

    @Test
    void selectFirmGet() {
    }

    @Test
    void selectFirmPost() {
    }

    @Test
    void checkAnswerGet() {
    }

    @Test
    void checkAnswerPost() {
    }

    @Test
    void confirmation() {
    }

    @Test
    void cancel() {
    }

    @Test
    void handleAuthorizationException() {
    }

    @Test
    void handleException() {
    }
}