package uk.gov.justice.laa.portal.landingpage.config.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Custom validator that ensures the JWT token has the expected audience
 */
@Slf4j
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final String audience;

    public AudienceValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        log.debug("Expected audience: {}", audience);
        log.debug("JWT audiences: {}", jwt.getAudience());
        log.debug("JWT audience contains expected: {}", jwt.getAudience().contains(audience));
        
        if (jwt.getAudience().contains(audience)) {
            log.info("Audience validation successful");
            return OAuth2TokenValidatorResult.success();
        }
        
        log.debug("Audience validation failed - Expected: {} | Found: {}", audience, jwt.getAudience());
        return OAuth2TokenValidatorResult.failure(
            new OAuth2Error("invalid_token", "The required audience " + audience + " is missing", null));
    }
}
