package uk.gov.justice.laa.portal.landingpage.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RandomPasswordGeneratorTest {

    @Test
    void generateRandomPassword() {
        String randomPassword = RandomPasswordGenerator.generateRandomPassword(8);
        assertThat(randomPassword).hasSize(8);
        assertThat(stringContainsItemFromList(randomPassword, RandomPasswordGenerator.UPPER.split(""))).isTrue();
        assertThat(stringContainsItemFromList(randomPassword, RandomPasswordGenerator.LOWER.split(""))).isTrue();
        assertThat(stringContainsItemFromList(randomPassword, RandomPasswordGenerator.DIGITS.split(""))).isTrue();
        assertThat(stringContainsItemFromList(randomPassword, RandomPasswordGenerator.SPECIAL_CHARS.split(""))).isTrue();
    }

    private static boolean stringContainsItemFromList(String inputStr, String[] items) {
        return Arrays.stream(items).anyMatch(inputStr::contains);
    }
}