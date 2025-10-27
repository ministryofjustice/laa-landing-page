package uk.gov.justice.laa.portal.landingpage.config.featureflag;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import uk.gov.justice.laa.portal.landingpage.controller.interceptor.MultFirmFeatureFlagInterceptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MultiFirmFeatureFlagConfigTest {

    @Test
    void testAddInterceptors() {
        // Arrange
        MultFirmFeatureFlagInterceptor mockInterceptor = mock(MultFirmFeatureFlagInterceptor.class);
        InterceptorRegistry mockRegistry = mock(InterceptorRegistry.class);
        InterceptorRegistration mockRegistration = mock(InterceptorRegistration.class);

        // Stub the method to return the mock registration
        when(mockRegistry.addInterceptor(mockInterceptor)).thenReturn(mockRegistration);
        when(mockRegistration.addPathPatterns("/admin/multi-firm/**")).thenReturn(mockRegistration);

        MultiFirmFeatureFlagConfig config = new MultiFirmFeatureFlagConfig(mockInterceptor);

        // Act
        config.addInterceptors(mockRegistry);

        // Assert
        verify(mockRegistry).addInterceptor(mockInterceptor);
        verify(mockRegistration).addPathPatterns("/admin/multi-firm/**");
    }


}