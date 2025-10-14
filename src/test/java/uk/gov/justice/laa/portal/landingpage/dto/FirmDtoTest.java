package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class FirmDtoTest {

    @Test
    public void testDisplayNameWithCode() {
        FirmDto firmDto1 = FirmDto.builder().id(UUID.randomUUID()).name("Alpha").code("Alpha").build();
        assertThat(firmDto1.getDisplayName()).isEqualTo("Alpha (Alpha)");
    }

    @Test
    public void testDisplayNameWithoutCode() {
        FirmDto firmDto1 = FirmDto.builder().id(UUID.randomUUID()).name("Alpha").build();
        assertThat(firmDto1.getDisplayName()).isEqualTo("Alpha");
    }

    @Test
    public void testDisplayNameWithEmptyCode() {
        FirmDto firmDto1 = FirmDto.builder().id(UUID.randomUUID()).name("Alpha").code("").build();
        assertThat(firmDto1.getDisplayName()).isEqualTo("Alpha");
    }
}
