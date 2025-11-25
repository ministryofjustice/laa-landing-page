package uk.gov.justice.laa.portal.landingpage.playwright.common;

import java.util.Random;

public class TestUtils {

    /**
     * Generates a random alphanumeric string of the specified length.
     */
    public static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a random Gmail address with the specified number of characters before @gmail.com.
     */
    public static String generateRandomEmail(int length) {
        return generateRandomString(length) + "@gmail.com";
    }
}
