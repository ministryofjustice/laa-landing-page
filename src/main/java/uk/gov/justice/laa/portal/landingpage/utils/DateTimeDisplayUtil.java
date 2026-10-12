package uk.gov.justice.laa.portal.landingpage.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeDisplayUtil {

    private static final ZoneId ukZone = ZoneId.of("Europe/London");

    public static String formatWithDaylightSavings(Instant instant, DateTimeFormatter formatter) {
        if (instant == null) {
            return null;
        }

        ZonedDateTime zonedDateTime = instant.atZone(ukZone);

        return zonedDateTime.format(formatter);
    }
}
