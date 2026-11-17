package uk.gov.justice.laa.portal.landingpage.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditUserDetailDtoTest {

    @Test
    void formattedCreatedDateWithFullMonthAndLowercaseAmPm() {
        AuditUserDetailDto user = AuditUserDetailDto.builder()
                .createdDate(LocalDateTime.of(2026, 1, 1, 13, 30))
                .build();

        assertThat(user.formattedCreatedDate()).isEqualTo("01 January 2026 1:30pm");
    }

    @Test
    void formattedCreatedDateReturnsEmptyStringWhenMissing() {
        assertThat(new AuditUserDetailDto().formattedCreatedDate()).isEmpty();
    }
}
