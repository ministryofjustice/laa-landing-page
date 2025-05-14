package uk.gov.justice.laa.portal.landingpage.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RandomPasswordGenerator class
 */
public class RandomPasswordGenerator {

    protected static final String UPPER = "BCDFGHJKMPQRTVWXY";
    protected static final String LOWER = UPPER.toLowerCase();
    protected static final String DIGITS = "346789";
    protected static final String SPECIAL_CHARS = "!@#$%^&*()_+{}[]";

    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SPECIAL_CHARS;

    private static SecureRandom random = new SecureRandom();

    public static String generateRandomPassword(int length) {
        List<Character> password = new ArrayList<>();

        // At least one uppercase letter
        password.add(UPPER.charAt(random.nextInt(UPPER.length())));
        // At least one lowercase letter
        password.add(LOWER.charAt(random.nextInt(LOWER.length())));
        // At least one digit
        password.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        // At least one special character
        password.add(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));

        // Remaining characters randomly selected from all characters
        for (int i = 4; i < length; i++) {
            password.add(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }
        Collections.shuffle(password);
        StringBuilder sb = new StringBuilder(); //now rebuild the word
        for (char c : password) {
            sb.append(c);
        }
        password = null;
        return sb.toString();
    }
}
