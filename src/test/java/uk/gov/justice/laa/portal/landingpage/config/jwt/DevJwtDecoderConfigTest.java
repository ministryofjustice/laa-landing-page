package uk.gov.justice.laa.portal.landingpage.config.jwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DevJwtDecoderConfigTest {

    @Test
    void shouldDecodeValidToken() {
        DevJwtDecoderConfig config = new DevJwtDecoderConfig();
        JwtDecoder jwtDecoder = config.jwtDecoder();
        String token = "valid-token";

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt).isNotNull();
        assertThat(jwt.getTokenValue()).isEqualTo(token);

        Instant now = Instant.now();
        Instant fiveMinutesFromNow = now.plusSeconds(3600);

        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getIssuedAt()).isBetween(now.minusSeconds(10), now.plusSeconds(10));
        assertThat(jwt.getExpiresAt()).isBetween(fiveMinutesFromNow.minusSeconds(10), fiveMinutesFromNow.plusSeconds(10));
    }

    @Test
    void shouldHandleNullToken() {
        DevJwtDecoderConfig config = new DevJwtDecoderConfig();
        JwtDecoder jwtDecoder = config.jwtDecoder();

        assertThrows(RuntimeException.class, () -> jwtDecoder.decode(null));
    }

    @Test
    void shouldHandleEmptyToken() {
        DevJwtDecoderConfig config = new DevJwtDecoderConfig();
        JwtDecoder jwtDecoder = config.jwtDecoder();
        String emptyToken = "";

        assertThatThrownBy(() -> jwtDecoder.decode(emptyToken))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Error creating JWT")
            .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
