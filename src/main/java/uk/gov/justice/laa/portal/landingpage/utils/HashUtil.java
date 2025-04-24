package uk.gov.justice.laa.portal.landingpage.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Utility class to perform hashing of string and uuid
 */
public class HashUtil {

    public static String sha256(UUID input) {
        if (input == null) {
            throw new RuntimeException("Invalid input value for hashing");
        }
        return sha256(input.toString());
    }

    public static String sha256(String input) {
        if (input == null || input.isEmpty()) {
            throw new RuntimeException("Invalid input string for hashing");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));


            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte hash : encodedHash) {
                String hex = Integer.toHexString(0xff & hash);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsaEx) {
            throw new RuntimeException("Unable to perform hashing", nsaEx);
        }
    }

}

