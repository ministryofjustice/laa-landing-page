package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import uk.gov.justice.laa.portal.landingpage.constants.ModelAttributes;

@ExtendWith(MockitoExtension.class)
class StaticPagesControllerTest {

    @InjectMocks
    private StaticPagesController staticPagesController;

    private Model model;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
    }

    @Test
    void cookies_shouldReturnCookiesTemplate() {
        // When
        String viewName = staticPagesController.cookies(model);

        // Then
        assertThat(viewName).isEqualTo("footer/cookies");
    }

    @Test
    void cookies_shouldSetPageTitleAttribute() {
        // When
        staticPagesController.cookies(model);

        // Then
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Cookies");
    }

    @Test
    void cookies_shouldReturnCorrectTemplateAndSetModel() {
        // When
        String viewName = staticPagesController.cookies(model);

        // Then
        assertThat(viewName).isEqualTo("footer/cookies");
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Cookies");
    }

    @Test
    void cookies_shouldNotModifyExistingModelAttributes() {
        // Given
        model.addAttribute("existingAttribute", "existingValue");

        // When
        staticPagesController.cookies(model);

        // Then
        assertThat(model.getAttribute("existingAttribute")).isEqualTo("existingValue");
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Cookies");
    }
}
