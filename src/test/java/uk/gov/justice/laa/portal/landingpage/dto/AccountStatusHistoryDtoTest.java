package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AccountStatusHistoryDtoTest {

    @Test
    void formattedStatusChangedDateWithShortMonthAndLowercaseAmPm() {
        AccountStatusHistoryDto record = AccountStatusHistoryDto.builder()
                .statusChangedDate(LocalDateTime.of(2026, 1, 1, 13, 30))
                .build();

        assertThat(record.formattedStatusChangedDate()).isEqualTo("01 Jan 2026 1:30pm");
    }

    @Test
    void formattedStatusChangedDateReturnsEmptyStringWhenMissing() {
        assertThat(new AccountStatusHistoryDto().formattedStatusChangedDate()).isEmpty();
    }
}
