package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

@ExtendWith(MockitoExtension.class)
class AuthorisationControllerTest {

    private AuthorisationController authorisationController;
    private Model model;

    @BeforeEach
    void setUp() {
        authorisationController = new AuthorisationController();
        model = new ExtendedModelMap();
    }

    @Test
    void notAuthorised_returnsNotAuthorisedView() {
        // When
        String result = authorisationController.notAuthorised(model);

        // Then
        assertThat(result).isEqualTo("not-authorised");
    }

    @Test
    void notAuthorised_setsPageTitleInModel() {
        // When
        authorisationController.notAuthorised(model);

        // Then
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Not Authorised");
    }
}
