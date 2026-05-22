package uk.gov.justice.laa.portal.landingpage.utils;

public class MaskUtil {

    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        int visible = 2; // number of chars to show at start/end

        if (value.length() <= visible * 2) {
            return "****";
        }

        String start = value.substring(0, visible);
        String end = value.substring(value.length() - visible);

        return start + "****" + end;
    }
}
