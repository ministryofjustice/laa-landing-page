package uk.gov.justice.laa.portal.landingpage.model;

import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.portal.landingpage.dto.AppDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaaApplicationForViewTest {

    @Test
    void testConstructorFromLaaApplication() {
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
        assertEquals(-1, app1.compareTo(app2));
    }
}
