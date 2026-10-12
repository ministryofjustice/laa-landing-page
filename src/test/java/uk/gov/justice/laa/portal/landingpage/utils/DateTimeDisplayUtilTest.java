package uk.gov.justice.laa.portal.landingpage.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.justice.laa.portal.landingpage.dto.UserActivationRequestSummaryDto.DISPLAY_FORMATTER;

class DateTimeDisplayUtilTest {

    @Nested
    @DisplayName("Null handling")
    class NullHandlingTests {

        @Test
        @DisplayName("Should return null when input instant is null")
        void shouldReturnNullWhenInstantIsNull() {
            String formatted = DateTimeDisplayUtil.formatWithDaylightSavings(null, DISPLAY_FORMATTER);

            assertThat(formatted).isNull();
        }

        @Test
        @DisplayName("Should return null when both instant and formatter are null")
        void shouldReturnNullWhenBothInstantAndFormatterAreNull() {
            String formatted = DateTimeDisplayUtil.formatWithDaylightSavings(null, null);

            assertThat(formatted).isNull();
        }

        @Test
        @DisplayName("Should throw NullPointerException when formatter is null and instant is provided")
        void shouldThrowExceptionWhenFormatterIsNull() {
            Instant instant = Instant.parse("2026-09-02T21:46:00Z");

            assertThatThrownBy(() -> DateTimeDisplayUtil.formatWithDaylightSavings(instant, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Format verification with DISPLAY_FORMATTER")
    class DisplayFormatterTests {

        @ParameterizedTest(name = "UTC {0} -> Formatted UK local string: {1}")
        @CsvSource({
            // BST (Summer, UTC+1)
            "2026-09-02T21:46:00Z, 2 September 2026 10:46pm",   // Single-digit day, PM
            "2026-09-07T12:44:31.229841Z, 7 September 2026 1:44pm", // Sub-second truncation, BST PM
            "2026-06-05T08:05:00Z, 5 June 2026 9:05am",         // Padded minutes (:05), BST AM
            "2026-07-25T11:00:00Z, 25 July 2026 12:00pm",       // Double-digit day, Noon BST PM
            "2026-07-25T23:00:00Z, 26 July 2026 12:00am",       // Midnight rollover BST AM

            // GMT (Winter, UTC+0)
            "2026-01-09T08:05:00Z, 9 January 2026 8:05am",      // GMT single-digit hour, AM
            "2026-11-15T15:30:00Z, 15 November 2026 3:30pm",    // GMT PM
            "2026-12-31T23:59:00Z, 31 December 2026 11:59pm"   // Year end, GMT PM
        })
        void shouldFormatInstantAccuratelyWithDisplayFormatter(String isoInstant, String expectedOutput) {
            Instant instant = Instant.parse(isoInstant);

            String result = DateTimeDisplayUtil.formatWithDaylightSavings(instant, DISPLAY_FORMATTER);

            assertThat(result).isEqualTo(expectedOutput);
        }

        @Test
        @DisplayName("Should verify AMPM_LOWERCASE map values for am and pm directly")
        void shouldVerifyAmPmMapValues() {
            assertThat(UserActivationRequestSummaryDto.AMPM_LOWERCASE)
                    .containsEntry(0L, "am")
                    .containsEntry(1L, "pm")
                    .hasSize(2);
        }
    }

    @Nested
    @DisplayName("Class Structure & Code Coverage")
    class CodeCoverageTests {

        @Test
        @DisplayName("Should instantiate utility class via reflection to satisfy 100% coverage")
        void testConstructorForCoverage() throws NoSuchMethodException, InvocationTargetException,
                InstantiationException, IllegalAccessException {
            Constructor<DateTimeDisplayUtil> constructor = DateTimeDisplayUtil.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            DateTimeDisplayUtil instance = constructor.newInstance();

            assertThat(instance).isNotNull();
        }
    }
}
