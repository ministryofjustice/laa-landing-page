package uk.gov.justice.laa.portal.landingpage.config.jwt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

/**
 * JWT Decoder configuration for development environments
 * Provides a permissive JWT decoder that accepts any token for easier local testing
 */
@Configuration
@Profile({"dev", "local", "test"})
public class DevJwtDecoderConfig {

    /**
     * For development and testing environments, provide a JwtDecoder that accepts any token
     * This should NOT be used in production
     */
    @Bean("testJwtDecoder")
    @Primary
    public JwtDecoder jwtDecoder() {
        return token -> {
            try {
                System.out.println("Using development JWT decoder with token: " + token.substring(0, Math.min(10, token.length())) + "...");
                // Create a dummy JWT with some standard claims
                return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", "user")
                    .claim("roles", "USER")
                    .claim("scope", "read")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            } catch (Exception e) {
                System.err.println("Error in development JWT decoder: " + e.getMessage());
                throw new RuntimeException("Error creating JWT", e);
            }
        };
    }
}
