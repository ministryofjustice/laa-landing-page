package uk.gov.justice.laa.portal.landingpage.config;

import com.microsoft.kiota.RequestInformation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenAuthAccessProviderTests {

    private static final String TOKEN = "test-token";
    private static final String AUTH_HEADER = "Authorization";

    @Test
    void addsHeaderWhenMissing() {
        // Arrange
        TokenAuthAccessProvider provider = new TokenAuthAccessProvider(TOKEN);
        RequestInformation request = new RequestInformation();

        // Act
        provider.authenticateRequest(request, null);

        // Assert
        assertThat(request.headers.containsKey(AUTH_HEADER)).isTrue();
        assertThat(request.headers.get(AUTH_HEADER))
                .containsExactly("Bearer " + TOKEN);
    }

    @Test
    void doesNotOverwriteExistingHeader() {
        // Arrange
        TokenAuthAccessProvider provider = new TokenAuthAccessProvider("SHOULD-NOT-BE-USED");
        RequestInformation request = new RequestInformation();
        request.headers.add(AUTH_HEADER, "Bearer existing-value");

        // Act
        provider.authenticateRequest(request, null);

        // Assert
        assertThat(request.headers.get(AUTH_HEADER))
                .containsExactly("Bearer existing-value");
    }

}