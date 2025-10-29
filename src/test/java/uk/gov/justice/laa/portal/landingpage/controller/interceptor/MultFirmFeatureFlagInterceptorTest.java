package uk.gov.justice.laa.portal.landingpage.controller.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultFirmFeatureFlagInterceptorTest {

    private MultFirmFeatureFlagInterceptor interceptor;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        interceptor = new MultFirmFeatureFlagInterceptor();
        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
    }

    @Test
    void testPreHandle_whenFeatureFlagEnabled_shouldAllowRequest() {
        // Arrange
        ReflectionTestUtils.setField(interceptor, "enableMultiFirmUser", true);

        // Act & Assert
        assertThat(interceptor.preHandle(mockRequest, mockResponse, new Object())).isTrue();
    }

    @Test
    void testPreHandle_whenFeatureFlagDisabled_shouldThrowAccessDeniedException() {
        // Arrange
        ReflectionTestUtils.setField(interceptor, "enableMultiFirmUser", false);
        when(mockRequest.getRequestURI()).thenReturn("/admin/multi-firm/test");

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                interceptor.preHandle(mockRequest, mockResponse, new Object()));

        assertThat(exception.getMessage()).isEqualTo("Multi Firm User functionality is disabled.");
    }
}