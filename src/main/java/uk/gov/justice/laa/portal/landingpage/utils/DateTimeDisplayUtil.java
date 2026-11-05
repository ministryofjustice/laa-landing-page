package uk.gov.justice.laa.portal.landingpage.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class DateTimeDisplayUtil {

    private static final ZoneId ukZone = ZoneId.of("Europe/London");

    public static String formatWithDaylightSavings(Instant instant, DateTimeFormatter formatter) {
        if (instant == null) {
            return null;
        }

        ZonedDateTime zonedDateTime = instant.atZone(ukZone);

        return zonedDateTime.format(formatter);
    }

    private static final Map<Long, String> AMPM_LOWERCASE =
            Map.of(0L, "am", 1L, "pm");

    public static final DateTimeFormatter FULL_MONTH_DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("dd MMMM yyyy h:mm")
                    .appendText(ChronoField.AMPM_OF_DAY, AMPM_LOWERCASE)
                    .toFormatter(Locale.UK);

    public static final DateTimeFormatter SHORT_MONTH_DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendPattern("dd MMM yyyy h:mm")
                    .appendText(ChronoField.AMPM_OF_DAY, AMPM_LOWERCASE)
                    .toFormatter(Locale.UK);

    /**
     * Formats a LocalDateTime using the provided formatter.
     */
    public static String format(LocalDateTime dateTime, DateTimeFormatter formatter) {
        return dateTime == null ? "" : dateTime.format(formatter);
    }

    /**
     * Formats a Date using the provided formatter and zone.
     */
    public static String format(Date date, DateTimeFormatter formatter, ZoneId zone) {
        return date == null ? ""
                : formatter.format(date.toInstant().atZone(zone));
    }
}
