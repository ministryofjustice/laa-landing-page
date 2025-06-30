package uk.gov.justice.laa.portal.landingpage.config.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AudienceValidatorTest {

    private static final String EXPECTED_AUDIENCE = "api://test-audience";
    private static final String DIFFERENT_AUDIENCE = "api://different-audience";

    @Test
    void shouldSucceedWhenAudienceMatches() {
        AudienceValidator validator = new AudienceValidator(EXPECTED_AUDIENCE);
        Jwt jwt = createJwtWithAudience(EXPECTED_AUDIENCE);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void shouldFailWhenAudienceDoesNotMatch() {
        AudienceValidator validator = new AudienceValidator(EXPECTED_AUDIENCE);
        Jwt jwt = createJwtWithAudience(DIFFERENT_AUDIENCE);

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        verifyErrorDetails(result);
    }

    @Test
    void shouldSucceedWhenMultipleAudiencesIncludeExpected() {
        AudienceValidator validator = new AudienceValidator(EXPECTED_AUDIENCE);
        Jwt jwt = createJwtWithMultipleAudiences(List.of(DIFFERENT_AUDIENCE, EXPECTED_AUDIENCE));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    private Jwt createJwtWithAudience(String audience) {
        return createJwtWithMultipleAudiences(Collections.singletonList(audience));
    }

    private Jwt createJwtWithMultipleAudiences(List<String> audiences) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", "RS256");

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "test-subject");
        
        Instant now = Instant.now();
        
        return new Jwt(
            "test-token-value",
            now,
            now.plusSeconds(300),
            headers,
            claims
        ) {
            @Override
            public List<String> getAudience() {
                return audiences;
            }
        };
    }

    private void verifyErrorDetails(OAuth2TokenValidatorResult result) {
        OAuth2Error error = result.getErrors().iterator().next();
        assertThat(error.getErrorCode()).isEqualTo("invalid_token");
        assertThat(error.getDescription()).contains(EXPECTED_AUDIENCE);
        assertThat(error.getDescription()).contains("missing");
    }
}
