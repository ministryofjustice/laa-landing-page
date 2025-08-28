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
        log.debug("JWT Configuration - Issuer URI: {}", issuerUri);
        log.debug("JWT Configuration - JWK Set URI: {}", jwkSetUri);
        log.debug("JWT Configuration - Audience: {}", audience);

        try {
            OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
            OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);

            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

            OAuth2TokenValidator<Jwt> withAudienceAndTimestamp =
                    new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, timestampValidator);

            jwtDecoder.setJwtValidator(withAudienceAndTimestamp);
            return jwtDecoder;
        } catch (Exception e) {
            log.error("Failed to create JWT Decoder: {}", e.getMessage(), e);
            throw new RuntimeException("JWT Decoder configuration failed", e);
        }
    }
}
