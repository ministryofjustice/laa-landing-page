package uk.gov.justice.laa.portal.landingpage.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class LogoutServiceTest {

    @InjectMocks
    private LogoutService logoutService;

    @Test
    public void buildAzureLogoutUrl_shouldBuildCorrectUrlWithAllParameters() {
        // Arrange
        String tenantId = "test-tenant-id";
        String baseUrl = "https://example.com";
        
        ReflectionTestUtils.setField(logoutService, "tenantId", tenantId);
        ReflectionTestUtils.setField(logoutService, "baseUrl", baseUrl);

        // Act
        String result = logoutService.buildAzureLogoutUrl();

        // Assert
        String expectedUrl = "https://login.microsoftonline.com/test-tenant-id/oauth2/v2.0/logout?post_logout_redirect_uri=https://example.com/login";
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    public void buildAzureLogoutUrl_shouldHandleLocalhostBaseUrl() {
        // Arrange
        String tenantId = "localhost-tenant";
        String baseUrl = "http://localhost:8080";
        
        ReflectionTestUtils.setField(logoutService, "tenantId", tenantId);
        ReflectionTestUtils.setField(logoutService, "baseUrl", baseUrl);

        // Act
        String result = logoutService.buildAzureLogoutUrl();

        // Assert
        String expectedUrl = "https://login.microsoftonline.com/localhost-tenant/oauth2/v2.0/logout?post_logout_redirect_uri=http://localhost:8080/login";
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    public void buildAzureLogoutUrl_shouldHandleBaseUrlWithPath() {
        // Arrange
        String tenantId = "path-tenant";
        String baseUrl = "https://app.example.com/portal";
        
        ReflectionTestUtils.setField(logoutService, "tenantId", tenantId);
        ReflectionTestUtils.setField(logoutService, "baseUrl", baseUrl);

        // Act
        String result = logoutService.buildAzureLogoutUrl();

        // Assert
        String expectedUrl = "https://login.microsoftonline.com/path-tenant/oauth2/v2.0/logout?post_logout_redirect_uri=https://app.example.com/portal/login";
        assertThat(result).isEqualTo(expectedUrl);
    }

    @Test
    public void buildAzureLogoutUrl_shouldHandleSpecialCharactersInTenantId() {
        // Arrange
        String tenantId = "tenant-with-special-chars_123";
        String baseUrl = "https://secure.example.com";
        
        ReflectionTestUtils.setField(logoutService, "tenantId", tenantId);
        ReflectionTestUtils.setField(logoutService, "baseUrl", baseUrl);

        // Act
        String result = logoutService.buildAzureLogoutUrl();

        // Assert
        assertThat(result).contains("tenant-with-special-chars_123");
        assertThat(result).contains("https://secure.example.com/login");
        assertThat(result).startsWith("https://login.microsoftonline.com/");
        assertThat(result).contains("/oauth2/v2.0/logout");
    }

    @Test
    public void buildAzureLogoutUrl_shouldProperlyEncodePostLogoutRedirectUri() {
        // Arrange
        String tenantId = "encode-test";
        String baseUrl = "https://test.com/app-normal";
        
        ReflectionTestUtils.setField(logoutService, "tenantId", tenantId);
        ReflectionTestUtils.setField(logoutService, "baseUrl", baseUrl);

        // Act
        String result = logoutService.buildAzureLogoutUrl();

        // Assert
        assertThat(result).contains("post_logout_redirect_uri=");
        assertThat(result).contains("test.com/app-normal");
        assertThat(result).contains("/login");
        assertThat(result).startsWith("https://login.microsoftonline.com/encode-test/oauth2/v2.0/logout");
    }
}
