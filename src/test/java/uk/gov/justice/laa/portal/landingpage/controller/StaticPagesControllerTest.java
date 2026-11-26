package uk.gov.justice.laa.portal.landingpage.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
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

    @Test
    void accessibility_shouldReturnAccessibilityTemplate() {
        // When
        String viewName = staticPagesController.accessibility(model);

        // Then
        assertThat(viewName).isEqualTo("footer/accessibility");
    }

    @Test
    void accessibility_shouldSetPageTitleAttribute() {
        // When
        staticPagesController.accessibility(model);

        // Then
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Accessibility");
    }

    @Test
    void accessibility_shouldReturnCorrectTemplateAndSetModel() {
        // When
        String viewName = staticPagesController.accessibility(model);

        // Then
        assertThat(viewName).isEqualTo("footer/accessibility");
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Accessibility");
    }

    @Test
    void accessibility_shouldNotModifyExistingModelAttributes() {
        // Given
        model.addAttribute("existingAttribute", "existingValue");

        // When
        staticPagesController.accessibility(model);

        // Then
        assertThat(model.getAttribute("existingAttribute")).isEqualTo("existingValue");
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Accessibility");
    }

    @Test
    void help_shouldReturnCorrectTemplateAndSetModel() {
        String viewName = staticPagesController.help(model);

        assertThat(viewName).isEqualTo("help");
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Help");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://portal-laa.dev.external-identity.service.justice.gov.uk",
        "https://portal-laa.test.external-identity.service.justice.gov.uk",
        "https://laa-sign-in.external-identity.service.justice.gov.uk"
    })
    void start_shouldReturnCorrectTemplateAndSetModel(String externalIdentityServiceLink) {
        ReflectionTestUtils.setField(staticPagesController, "externalIdentityServiceLink", externalIdentityServiceLink);
        model.addAttribute("existingAttribute", "existingValue");

        String viewName = staticPagesController.start(model);

        assertThat(viewName).isEqualTo("start");
        assertThat(model.getAttribute(ModelAttributes.PAGE_TITLE)).isEqualTo("Start");
        assertThat(model.getAttribute("externalIdentityServiceLink")).isEqualTo(externalIdentityServiceLink);
        assertThat(model.getAttribute("existingAttribute")).isEqualTo("existingValue");
    }
}
