package uk.gov.justice.laa.portal.landingpage.config.featureflag;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MultiFirmFeatureFlagConfig implements WebMvcConfigurer {

    private final MultFirmFeatureFlagInterceptor featureFlagInterceptor;

    public MultiFirmFeatureFlagConfig(MultFirmFeatureFlagInterceptor featureFlagInterceptor) {
        this.featureFlagInterceptor = featureFlagInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(featureFlagInterceptor)
                .addPathPatterns("/admin/multi-firm/**");
    }
}
