package uk.gov.justice.laa.portal.landingpage.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaaApplicationForViewTest {

    @Test
    void testConstructorFromLaaApplication_withSpecialHandling() {
        AppDto.AlternativeAppDescriptionDto descriptionIfAppAssigned = AppDto.AlternativeAppDescriptionDto
                .builder().assignedAppId(UUID.randomUUID().toString()).alternativeDescription("Some description").build();

        AppDto laaApplication = AppDto.builder()
                .name("AppName").title("AppTitle").description("AppDescription")
                .url("http://example.com").ordinal(1)
                .alternativeAppDescription(descriptionIfAppAssigned).build();

        LaaApplicationForView view = new LaaApplicationForView(laaApplication);

        assertEquals("AppName", view.getName());
        assertEquals("AppTitle", view.getTitle());
        assertEquals("AppDescription", view.getDescription());
        assertEquals("http://example.com", view.getUrl());
        assertEquals(1, view.getOrdinal());
        assertTrue(view.isSpecialHandling());
    }

    @Test
    void testConstructorFromLaaApplication_withoutSpecialHandling() {
        AppDto laaApplication = AppDto.builder()
                .name("AppName").title("AppTitle").description("AppDescription")
                .url("http://example.com").ordinal(2).build();

        LaaApplicationForView view = new LaaApplicationForView(laaApplication);

        assertFalse(view.isSpecialHandling());
    }

    @Test
    void testCompareTo() {
        LaaApplicationForView app1 = LaaApplicationForView.builder().name("App 1").ordinal(1).build();
        LaaApplicationForView app2 = LaaApplicationForView.builder().name("App 2").ordinal(2).build();

        assertTrue(app1.compareTo(app2) < 0);
        assertTrue(app2.compareTo(app1) > 0);
        assertEquals(0, app1.compareTo(app1));
    }
}
