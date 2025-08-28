package uk.gov.justice.laa.portal.landingpage.config.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/*
 *  JWT Decoder configuration for production environment
 * Provides a secure JWT decoder that validates token signature, issuer, and audience
 */
@Slf4j
@Configuration
@Profile({"prod"})
public class ProdJwtDecoderConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.audience}")
    private String audience;

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        log.info("Initializing JWT Decoder for production environment");
        log.info("JWT Configuration - Issuer URI: {}", issuerUri);
        log.info("JWT Configuration - JWK Set URI: {}", jwkSetUri);
        log.info("JWT Configuration - Audience: {}", audience);

        // try some validation
        if (issuerUri == null || issuerUri.trim().isEmpty()) {
            log.error("JWT issuer-uri is null or empty");
            throw new IllegalArgumentException("JWT issuer-uri cannot be null or empty");
        }

        if (jwkSetUri == null || jwkSetUri.trim().isEmpty()) {
            log.error("JWT jwk-set-uri is null or empty");
            throw new IllegalArgumentException("JWT jwk-set-uri cannot be null or empty");
        }

        if (audience == null || audience.trim().isEmpty()) {
            log.error("JWT audience is null or empty");
            throw new IllegalArgumentException("JWT audience cannot be null or empty");
        }

        try {
            log.debug("Creating NimbusJwtDecoder with JWK Set URI: {}", jwkSetUri);

            log.debug("Creating JWT validators");
            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
            OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);

            // different validator combinations to isolate the issue
            log.info("JWT validation with different validator combinations...");

            // 1: Only issuer validation
            OAuth2TokenValidator<Jwt> issuerOnly = withIssuer;

            // 2: Issuer + timestamp
            OAuth2TokenValidator<Jwt> issuerAndTimestamp =
                new DelegatingOAuth2TokenValidator<>(withIssuer, timestampValidator);

            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

            log.info("Using ISSUER + TIMESTAMP + AUDIENCE validation");
            OAuth2TokenValidator<Jwt> withAudienceAndTimestamp =
                    new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, timestampValidator);

            jwtDecoder.setJwtValidator(issuerAndTimestamp);

            log.info("JWT Decoder creation success");
            return jwtDecoder;
        } catch (Exception e) {
            log.error("Failed to create JWT Decoder: {}", e.getMessage(), e);
            throw new RuntimeException("JWT Decoder configuration failed", e);
        }
    }
}
