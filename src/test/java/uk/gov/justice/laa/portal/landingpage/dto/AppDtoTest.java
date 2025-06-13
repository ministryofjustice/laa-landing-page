package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AppDtoTest {

    @Test
    public void testEqualsIsTrueWhenIdsAreSame() {
        AppDto appDto1 = new AppDto();
        appDto1.setId("testId");
        AppDto appDto2 = new AppDto();
        appDto2.setId("testId");
        assertThat(appDto1.equals(appDto2)).isTrue();
    }

    @Test
    public void testEqualsIsFalseWhenIdsAreDifferent() {
        AppDto appDto1 = new AppDto();
        appDto1.setId("testId1");
        AppDto appDto2 = new AppDto();
        appDto2.setId("testId2");
        assertThat(appDto1.equals(appDto2)).isFalse();
    }
}
