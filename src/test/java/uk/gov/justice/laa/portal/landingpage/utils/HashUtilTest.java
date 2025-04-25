package uk.gov.justice.laa.portal.landingpage.utils;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test class for exercising HashUtil
 */
public class HashUtilTest {

    @Test
    public void hashString() {
        assertThat(HashUtil.sha256("test")).isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
    }

    @Test
    public void hashUuid() {
        UUID uuid = UUID.fromString("698815d2-5760-4fd0-bdef-54c683e91b26");
        assertThat(HashUtil.sha256(uuid)).isEqualTo("4efb3caa44d53b15ef398fa622110166f63eadc9ad68f6f8954529c39b901889");
    }

    @Test
    public void hashNullString() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
            String s = null;
            HashUtil.sha256(s);
        }).withMessage("Invalid input string for hashing");
    }

    @Test
    @SuppressWarnings("assigned to null")
    public void hashNullUuid() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
            UUID uuid = null;
            HashUtil.sha256(uuid);
        }).withMessage("Invalid input value for hashing");
    }
}
