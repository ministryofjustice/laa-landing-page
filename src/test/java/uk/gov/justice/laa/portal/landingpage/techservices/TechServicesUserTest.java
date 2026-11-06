package uk.gov.justice.laa.portal.landingpage.techservices;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class TechServicesUserTest {

    @Test
    void formattedCreatedDateTimeWithFullMonthAndSystemTimeZone() {
        TechServicesUser user = TechServicesUser.builder()
                .createdDateTime(Date.from(LocalDateTime.of(2026, 1, 1, 13, 30)
                        .atZone(ZoneId.systemDefault()).toInstant()))
                .build();

        assertThat(user.formattedCreatedDateTime()).isEqualTo("01 January 2026 1:30pm");
    }

    @Test
    void formattedCreatedDateTimeReturnsUnknownWhenMissing() {
        assertThat(new TechServicesUser().formattedCreatedDateTime()).isEqualTo("Unknown");
    }
}
