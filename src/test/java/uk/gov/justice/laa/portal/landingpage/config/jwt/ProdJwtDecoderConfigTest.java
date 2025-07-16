package uk.gov.justice.laa.portal.landingpage.config.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdJwtDecoderConfigTest {

    private static final String TEST_ISSUER_URI = "https://test-issuer.com";
    private static final String TEST_JWK_SET_URI = "https://test-issuer.com/jwks.json";
    private static final String TEST_AUDIENCE = "api://audience";

    @Mock
    private NimbusJwtDecoder mockNimbusJwtDecoder;
    
    @Mock
    private JwkSetUriJwtDecoderBuilder mockBuilder;

    @Mock
    private OAuth2TokenValidator<Jwt> mockIssuerValidator;

    private ProdJwtDecoderConfig prodJwtDecoderConfig;

    @BeforeEach
    void setUp() {
        prodJwtDecoderConfig = new ProdJwtDecoderConfig();

        ReflectionTestUtils.setField(prodJwtDecoderConfig, "issuerUri", TEST_ISSUER_URI);
        ReflectionTestUtils.setField(prodJwtDecoderConfig, "jwkSetUri", TEST_JWK_SET_URI);
        ReflectionTestUtils.setField(prodJwtDecoderConfig, "audience", TEST_AUDIENCE);
    }

    @Test
    void jwtDecoder_shouldConfigureWithCorrectJwkSetUri() {
        try (MockedStatic<NimbusJwtDecoder> mockedStatic = mockStatic(NimbusJwtDecoder.class)) {
            when(NimbusJwtDecoder.withJwkSetUri(TEST_JWK_SET_URI)).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockNimbusJwtDecoder);

            JwtDecoder result = prodJwtDecoderConfig.jwtDecoder();

            assertThat(result).isSameAs(mockNimbusJwtDecoder);
            mockedStatic.verify(() -> NimbusJwtDecoder.withJwkSetUri(TEST_JWK_SET_URI));
        }
    }

    @Test
    void jwtDecoder_shouldConfigureWithAllValidators() {
        try (MockedStatic<NimbusJwtDecoder> mockedStaticDecoder = mockStatic(NimbusJwtDecoder.class);
             MockedStatic<JwtValidators> mockedStaticValidators = mockStatic(JwtValidators.class)) {

            when(NimbusJwtDecoder.withJwkSetUri(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockNimbusJwtDecoder);

            when(JwtValidators.createDefaultWithIssuer(TEST_ISSUER_URI)).thenReturn(mockIssuerValidator);

            ArgumentCaptor<OAuth2TokenValidator<Jwt>> validatorCaptor = ArgumentCaptor.forClass(OAuth2TokenValidator.class);

            prodJwtDecoderConfig.jwtDecoder();

            verify(mockNimbusJwtDecoder).setJwtValidator(validatorCaptor.capture());

            OAuth2TokenValidator<Jwt> capturedValidator = validatorCaptor.getValue();
            assertThat(capturedValidator).isInstanceOf(DelegatingOAuth2TokenValidator.class);

            mockedStaticValidators.verify(() -> JwtValidators.createDefaultWithIssuer(TEST_ISSUER_URI));
        }
    }
    
    @Test
    void jwtDecoder_shouldIncludeAudienceValidator() {
        try (MockedStatic<NimbusJwtDecoder> mockedStaticDecoder = mockStatic(NimbusJwtDecoder.class)) {
            when(NimbusJwtDecoder.withJwkSetUri(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockNimbusJwtDecoder);

            ArgumentCaptor<OAuth2TokenValidator<Jwt>> validatorCaptor = ArgumentCaptor.forClass(OAuth2TokenValidator.class);

            prodJwtDecoderConfig.jwtDecoder();

            verify(mockNimbusJwtDecoder).setJwtValidator(validatorCaptor.capture());

            OAuth2TokenValidator<Jwt> capturedValidator = validatorCaptor.getValue();
            assertThat(capturedValidator).isInstanceOf(DelegatingOAuth2TokenValidator.class);
        }
    }
    
    @Test
    void jwtDecoder_shouldIncludeTimestampValidator() {
        try (MockedStatic<NimbusJwtDecoder> mockedStaticDecoder = mockStatic(NimbusJwtDecoder.class)) {

            when(NimbusJwtDecoder.withJwkSetUri(anyString())).thenReturn(mockBuilder);
            when(mockBuilder.build()).thenReturn(mockNimbusJwtDecoder);

            ArgumentCaptor<OAuth2TokenValidator<Jwt>> validatorCaptor = ArgumentCaptor.forClass(OAuth2TokenValidator.class);

            prodJwtDecoderConfig.jwtDecoder();

            verify(mockNimbusJwtDecoder).setJwtValidator(validatorCaptor.capture());

            OAuth2TokenValidator<Jwt> capturedValidator = validatorCaptor.getValue();
            assertThat(capturedValidator).isInstanceOf(DelegatingOAuth2TokenValidator.class);
        }
    }
}
